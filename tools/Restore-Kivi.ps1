<#
.SYNOPSIS
Restore the original Home, or all applications disabled in the tested KIVI profile.
.DESCRIPTION
For the tested KIVI 4K Android TV (bangbae, Android 11 / API 30) only.
No application is removed and no application data is cleared.
Default Scope=Home restores only the original launcher. Scope=All also restores
the 14 optional applications/components changed in the documented test.
The two applications that were already disabled before the test are untouched.
#>
[CmdletBinding(SupportsShouldProcess=$true, ConfirmImpact='Low')]
param(
    [Parameter(Mandatory=$true)]
    [ValidatePattern('^(?:\d{1,3}\.){3}\d{1,3}:\d{1,5}$')]
    [string]$Address,
    [string]$AdbPath=(Join-Path $PSScriptRoot 'adb.exe'),
    [ValidateSet('Home','All')]
    [string]$Scope='Home'
)
$ErrorActionPreference='Stop'
if(-not (Test-Path -LiteralPath $AdbPath -PathType Leaf)){
    $adbCommand=Get-Command $AdbPath -ErrorAction SilentlyContinue
    if(-not $adbCommand){throw 'ADB not found. Supply -AdbPath pointing to the official platform-tools adb.exe.'}
    $AdbPath=$adbCommand.Source
}
function Invoke-RestoreAdb {
    $adbArguments=$args
    $result=& $AdbPath -s $Address @adbArguments
    if($LASTEXITCODE -ne 0){throw "ADB command failed: $($adbArguments -join ' ')"}
    return $result
}
& $AdbPath connect $Address | Write-Output
if($LASTEXITCODE -ne 0){throw 'Unable to connect to the television.'}
if((Invoke-RestoreAdb get-state | Out-String).Trim() -ne 'device'){
    throw 'Authorize this computer in the debugging prompt on the television first.'
}
$brand=(Invoke-RestoreAdb shell getprop ro.product.brand | Out-String).Trim()
$device=(Invoke-RestoreAdb shell getprop ro.product.device | Out-String).Trim()
$api=(Invoke-RestoreAdb shell getprop ro.build.version.sdk | Out-String).Trim()
if($brand -ne 'KIVI' -or $device -ne 'bangbae' -or $api -ne '30'){
    throw 'This recovery profile is only for the tested KIVI bangbae on Android 11 / API 30. No packages were changed.'
}
$packages=@('com.google.android.tvlauncher')
if($Scope -eq 'All'){
    $packages=@(
        'ru.ivi.client',
        'rtb.mobile.android',
        'com.kinopub',
        'com.esaba.downloader',
        'com.google.android.youtube.tvmusic',
        'com.google.android.play.games',
        'com.netflix.ninja',
        'com.amazon.amazonvideo.livingroom',
        'com.google.android.videos',
        'kivi.media',
        'tv.anoki.acr.controller',
        'com.google.android.tvrecommendations',
        'com.google.android.leanbacklauncher.partnercustomizer',
        'fusion.android.tv.demo',
        'com.google.android.tvlauncher'
    )
}
$installed=@(Invoke-RestoreAdb shell pm list packages | ForEach-Object {$_ -replace '^package:',''})
if($installed -notcontains 'com.google.android.tvlauncher'){
    throw 'The original Home package is missing. This script does not download or reinstall system packages.'
}
foreach($package in $packages){
    if($installed -notcontains $package){Write-Warning "Not installed; skipped: $package"; continue}
    # Every package in this exact test profile originally had enabled=0 (default).
    if($PSCmdlet.ShouldProcess($package,'Restore original default package state')){
        Invoke-RestoreAdb shell pm default-state --user 0 $package | Write-Output
    }
}
if($PSCmdlet.ShouldProcess('Original KIVI Home','Assign default Home and open it')){
    Invoke-RestoreAdb shell cmd package set-home-activity --user 0 com.google.android.tvlauncher/.MainActivity | Write-Output
    Invoke-RestoreAdb shell input keyevent KEYCODE_HOME | Out-Null
    Invoke-RestoreAdb shell cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME | Write-Output
    Write-Output 'Original Home restored. TV Home Launcher and its saved settings remain installed.'
}
