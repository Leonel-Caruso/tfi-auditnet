$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $PSScriptRoot
$envFile = Join-Path $projectRoot ".env"

if (-not (Test-Path $envFile)) {
    throw "No se encontró $envFile. Copiá .env.example a .env y completá los valores locales."
}

Get-Content $envFile | ForEach-Object {
    $line = $_.Trim()
    if (-not $line -or $line.StartsWith("#")) {
        return
    }

    $parts = $line -split "=", 2
    if ($parts.Count -ne 2) {
        return
    }

    $name = $parts[0].Trim()
    $value = $parts[1].Trim()
    [Environment]::SetEnvironmentVariable($name, $value, "Process")
}

if (-not $env:POSTGRES_DB -or -not $env:POSTGRES_USER -or -not $env:POSTGRES_PASSWORD -or -not $env:POSTGRES_PORT) {
    throw "Faltan variables POSTGRES_* obligatorias en .env."
}

$env:DB_USERNAME = $env:POSTGRES_USER
$env:DB_PASSWORD = $env:POSTGRES_PASSWORD
$env:DB_JDBC_URL = "jdbc:postgresql://localhost:$($env:POSTGRES_PORT)/$($env:POSTGRES_DB)"

$required = @(
    "JWT_SECRET",
    "BOOTSTRAP_ADMIN_EMAIL",
    "BOOTSTRAP_ADMIN_USERNAME",
    "BOOTSTRAP_ADMIN_PASSWORD",
    "BOOTSTRAP_ORG_IDENTIFIER",
    "BOOTSTRAP_ORG_NAME"
)

foreach ($name in $required) {
    if (-not [Environment]::GetEnvironmentVariable($name, "Process")) {
        throw "Falta la variable $name en .env."
    }
}

if ($env:JWT_SECRET.Length -lt 32) {
    throw "JWT_SECRET debe tener al menos 32 caracteres."
}

# La misma clave simétrica se representa como JWK Base64URL para que
# SmallRye JWT pueda verificar correctamente los tokens HS256 en HTTP.
$secretBytes = [System.Text.Encoding]::UTF8.GetBytes($env:JWT_SECRET)
$secretBase64Url = [Convert]::ToBase64String($secretBytes)
$secretBase64Url = $secretBase64Url.TrimEnd('=').Replace('+', '-').Replace('/', '_')

$jwkJson = '{"kty":"oct","k":"' + $secretBase64Url + '","alg":"HS256"}'
$jwkBytes = [System.Text.Encoding]::UTF8.GetBytes($jwkJson)
$env:JWT_VERIFY_JWK = [Convert]::ToBase64String($jwkBytes)
$env:JWT_VERIFY_JWK = $env:JWT_VERIFY_JWK.TrimEnd('=').Replace('+', '-').Replace('/', '_')

Write-Host "Variables locales cargadas para esta terminal."
Write-Host "DB_USERNAME, DB_JDBC_URL, JWT_SECRET, JWT_VERIFY_JWK y variables BOOTSTRAP_* disponibles."
Write-Host "Los valores secretos no se muestran en pantalla."
