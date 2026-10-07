. (Join-Path $PSScriptRoot 'common.ps1')

$serial = Get-EmulatorSerial
$proxyListening = Test-ListeningPort -Port 8080
$webListening = Test-ListeningPort -Port 8081
$capture = Get-ChildItem -LiteralPath $script:CaptureDir -Filter '*.mitm' -File -ErrorAction SilentlyContinue |
    Sort-Object LastWriteTime -Descending | Select-Object -First 1
$webUrlPath = Join-Path $script:PidDir 'mitmweb.url'
$webUiUrl = if (Test-Path -LiteralPath $webUrlPath -PathType Leaf) {
    (Get-Content -LiteralPath $webUrlPath -Raw).Trim()
} else {
    'http://127.0.0.1:8081'
}

$status = [ordered]@{
    emulator = if ($null -eq $serial) { 'stopped' } else { $serial }
    android_booted = $false
    apk_installed = $false
    device_proxy = $null
    proxy_port_8080 = $proxyListening
    web_ui_port_8081 = $webListening
    web_ui_url = $webUiUrl
    latest_capture = if ($null -eq $capture) { $null } else { $capture.FullName }
    latest_capture_bytes = if ($null -eq $capture) { 0 } else { $capture.Length }
}

if ($null -ne $serial) {
    $status.android_booted = (((& $script:Adb -s $serial shell getprop sys.boot_completed 2>$null) -join '').Trim() -eq '1')
    $status.apk_installed = (((& $script:Adb -s $serial shell pm path $script:PackageName 2>$null) -join '').Trim() -match '^package:')
    $status.device_proxy = ((& $script:Adb -s $serial shell settings get global http_proxy 2>$null) -join '').Trim()
}

[pscustomobject]$status | Format-List
