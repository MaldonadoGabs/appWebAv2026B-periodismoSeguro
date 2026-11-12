# PASO 2: Azure DevOps (conexion a Azure + conexion a GitHub + pipeline)
# Necesita dos variables de entorno (DESPLEGAR.ps1 las pide solo):
#   AZURE_DEVOPS_EXT_PAT         = token de Azure DevOps (Full access)
#   AZURE_DEVOPS_EXT_GITHUB_PAT  = token de GitHub (repo + admin:repo_hook)
param(
  [string]$Org = 'https://dev.azure.com/gabrielmaldonado1',
  [string]$Project = 'AppWebAv2026-B',
  [string]$Repo = 'https://github.com/MaldonadoGabs/appWebAv2026B-periodismoSeguro',
  [string]$Sufijo = 'psa26'
)
$ErrorActionPreference = 'Stop'
function Ok($what) { if ($LASTEXITCODE -ne 0) { throw "FALLO: $what (revisa el mensaje de arriba)" } }

if (-not $env:AZURE_DEVOPS_EXT_PAT)        { throw 'Falta AZURE_DEVOPS_EXT_PAT (token de Azure DevOps)' }
if (-not $env:AZURE_DEVOPS_EXT_GITHUB_PAT) { throw 'Falta AZURE_DEVOPS_EXT_GITHUB_PAT (token de GitHub)' }

$RG = "rg-periodismo-$Sufijo"
$d = @('--org', $Org, '--project', $Project, '--detect', 'false')

az extension add --name azure-devops --upgrade -y
az devops project show --project $Project --org $Org --detect false -o none; Ok 'No puedo entrar al proyecto: token de Azure DevOps mal o sin permisos'

# --- Conexion a Azure (service principal solo sobre el grupo de recursos) ---
$ex = az devops service-endpoint list @d --query "[?name=='sc-azure'].id | [0]" -o tsv
if (-not $ex) {
  $sub = az account show --query id -o tsv
  $tid = az account show --query tenantId -o tsv
  $sp  = az ad sp create-for-rbac --name "sp-periodismo-$Sufijo-$(Get-Random)" --role Contributor --scopes "/subscriptions/$sub/resourceGroups/$RG" -o json | ConvertFrom-Json; Ok 'crear service principal'
  $env:AZURE_DEVOPS_EXT_AZURE_RM_SERVICE_PRINCIPAL_KEY = $sp.password
  az devops service-endpoint azurerm create @d --name sc-azure --azure-rm-service-principal-id $sp.appId `
    --azure-rm-subscription-id $sub --azure-rm-subscription-name 'suscripcion' --azure-rm-tenant-id $tid -o none; Ok 'crear conexion sc-azure'
}

# --- Conexion a GitHub ---
$gh = az devops service-endpoint list @d --query "[?name=='sc-github'].id | [0]" -o tsv
if ($gh) {
  # se recrea siempre para usar el token de GitHub de esta corrida
  az devops service-endpoint delete @d --id $gh --yes; Ok 'borrar sc-github anterior'
  $gh = $null
}
if (-not $gh) {
  az devops service-endpoint github create @d --name sc-github --github-url $Repo -o none; Ok 'crear conexion sc-github (token de GitHub)'
  $gh = az devops service-endpoint list @d --query "[?name=='sc-github'].id | [0]" -o tsv
}
if (-not $gh) { throw 'No se creo la conexion sc-github' }

# --- Pipeline ---
$pl = az pipelines list @d --query "[?name=='periodismo-ci-cd'].id | [0]" -o tsv
if (-not $pl) {
  az pipelines create @d --name periodismo-ci-cd --repository $Repo --branch main --repository-type github `
    --service-connection $gh --yml-path azure-pipelines.yml --skip-first-run true -o none; Ok 'crear pipeline'
}

Write-Host "`n=== LISTO PASO 2 === Pipeline periodismo-ci-cd creado." -ForegroundColor Green
