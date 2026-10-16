[CmdletBinding()]
param([switch]$Offline)

$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$backendPath = Join-Path $projectRoot 'backend'
$localConfig = Join-Path $backendPath 'src/main/resources/application-local.yml'

if (-not (Test-Path -LiteralPath $localConfig)) {
    throw 'Missing backend/src/main/resources/application-local.yml. Provide the existing local development configuration first.'
}
if ([string]::IsNullOrWhiteSpace($env:DASHSCOPE_API_KEY)) {
    throw 'DASHSCOPE_API_KEY is missing in this process. Set it locally and restart the terminal or IDE; never put the key in tracked files.'
}

$configUri = ([System.Uri]([System.IO.Path]::GetFullPath($localConfig))).AbsoluteUri
$mavenArgs = @('-f', (Join-Path $backendPath 'pom.xml'))
if ($Offline) { $mavenArgs += '-o' }
$mavenArgs += @(
    'spring-boot:run',
    '-Dspring-boot.run.profiles=local,ai',
    "-Dspring-boot.run.arguments=--server.port=8080 --spring.config.additional-location=$configUri --crm.init.admin.enabled=false"
)

Write-Host 'Starting local AI backend on port 8080 with profiles local,ai. API key value is hidden.'
& mvn @mavenArgs
if ($LASTEXITCODE -ne 0) { throw "Backend exited with code $LASTEXITCODE." }
