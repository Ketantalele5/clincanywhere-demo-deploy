$ErrorActionPreference = 'Stop'
$listeners = Get-NetTCPConnection -State Listen -LocalPort 8080 -ErrorAction SilentlyContinue
if (-not $listeners) {
    exit 0
}

try {
    $health = Invoke-RestMethod -Uri 'http://127.0.0.1:8080/api/health' -TimeoutSec 2
} catch {
    $health = $null
}

if ($health.application -ne 'ClincyAnywhere API') {
    Write-Error 'Port 8080 is occupied by another service; leaving it untouched.'
    exit 1
}

$listeners.OwningProcess | Sort-Object -Unique | ForEach-Object {
    Stop-Process -Id $_ -Force
}

$deadline = (Get-Date).AddSeconds(15)
do {
    Start-Sleep -Milliseconds 250
    $listeners = Get-NetTCPConnection -State Listen -LocalPort 8080 -ErrorAction SilentlyContinue
} while ($listeners -and (Get-Date) -lt $deadline)

if ($listeners) {
    Write-Error 'Could not stop the existing ClincyAnywhere API on port 8080.'
    exit 1
}

Write-Host 'Stopped existing ClincyAnywhere API.'
