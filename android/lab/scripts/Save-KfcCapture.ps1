. (Join-Path $PSScriptRoot 'common.ps1')

$webUrlPath = Join-Path $script:PidDir 'mitmweb.url'
$capturePathFile = Join-Path $script:PidDir 'capture.path'
if (-not (Test-Path -LiteralPath $webUrlPath -PathType Leaf)) {
    throw 'The current mitmweb URL is unknown. Start the lab first.'
}
if (-not (Test-Path -LiteralPath $capturePathFile -PathType Leaf)) {
    throw 'The current capture path is unknown. Start a new lab session first.'
}

$webUrl = (Get-Content -LiteralPath $webUrlPath -Raw).Trim()
$capturePath = [IO.Path]::GetFullPath((Get-Content -LiteralPath $capturePathFile -Raw).Trim())
$captureRoot = [IO.Path]::GetFullPath($script:CaptureDir)
if (-not $capturePath.StartsWith($captureRoot + '\') -or [IO.Path]::GetExtension($capturePath) -ne '.mitm') {
    throw "Refusing to write outside the capture directory: $capturePath"
}

$webSession = New-Object Microsoft.PowerShell.Commands.WebRequestSession
Invoke-WebRequest -Uri $webUrl -WebSession $webSession -TimeoutSec 15 | Out-Null
$temporaryPath = "$capturePath.partial"
Invoke-WebRequest -Uri 'http://127.0.0.1:8081/flows/dump' -WebSession $webSession `
    -OutFile $temporaryPath -TimeoutSec 120
if (-not (Test-Path -LiteralPath $temporaryPath -PathType Leaf)) {
    throw 'mitmweb did not return a capture file.'
}

[IO.File]::Move($temporaryPath, $capturePath, $true)
$capture = Get-Item -LiteralPath $capturePath
Write-Host "Saved $($capture.Length) bytes to $($capture.FullName)"
