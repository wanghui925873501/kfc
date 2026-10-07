$ErrorActionPreference = 'Stop'

$script:LabRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$script:SdkRoot = Join-Path $script:LabRoot 'sdk'
$script:Adb = Join-Path $script:SdkRoot 'platform-tools\adb.exe'
$script:Emulator = Join-Path $script:SdkRoot 'emulator\emulator.exe'
$script:MitmWeb = Join-Path $script:LabRoot 'mitmproxy-venv\Scripts\mitmweb.exe'
$script:Python = Join-Path $script:LabRoot 'mitmproxy-venv\Scripts\python.exe'
$script:MitmHome = Join-Path $script:LabRoot 'mitmproxy-home'
$script:CaptureDir = Join-Path $script:LabRoot 'captures'
$script:LogDir = Join-Path $script:LabRoot 'logs'
$script:PidDir = Join-Path $script:LabRoot 'run'
$script:AvdHome = Join-Path $script:LabRoot 'avd'
$script:AvdName = 'kfc_capture_api23'
$script:PackageName = 'com.yek.android.kfc.activitys'
$script:LauncherActivity = 'com.yum.brandkfc.SplashAct'
$script:ApkPath = [IO.Path]::GetFullPath((Join-Path $script:LabRoot '..\KFC_Brand.apk'))

$env:ANDROID_HOME = $script:SdkRoot
$env:ANDROID_SDK_ROOT = $script:SdkRoot
$env:ANDROID_USER_HOME = Join-Path $script:LabRoot 'android-user-home'
$env:ANDROID_AVD_HOME = $script:AvdHome
$env:PYTHONUNBUFFERED = '1'

foreach ($requiredPath in @($script:Adb, $script:Emulator, $script:MitmWeb, $script:Python, $script:ApkPath)) {
    if (-not (Test-Path -LiteralPath $requiredPath -PathType Leaf)) {
        throw "Required file is missing: $requiredPath"
    }
}

New-Item -ItemType Directory -Force -Path $script:CaptureDir, $script:LogDir, $script:PidDir, $script:MitmHome | Out-Null

function Get-EmulatorSerial {
    $line = & $script:Adb devices | Select-String -Pattern '^emulator-[0-9]+\s+device$' | Select-Object -First 1
    if ($null -eq $line) {
        return $null
    }
    return (($line.Line -split '\s+')[0]).Trim()
}

function Get-ListeningProcessIds {
    param([Parameter(Mandatory = $true)][int]$Port)

    $pattern = "^\s*TCP\s+\S+:$Port\s+\S+\s+LISTENING\s+([0-9]+)\s*$"
    $processIds = foreach ($line in (& netstat.exe -ano -p TCP)) {
        if ($line -match $pattern) {
            [int]$Matches[1]
        }
    }
    return @($processIds | Select-Object -Unique)
}

function Test-ListeningPort {
    param([Parameter(Mandatory = $true)][int]$Port)

    return @((Get-ListeningProcessIds -Port $Port)).Count -gt 0
}

function Wait-ForAndroidBoot {
    param(
        [Parameter(Mandatory = $true)][string]$Serial,
        [int]$TimeoutSeconds = 240
    )

    & $script:Adb -s $Serial wait-for-device | Out-Null
    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    do {
        $bootComplete = ((& $script:Adb -s $Serial shell getprop sys.boot_completed 2>$null) -join '').Trim()
        if ($bootComplete -eq '1') {
            return
        }
        Start-Sleep -Seconds 3
    } while ((Get-Date) -lt $deadline)

    throw "Android emulator did not finish booting within $TimeoutSeconds seconds."
}
