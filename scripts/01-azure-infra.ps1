# PASO 1: infraestructura Azure (RG + ACR + Container Apps Env + PostgreSQL + Container App)
# Se detiene en el primer error y se puede repetir sin problema.
# Busca solo una region que tu suscripcion permita (Azure for Students limita las regiones).
$ErrorActionPreference = 'Stop'
function Ok($what) { if ($LASTEXITCODE -ne 0) { throw "FALLO en: $what  (copia el mensaje de arriba)" } }

$Sufijo   = 'psa26'
$RG       = "rg-periodismo-$Sufijo"
$Acr      = "acrperiodismo$Sufijo"
$Env      = "cae-periodismo-$Sufijo"
$App      = "ca-periodismo-$Sufijo"
$Pg       = "pg-periodismo-$Sufijo"
$PgUser   = 'periodismo'
$PgPass   = -join ((48..57)+(65..90)+(97..122) | Get-Random -Count 24 | ForEach-Object {[char]$_})

# Prueba una accion en cada region hasta que una funcione. Devuelve la region o $null.
function Probar-Regiones($candidatas, $accion) {
  foreach ($l in $candidatas) {
    Write-Host "   probando region $l ..." -ForegroundColor DarkGray
    $old = $ErrorActionPreference; $ErrorActionPreference = 'Continue'
    & $accion $l 2>&1 | Out-Null
    $code = $LASTEXITCODE
    $ErrorActionPreference = $old
    if ($code -eq 0) { return $l }
  }
  return $null
}

az login | Out-Null; Ok 'az login'
az extension add --name containerapp --upgrade -y
foreach ($ns in 'Microsoft.App','Microsoft.OperationalInsights','Microsoft.ContainerRegistry','Microsoft.DBforPostgreSQL') {
  az provider register --namespace $ns --wait; Ok "registrar $ns"
}

# Regiones candidatas: primero las que permite la politica de tu suscripcion (si se puede leer), luego una lista comun
$comunes = 'eastus','eastus2','centralus','westus3','westus2','southcentralus','canadacentral','brazilsouth','chilecentral','mexicocentral','spaincentral','italynorth','polandcentral','austriaeast','switzerlandnorth','norwayeast','swedencentral','northeurope','westeurope','uksouth','francecentral','germanywestcentral'
$politica = @(az policy assignment list --query "[].parameters.listOfAllowedLocations.value[]" -o tsv) | Where-Object { $_ }
$candidatas = @($politica) + @($comunes) | Select-Object -Unique
if ($politica) { Write-Host "Regiones permitidas por tu suscripcion: $($politica -join ', ')" -ForegroundColor Yellow }

Write-Host '[1/7] Grupo de recursos' -ForegroundColor Cyan
if ((az group exists -n $RG) -ne 'true') {
  az group create -n $RG -l ($candidatas | Select-Object -First 1) -o none; Ok 'crear grupo de recursos'
}

Write-Host '[2/7] Container Registry (busca region permitida)' -ForegroundColor Cyan
$Location = az acr list -g $RG --query "[?name=='$Acr'].location | [0]" -o tsv
if (-not $Location) {
  $Location = Probar-Regiones $candidatas { param($l) az acr create -n $Acr -g $RG --sku Basic -l $l -o none }
  if (-not $Location) { throw 'Ninguna region permitida para crear el ACR. Pide al admin de la suscripcion que revise las politicas.' }
}
Write-Host "   ACR en: $Location" -ForegroundColor Green

Write-Host '[3/7] Entorno de Container Apps' -ForegroundColor Cyan
$envExiste = az containerapp env list -g $RG --query "[?name=='$Env'] | length(@)" -o tsv
if ($envExiste -eq '0') {
  $orden = @($Location) + @($candidatas) | Select-Object -Unique
  $r = Probar-Regiones $orden { param($l) az containerapp env create -n $Env -g $RG -l $l -o none }
  if (-not $r) { throw 'Ninguna region permitida para Container Apps.' }
  Write-Host "   Entorno en: $r" -ForegroundColor Green
}

Write-Host '[4/7] PostgreSQL (puede tardar 5-10 min)' -ForegroundColor Cyan
$pgExiste = az postgres flexible-server list -g $RG --query "[?name=='$Pg'] | length(@)" -o tsv
if ($pgExiste -eq '0') {
  $orden = @($Location) + @($candidatas) | Select-Object -Unique
  $r = Probar-Regiones $orden { param($l) az postgres flexible-server create -n $Pg -g $RG -l $l --admin-user $PgUser --admin-password $PgPass --sku-name Standard_B1ms --tier Burstable --storage-size 32 --version 16 --public-access 0.0.0.0 --yes -o none }
  if (-not $r) { throw 'Ninguna region permitida para PostgreSQL. Copia este mensaje a Claude.' }
  Write-Host "   PostgreSQL en: $r" -ForegroundColor Green
} else {
  az postgres flexible-server update -n $Pg -g $RG --admin-password $PgPass -o none; Ok 'actualizar clave de PostgreSQL'
}
az postgres flexible-server db create -g $RG -s $Pg -n periodismo -o none; Ok 'crear base periodismo'

Write-Host '[5/7] Container App' -ForegroundColor Cyan
$appExiste = az containerapp list -g $RG --query "[?name=='$App'] | length(@)" -o tsv
if ($appExiste -eq '0') {
  az containerapp create -n $App -g $RG --environment $Env `
    --image mcr.microsoft.com/k8se/quickstart:latest --target-port 80 --ingress external `
    --min-replicas 1 --max-replicas 1 -o none; Ok 'crear Container App'
}

Write-Host '[6/7] Permisos ACR y variables' -ForegroundColor Cyan
az containerapp identity assign -n $App -g $RG --system-assigned -o none; Ok 'identidad del Container App'
$principal = az containerapp show -n $App -g $RG --query identity.principalId -o tsv; Ok 'leer identidad'
$acrId = az acr show -n $Acr -g $RG --query id -o tsv; Ok 'leer ACR'
az role assignment create --assignee $principal --role AcrPull --scope $acrId -o none; Ok 'dar AcrPull'
az containerapp registry set -n $App -g $RG --server "$Acr.azurecr.io" --identity system -o none; Ok 'conectar ACR'
az containerapp secret set -n $App -g $RG --secrets "db-password=$PgPass" -o none; Ok 'guardar secreto de la BD'
$dbUrl = "jdbc:postgresql://$Pg.postgres.database.azure.com:5432/periodismo?sslmode=require"
az containerapp update -n $App -g $RG --set-env-vars "DB_URL=$dbUrl" "DB_USER=$PgUser" "DB_PASSWORD=secretref:db-password" "PORT=8080" -o none; Ok 'variables de entorno'
az containerapp ingress update -n $App -g $RG --target-port 8080 -o none; Ok 'puerto 8080'

Write-Host '[7/7] Verificando' -ForegroundColor Cyan
$fqdn = az containerapp show -n $App -g $RG --query properties.configuration.ingress.fqdn -o tsv; Ok 'leer URL'
if (-not $fqdn) { throw 'El Container App no tiene URL' }
Write-Host "`n=== LISTO PASO 1 ===" -ForegroundColor Green
Write-Host "RG=$RG  ACR=$Acr  APP=$App"
Write-Host "URL: https://$fqdn"
