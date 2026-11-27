param([string]$OutputDirectory, [string]$VpnApk)

$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$compiler = Join-Path $env:WINDIR 'Microsoft.NET\Framework64\v4.0.30319\csc.exe'
if (-not (Test-Path -LiteralPath $compiler)) {
    $compiler = Join-Path $env:WINDIR 'Microsoft.NET\Framework\v4.0.30319\csc.exe'
}
$portable = Join-Path $root 'export\TV-Home-Apple-Style.apk'
if (-not (Test-Path -LiteralPath $portable -PathType Leaf)) {
    $portable = Join-Path $root 'downloads\TV-Home-Apple-Style.apk'
}
if ((Get-FileHash -LiteralPath $portable).Hash -ne '5FB7F30981EBB9304B2A9B30D13E2E813566118CC7713D131EE43837DDD30467') {
    throw "Launcher APK checksum mismatch: $portable"
}
$adbBundle = Join-Path $PSScriptRoot 'vendor\adb-windows-37.0.1.zip'
if ((Get-FileHash -LiteralPath $adbBundle).Hash -ne '270E6BC5473F30B0760224FB429936A876E04DC8E2C5E3BC3489020BADA6D7C0') {
    throw "ADB 37.0.1 checksum mismatch: $adbBundle"
}
$exeName = 'TV-Home-Installer.exe'
$vpnResource = @()
if ($PSBoundParameters.ContainsKey('VpnApk')) {
    $VpnApk = (Get-Item -LiteralPath $VpnApk).FullName
    if ((Get-FileHash -LiteralPath $VpnApk).Hash -ne '169F39F9D79AB4D6343588C3ED107E9113F1ECAF793E9ECD06A4FE7E6CC6E7B5') {
        throw "VPN 4.10.1 checksum mismatch: $VpnApk"
    }
    $exeName = 'TV-Home-Installer-Offline.exe'
    $vpnResource = @("/resource:$VpnApk,happ.apk")
}
if (-not $OutputDirectory) { $OutputDirectory = Join-Path $root 'export' }
$OutputDirectory = (New-Item -Path $OutputDirectory -ItemType Directory -Force).FullName
$output = Join-Path $OutputDirectory $exeName
$guideName = 'TV-Home-USB-Instruction.txt'
$guide = Join-Path $OutputDirectory $guideName
$icon = Join-Path $PSScriptRoot 'TV-Home-Installer.ico'
$guideText = [IO.File]::ReadAllText((Join-Path $PSScriptRoot $guideName)) -replace '\r?\n', "`r`n"
[IO.File]::WriteAllText($guide, $guideText, [Text.UTF8Encoding]::new($true))
& $compiler /nologo /target:winexe /platform:anycpu /optimize+ /codepage:65001 `
    /reference:System.Windows.Forms.dll /reference:System.Drawing.dll /reference:System.IO.Compression.dll /reference:System.Net.Http.dll "/out:$output" `
    "/win32icon:$icon" "/resource:$icon,installer.ico" `
    "/resource:$portable,launcher.apk" "/resource:$guide,usb-guide.txt" "/resource:$adbBundle,adb-windows.zip" `
    @vpnResource `
    (Join-Path $PSScriptRoot 'InstallerCore.cs') (Join-Path $PSScriptRoot 'AdbTools.cs') `
    (Join-Path $PSScriptRoot 'VpnPackage.cs') (Join-Path $PSScriptRoot 'InstallerWindow.cs')
if ($LASTEXITCODE -ne 0) { throw 'Windows installer compilation failed.' }
Get-Item -LiteralPath $output | Select-Object FullName, Length
