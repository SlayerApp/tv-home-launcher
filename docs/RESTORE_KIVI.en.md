# Restore the original KIVI Home

[Русский](RESTORE_KIVI.md) | [English](RESTORE_KIVI.en.md)

This profile is for the tested **KIVI 4K Android TV, device bangbae, Android 11 / API 30**. It is not intended for Xiaomi or other televisions. The launcher APK does not disable system applications; the KIVI cleanup was a separate operation approved by its owner.

The original Home was only disabled. Its files and data are preserved. Restoring it does not require a factory reset, root access or flashing. TV Home Launcher and its saved settings remain installed.

## Prepare

1. Download [Restore-Kivi.ps1](../tools/Restore-Kivi.ps1) using **Raw → Download raw file**, or obtain it from the repository source ZIP.
2. Download the official [Android SDK Platform-Tools for Windows](https://developer.android.com/tools/releases/platform-tools) and extract the archive. Locate `adb.exe`.
3. Turn on the TV. Connect the computer and TV to the same home network. Enable USB/ADB debugging in KIVI settings, find the current TV IP address, and authorize the computer when prompted on the TV. Network ADB does not require a USB cable.
4. Open PowerShell in the script folder. If Windows blocks this downloaded file, review its contents and then run `Unblock-File .\Restore-Kivi.ps1`.

## Restore only the original Home

Replace the example address and ADB path with **your own** values:

```powershell
.\Restore-Kivi.ps1 -Address '192.168.1.50:5555' -AdbPath 'C:\platform-tools\adb.exe'
```

The script checks the device and Android API, restores the default state of `com.google.android.tvlauncher`, assigns it as Home, and presses Home. Other disabled applications stay disabled.

## Restore the entire cleanup profile

```powershell
.\Restore-Kivi.ps1 -Address '192.168.1.50:5555' -AdbPath 'C:\platform-tools\adb.exe' -Scope All
```

This restores the original states of 15 packages: stock Home, IVI, RUTUBE, KinoPub, Downloader, YouTube Music, Play Games, Netflix, Prime Video, Google Play Movies, KIVI Media, Anoki ACR, recommendations, the partner Home customizer and the retail demo. VK Video, YouTube, Kinopoisk and Happ were preserved during cleanup. The two packages disabled before the test are left untouched.

Add `-WhatIf` to preview without enabling packages or opening the original Home. The script still connects to the TV and checks its model. An unavailable TV or a different model/API stops the script before package changes.

## Switch back to TV Home Launcher

Keep TV Home Launcher installed. On this same tested KIVI, the original Home can be reversibly disabled again:

```powershell
& 'C:\platform-tools\adb.exe' -s '192.168.1.50:5555' shell am start -n com.gathin.tvhome/.MainActivity
& 'C:\platform-tools\adb.exe' -s '192.168.1.50:5555' shell pm disable-user --user 0 com.google.android.tvlauncher
& 'C:\platform-tools\adb.exe' -s '192.168.1.50:5555' shell cmd package set-home-activity --user 0 com.gathin.tvhome/.MainActivity
& 'C:\platform-tools\adb.exe' -s '192.168.1.50:5555' shell input keyevent KEYCODE_HOME
```

Debugging can be turned off in TV settings when setup is complete. Enable it again before a future recovery.

See [tested televisions](TESTED_TVS.en.md) for actual verification results.
