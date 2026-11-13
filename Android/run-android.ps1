$ErrorActionPreference = 'Stop'
$sdk = Join-Path $env:LOCALAPPDATA 'Android\Sdk'
$adb = Join-Path $sdk 'platform-tools\adb.exe'
$emulator = Join-Path $sdk 'emulator\emulator.exe'
$avdName = 'KnowledgeInterview_API34'
$projectDir = $PSScriptRoot
$apk = Join-Path $projectDir 'app\build\outputs\apk\debug\app-debug.apk'
if (!(Test-Path $adb) -or !(Test-Path $emulator)) { throw 'Android SDK tools not found' }
function Get-TargetEmulator {
    $lines = @(& $adb devices)
    foreach ($line in $lines) {
        if ($line -match '^(emulator-\d+)\s+device\s*$') {
            $candidate = $Matches[1]
            $info = @(& $adb -s $candidate emu avd name 2>$null)
            if ($info -contains $avdName) { return $candidate }
        }
    }
    return $null
}
# 检测正在启动但尚未连接 ADB 的模拟器，避免重复创建实例。
function Test-TargetEmulatorProcess {
    $processes = @(Get-CimInstance Win32_Process -Filter "Name = 'emulator.exe'" -ErrorAction SilentlyContinue)
    foreach ($process in $processes) {
        if ($process.CommandLine -and $process.CommandLine.Contains($avdName) -and $process.CommandLine -match '(^|\s)-avd(\s|=)') {
            return $true
        }
    }
    return $false
}

# 通过 Windows 任务计划程序独立启动模拟器，避免 OpenIDEA 停止脚本时连带关闭模拟器。
function Start-IndependentEmulator {
    $taskName = 'KnowledgeInterview-Emulator-API34'
    $userName = [Security.Principal.WindowsIdentity]::GetCurrent().Name
    $action = New-ScheduledTaskAction -Execute $emulator -Argument "-avd $avdName"
    $principal = New-ScheduledTaskPrincipal -UserId $userName -LogonType Interactive -RunLevel Limited
    $settings = New-ScheduledTaskSettingsSet -ExecutionTimeLimit ([TimeSpan]::Zero) -MultipleInstances IgnoreNew -AllowStartIfOnBatteries -DontStopIfGoingOnBatteries
    Register-ScheduledTask -TaskName $taskName -Action $action -Principal $principal -Settings $settings -Force | Out-Null
    Start-ScheduledTask -TaskName $taskName
}

$serial = Get-TargetEmulator
if ($serial) {
    Write-Output "Reusing Android emulator: $serial ($avdName)"
} elseif (Test-TargetEmulatorProcess) {
    Write-Output "Android emulator is already starting: $avdName"
} else {
    Write-Output "Starting independent Android emulator: $avdName"
    Start-IndependentEmulator
}
$deadline = (Get-Date).AddMinutes(5)
while (-not $serial -and (Get-Date) -lt $deadline) {
    Start-Sleep -Seconds 3
    $serial = Get-TargetEmulator
}
if (-not $serial) { throw "Emulator $avdName did not connect to ADB" }
Write-Output "Target emulator: $serial ($avdName)"
$booted = $false
while ((Get-Date) -lt $deadline) {
    $boot = (& $adb -s $serial shell getprop sys.boot_completed | Out-String).Trim()
    if ($boot -eq '1') { $booted = $true; break }
    Start-Sleep -Seconds 3
}
if (-not $booted) { throw 'Android emulator boot timed out' }
Write-Output 'Building frontend for Android WebView...'
$frontendDir = Join-Path $projectDir '..\frontend'
$frontendDist = Join-Path $frontendDir 'dist'
Push-Location $frontendDir
try {
    & npm.cmd run build
    if ($LASTEXITCODE -ne 0) { throw "Frontend build failed: $LASTEXITCODE" }
} finally { Pop-Location }
if (!(Test-Path (Join-Path $frontendDist 'index.html'))) { throw 'Frontend dist/index.html not found' }
$assetsRoot = Join-Path $projectDir 'app\src\main\assets'
$assetsDir = Join-Path $assetsRoot 'www'
$staging = Join-Path $assetsRoot 'www.stage'
New-Item -ItemType Directory -Force -Path $assetsRoot | Out-Null
if (Test-Path $staging) { Remove-Item -LiteralPath $staging -Recurse -Force }
New-Item -ItemType Directory -Path $staging | Out-Null
Get-ChildItem -LiteralPath $frontendDist -Force | Copy-Item -Destination $staging -Recurse -Force
if (!(Test-Path (Join-Path $staging 'index.html'))) { throw 'Frontend assets staging failed' }
if (Test-Path $assetsDir) { Remove-Item -LiteralPath $assetsDir -Recurse -Force }
Move-Item -LiteralPath $staging -Destination $assetsDir
Write-Output 'Frontend assets copied into Android app assets/www'
Write-Output 'Building Android debug APK...'
Push-Location $projectDir
try {
    & (Join-Path $projectDir 'gradlew.bat') ':app:assembleDebug'
    if ($LASTEXITCODE -ne 0) { throw "Gradle build failed: $LASTEXITCODE" }
} finally { Pop-Location }
if (!(Test-Path $apk)) { throw "APK not found: $apk" }
Write-Output "Installing APK to $serial..."
& $adb -s $serial install -r $apk
if ($LASTEXITCODE -ne 0) { throw 'APK installation failed' }
Write-Output 'Forwarding emulator localhost:8080 to Windows backend port 8080...'
& $adb -s $serial reverse 'tcp:8080' 'tcp:8080'
if ($LASTEXITCODE -ne 0) { throw 'ADB reverse port forwarding failed' }
& $adb -s $serial shell am force-stop 'com.knowledgeinterview.android'
Write-Output 'Launching MainActivity...'
& $adb -s $serial shell am start -n 'com.knowledgeinterview.android/.MainActivity'
if ($LASTEXITCODE -ne 0) { throw 'Android app launch failed' }
Write-Output "Android app launched on $serial"