param(
    [string]$FrontendHost = "localhost",
    [string]$BackendHost = "localhost"
)

$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
$backend = Join-Path $root "backend"
$frontend = Join-Path $root "frontend"

Write-Host "Starting Art Decor local development stack..." -ForegroundColor Cyan
Write-Host "PostgreSQL: artdecor / artdecor_dev_password / artdecor_workforce"

Set-Location $root
docker compose up -d postgres

Write-Host "Waiting for PostgreSQL health check..."
for ($i = 0; $i -lt 30; $i++) {
    $status = docker inspect --format '{{.State.Health.Status}}' art-decor-postgres 2>$null
    if ($status -eq "healthy") {
        break
    }
    Start-Sleep -Seconds 2
}

$finalStatus = docker inspect --format '{{.State.Health.Status}}' art-decor-postgres 2>$null
if ($finalStatus -ne "healthy") {
    throw "PostgreSQL did not become healthy. Run 'docker logs art-decor-postgres' for details."
}

$env:SPRING_PROFILES_ACTIVE = "dev"
$env:DB_URL = "jdbc:postgresql://localhost:5432/artdecor_workforce"
$env:DB_USERNAME = "artdecor"
$env:DB_PASSWORD = "artdecor_dev_password"
$env:APP_BOOTSTRAP_ENABLED = "true"
$env:APP_BOOTSTRAP_EMPLOYEE_PASSWORD = "Employee123!"
$env:CORS_ALLOWED_ORIGINS = "http://localhost:5173,http://127.0.0.1:5173,http://${BackendHost}:5173"
$env:CORS_ALLOWED_ORIGIN_PATTERNS = "http://localhost:*,http://127.0.0.1:*,http://192.168.*.*:*,http://10.*.*.*:*,http://172.16.*.*:*,http://172.17.*.*:*,http://172.18.*.*:*,http://172.19.*.*:*,http://172.20.*.*:*,http://172.21.*.*:*,http://172.22.*.*:*,http://172.23.*.*:*,http://172.24.*.*:*,http://172.25.*.*:*,http://172.26.*.*:*,http://172.27.*.*:*,http://172.28.*.*:*,http://172.29.*.*:*,http://172.30.*.*:*,http://172.31.*.*:*"

$apiBaseUrl = "http://${BackendHost}:8080/api/v1"
if ($FrontendHost -eq "localhost") {
    $apiBaseUrl = "http://localhost:8080/api/v1"
}
$env:VITE_API_BASE_URL = $apiBaseUrl

Write-Host ""
Write-Host "Open two terminals from this project root and run:" -ForegroundColor Green
Write-Host "Terminal 1:"
Write-Host "  cd backend"
Write-Host "  `$env:SPRING_PROFILES_ACTIVE='dev'"
Write-Host "  `$env:DB_URL='jdbc:postgresql://localhost:5432/artdecor_workforce'"
Write-Host "  `$env:DB_USERNAME='artdecor'"
Write-Host "  `$env:DB_PASSWORD='artdecor_dev_password'"
Write-Host "  `$env:APP_BOOTSTRAP_ENABLED='true'"
Write-Host "  `$env:APP_BOOTSTRAP_EMPLOYEE_PASSWORD='Employee123!'"
Write-Host "  mvn.cmd spring-boot:run"
Write-Host ""
Write-Host "Terminal 2:"
Write-Host "  cd frontend"
Write-Host "  `$env:VITE_API_BASE_URL='$apiBaseUrl'"
Write-Host "  npm.cmd run dev -- --host 0.0.0.0"
Write-Host ""
Write-Host "Laptop URL: http://localhost:5173"
Write-Host "Phone URL:  http://<your-laptop-ip>:5173"
Write-Host "Backend health: http://${BackendHost}:8080/actuator/health"
