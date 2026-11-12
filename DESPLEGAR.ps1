# TODO EN UNO. Corre:  .\DESPLEGAR.ps1
# Si el paso 1 (Azure) ya termino bien y solo falla el paso 2:  .\DESPLEGAR.ps1 -SoloPaso2
param([switch]$SoloPaso2)
$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot

function Pedir($texto) {
  Write-Host $texto -ForegroundColor Yellow
  $s = Read-Host -AsSecureString
  [Runtime.InteropServices.Marshal]::PtrToStringAuto([Runtime.InteropServices.Marshal]::SecureStringToBSTR($s))
}

if (-not (Get-Command az -ErrorAction SilentlyContinue)) {
  Write-Host 'Instalando Azure CLI...' -ForegroundColor Yellow
  winget install -e --id Microsoft.AzureCLI --accept-package-agreements --accept-source-agreements
  $env:Path += ';C:\Program Files\Microsoft SDKs\Azure\CLI2\wbin'
  if (-not (Get-Command az -ErrorAction SilentlyContinue)) {
    throw 'Azure CLI instalado. Cierra esta ventana, abre PowerShell nuevo y corre .\DESPLEGAR.ps1 otra vez.'
  }
}

if (-not $SoloPaso2) {
  Write-Host "`nPASO 1/2: creando Azure (unos 10 min, se abrira el login)..." -ForegroundColor Cyan
  & .\scripts\01-azure-infra.ps1
} else {
  az login | Out-Null
}

Write-Host "`nPASO 2/2: conectando Azure DevOps..." -ForegroundColor Cyan
Write-Host 'Necesito 2 tokens (no se ven al pegar; clic derecho para pegar y Enter).'
$env:AZURE_DEVOPS_EXT_PAT        = Pedir '1) Token de AZURE DEVOPS (dev.azure.com > User settings > Personal access tokens > New, Full access):'
$env:AZURE_DEVOPS_EXT_GITHUB_PAT = Pedir '2) Token de GITHUB (repo + admin:repo_hook):'
& .\scripts\02-azure-devops.ps1

Write-Host "`nLISTO. Ahora en Azure DevOps: Pipelines > periodismo-ci-cd > Run pipeline." -ForegroundColor Green
