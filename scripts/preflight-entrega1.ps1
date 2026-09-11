$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $PSScriptRoot
$envFile = Join-Path $projectRoot ".env"

Write-Host "Preflight de Entrega 1"
Write-Host "Proyecto: $projectRoot"

if (-not (Test-Path $envFile)) {
    throw "Falta .env. Copiá el .env del bloque validado o partí de .env.example."
}

& (Join-Path $PSScriptRoot "load-env.ps1")

if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
    throw "Docker no está disponible en PATH."
}
if (-not (Get-Command mvn -ErrorAction SilentlyContinue)) {
    throw "Maven no está disponible en PATH."
}
if (-not (Get-Command npm -ErrorAction SilentlyContinue)) {
    throw "npm no está disponible en PATH."
}

Write-Host ""
Write-Host "PostgreSQL / Compose:"
Push-Location $projectRoot
try {
    docker compose --env-file .env -f docker/compose.database.yaml ps
}
finally {
    Pop-Location
}

Write-Host ""
Write-Host "Puertos locales:"
foreach ($port in @(5432, 8081, 8082, 4321)) {
    $ipv4 = Test-NetConnection 127.0.0.1 -Port $port -WarningAction SilentlyContinue
    $local = Test-NetConnection localhost -Port $port -WarningAction SilentlyContinue

    $activo = $ipv4.TcpTestSucceeded -or $local.TcpTestSucceeded

    Write-Host ("{0}: {1}" -f $port, $(if ($activo) { "activo" } else { "libre/no iniciado" }))
}

Write-Host ""
Write-Host "Preflight completado. No se mostraron secretos."
