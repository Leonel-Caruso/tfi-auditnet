$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $PSScriptRoot

function Invoke-Step([string]$label, [scriptblock]$action) {
    Write-Host ""
    Write-Host "=== $label ==="
    & $action
    if ($LASTEXITCODE -ne 0) {
        throw "$label falló con código $LASTEXITCODE."
    }
}

Push-Location (Join-Path $projectRoot "backend\management-service")
try {
    Invoke-Step "management-service / mvn test" { mvn test }
}
finally {
    Pop-Location
}

Push-Location (Join-Path $projectRoot "backend\audit-core-service")
try {
    Invoke-Step "audit-core-service / mvn test" { mvn test }
}
finally {
    Pop-Location
}

$frontend = Join-Path $projectRoot "frontend\web-app"
Push-Location $frontend
try {
    if (-not (Test-Path (Join-Path $frontend "node_modules"))) {
        Invoke-Step "frontend / npm install" { npm install }
    }
    Invoke-Step "frontend / npm run build" { npm run build }
}
finally {
    Pop-Location
}

Write-Host ""
Write-Host "Entrega 1: regresión automática finalizada correctamente."
