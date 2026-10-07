. (Join-Path $PSScriptRoot 'common.ps1')

if ((Test-ListeningPort -Port 8081) -and (Test-Path -LiteralPath (Join-Path $script:PidDir 'capture.path'))) {
    try {
        & (Join-Path $PSScriptRoot 'Save-KfcCapture.ps1')
    } catch {
        Write-Warning "Could not snapshot live flows before shutdown: $($_.Exception.Message)"
    }
}

$serial = Get-EmulatorSerial
if ($null -ne $serial) {
    & $script:Adb -s $serial shell settings put global http_proxy ':0' | Out-Null
    & $script:Adb -s $serial emu kill | Out-Null
}

$proxyOwners = Get-ListeningProcessIds -Port 8080
foreach ($processId in $proxyOwners) {
    Stop-Process -Id $processId -ErrorAction SilentlyContinue
}

$pidPath = Join-Path $script:PidDir 'mitmweb.pid'
if (Test-Path -LiteralPath $pidPath -PathType Leaf) {
    $savedPid = (Get-Content -LiteralPath $pidPath -Raw).Trim()
    if ($savedPid -match '^[0-9]+$') {
        & taskkill.exe /PID $savedPid /T /F 2>$null | Out-Null
    }
}

Write-Host 'KFC capture lab stopped. Existing .mitm capture files were preserved.'
