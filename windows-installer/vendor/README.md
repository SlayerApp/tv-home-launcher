# Bundled Android Debug Bridge for Windows

`adb-windows-37.0.1.zip` contains ADB and its Windows runtime DLLs from Google's Android SDK Platform-Tools 37.0.1. The original file contents, package version metadata and complete upstream `NOTICE.txt` are retained. The installer extracts this bundle and runs ADB as a separate process.

- Upstream [Platform-Tools release and download](https://developer.android.com/tools/releases/platform-tools), original Windows archive SHA-256 `45F4D63113E895EBDE0C90F194099A4676B6AC653BD28D54314A9E022BBC1A99`.
- [ADB source and Apache 2.0 license](https://android.googlesource.com/platform/packages/modules/adb/) and [Windows USB support source](https://android.googlesource.com/platform/development/+/refs/heads/main/host/windows/usb/).
- [mingw-w64 winpthreads source and licenses](https://github.com/mingw-w64/mingw-w64/tree/master/mingw-w64-libraries/winpthreads).

The full upstream third-party notices are included inside the bundle at `platform-tools/NOTICE.txt` and are extracted with ADB to `%LOCALAPPDATA%\TVHomeInstaller\adb-37.0.1`.
