# =============================================
# start.ps1 - Booking System Startup Script
# =============================================

$JAVA17 = "C:\Program Files\Java\jdk-17"
$env:JAVA_HOME = $JAVA17
$env:PATH = "$JAVA17\bin;" + $env:PATH

Write-Host "Using Java: $(java -version 2>&1 | Select-Object -First 1)" -ForegroundColor Cyan

# Kill anything on port 8080
$proc = Get-NetTCPConnection -LocalPort 8080 -ErrorAction SilentlyContinue |
        Select-Object -ExpandProperty OwningProcess -First 1
if ($proc) {
    Stop-Process -Id $proc -Force
    Write-Host "Killed old process on port 8080 (PID $proc)" -ForegroundColor Yellow
    Start-Sleep -Seconds 1
}

Write-Host "Starting Booking System..." -ForegroundColor Green
Write-Host "Swagger UI: http://localhost:8080/swagger-ui/index.html" -ForegroundColor Cyan
Write-Host ""

.\mvnw.cmd spring-boot:run
