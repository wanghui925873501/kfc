param(
    [switch]$ResetApp,
    [switch]$NoLaunch
)

. (Join-Path $PSScriptRoot 'common.ps1')

$capturePathFile = Join-Path $script:PidDir 'capture.path'
$proxyListening = Test-ListeningPort -Port 8080
if (-not $proxyListening) {
    $captureStamp = Get-Date -Format 'yyyyMMdd-HHmmss'
    $capturePath = Join-Path $script:CaptureDir "kfc-$captureStamp.mitm"
    Set-Content -LiteralPath $capturePathFile -Value $capturePath -Encoding utf8
    $proxyArgs = @(
        '--listen-host', '0.0.0.0',
        '--listen-port', '8080',
        '--web-host', '127.0.0.1',
        '--web-port', '8081',
        '--no-web-open-browser',
        '--set', "confdir=$script:MitmHome",
        '--set', 'block_global=false'
    )
    $proxyProcess = Start-Process -FilePath $script:MitmWeb -ArgumentList $proxyArgs -WindowStyle Hidden `
        -RedirectStandardOutput (Join-Path $script:LogDir 'mitmweb.out.log') `
        -RedirectStandardError (Join-Path $script:LogDir 'mitmweb.err.log') -PassThru
    Set-Content -LiteralPath (Join-Path $script:PidDir 'mitmweb.pid') -Value $proxyProcess.Id -Encoding ascii

    $proxyDeadline = (Get-Date).AddSeconds(30)
    do {
        Start-Sleep -Milliseconds 500
        $proxyListening = Test-ListeningPort -Port 8080
    } while (-not $proxyListening -and (Get-Date) -lt $proxyDeadline)
    if (-not $proxyListening) {
        throw 'mitmweb did not start listening on port 8080. See logs\mitmweb.err.log.'
    }
}

if (-not (Test-Path -LiteralPath $capturePathFile -PathType Leaf)) {
    $captureStamp = Get-Date -Format 'yyyyMMdd-HHmmss'
    $capturePath = Join-Path $script:CaptureDir "kfc-$captureStamp.mitm"
    Set-Content -LiteralPath $capturePathFile -Value $capturePath -Encoding utf8
}

$captureUiUrl = 'http://127.0.0.1:8081'
$webLogPath = Join-Path $script:LogDir 'mitmweb.out.log'
$webLogDeadline = (Get-Date).AddSeconds(10)
do {
    if (Test-Path -LiteralPath $webLogPath -PathType Leaf) {
        $webLogLine = Get-Content -LiteralPath $webLogPath |
            Select-String -Pattern 'Web server listening at (http://\S+)' |
            Select-Object -Last 1
        if ($null -ne $webLogLine -and $webLogLine.Line -match 'Web server listening at (http://\S+)') {
            $captureUiUrl = $Matches[1]
            break
        }
    }
    Start-Sleep -Milliseconds 250
} while ((Get-Date) -lt $webLogDeadline)
Set-Content -LiteralPath (Join-Path $script:PidDir 'mitmweb.url') -Value $captureUiUrl -Encoding utf8

& $script:Adb start-server | Out-Null
$serial = Get-EmulatorSerial
if ($null -eq $serial) {
    $emulatorArgs = @(
        '-avd', $script:AvdName,
        '-writable-system',
        '-no-snapshot-load',
        '-gpu', 'auto',
        '-memory', '4096',
        '-cores', '4',
        '-netdelay', 'none',
        '-netspeed', 'full'
    )
    $emulatorProcess = Start-Process -FilePath $script:Emulator -ArgumentList $emulatorArgs `
        -RedirectStandardOutput (Join-Path $script:LogDir 'emulator.out.log') `
        -RedirectStandardError (Join-Path $script:LogDir 'emulator.err.log') -PassThru
    Set-Content -LiteralPath (Join-Path $script:PidDir 'emulator.pid') -Value $emulatorProcess.Id -Encoding ascii

    $deviceDeadline = (Get-Date).AddMinutes(2)
    do {
        Start-Sleep -Seconds 2
        $serial = Get-EmulatorSerial
    } while ($null -eq $serial -and (Get-Date) -lt $deviceDeadline)
    if ($null -eq $serial) {
        throw 'Android emulator was not detected by adb within two minutes.'
    }
}

Wait-ForAndroidBoot -Serial $serial

$installedPackage = & $script:Adb -s $serial shell pm path $script:PackageName 2>$null
if (($installedPackage -join '') -notmatch '^package:') {
    & $script:Adb -s $serial install -r -g $script:ApkPath
    if ($LASTEXITCODE -ne 0) {
        throw 'APK installation failed.'
    }
}

& $script:Adb -s $serial shell settings put global http_proxy '10.0.2.2:8080' | Out-Null

$caPath = Join-Path $script:MitmHome 'mitmproxy-ca-cert.cer'
$caDeadline = (Get-Date).AddSeconds(20)
while (-not (Test-Path -LiteralPath $caPath -PathType Leaf) -and (Get-Date) -lt $caDeadline) {
    Start-Sleep -Milliseconds 500
}
if (-not (Test-Path -LiteralPath $caPath -PathType Leaf)) {
    throw 'mitmproxy CA certificate was not generated.'
}

$certificateHash = ((& $script:Python (Join-Path $PSScriptRoot 'cert_hash.py') $caPath) -join '').Trim()
if ($certificateHash -notmatch '^[0-9a-f]{8}$') {
    throw "Unexpected certificate hash: $certificateHash"
}
$certificateTarget = "/system/etc/security/cacerts/$certificateHash.0"

& $script:Adb -s $serial root | Out-Null
& $script:Adb -s $serial wait-for-device | Out-Null
& $script:Adb -s $serial remount
if ($LASTEXITCODE -ne 0) {
    throw 'The emulator system partition could not be remounted. Stop the lab, then start it again so -writable-system is applied.'
}
& $script:Adb -s $serial push $caPath $certificateTarget
if ($LASTEXITCODE -ne 0) {
    throw 'Copying the mitmproxy CA certificate into the emulator failed.'
}
& $script:Adb -s $serial shell "chmod 644 $certificateTarget && chown root:root $certificateTarget"
if ($LASTEXITCODE -ne 0) {
    throw 'Installing the mitmproxy CA certificate in the test emulator failed.'
}

if ($ResetApp) {
    & $script:Adb -s $serial shell pm clear $script:PackageName | Out-Null
}

if (-not $NoLaunch) {
    & $script:Adb -s $serial shell am force-stop $script:PackageName | Out-Null
    & $script:Adb -s $serial shell am start -n "$script:PackageName/$script:LauncherActivity" | Out-Null
}

Write-Host "KFC capture lab is ready."
Write-Host "Emulator: $serial"
Write-Host "Proxy: 10.0.2.2:8080"
Write-Host "Capture UI: $captureUiUrl"
Write-Host "Capture files: $script:CaptureDir"
