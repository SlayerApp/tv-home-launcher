# Building

[English](BUILD.en.md) | [Русский](BUILD.md)

Requires JDK 17, Android SDK Platform 34, and access to Google/Maven Central for the initial dependency download. The project uses Android Gradle Plugin 8.5.2 and Gradle Wrapper.

Set `ANDROID_HOME` to your installed SDK, or create your own `local.properties`. Do not commit that local file.

Windows:

```powershell
.\gradlew.bat testReleaseUnitTest lintRelease assembleRelease
```

macOS / Linux:

```sh
chmod +x gradlew
./gradlew testReleaseUnitTest lintRelease assembleRelease
```

Output: `app/build/outputs/apk/release/app-release.apk`.

The portable build `com.gathin.tvhome` version `3.7.1-portable8` uses versionCode 22, minSdk 26 and targetSdk 34.

## Signing and updates

The current configuration uses a local Android debug signing config. A build made on another computer will have a different signature. The source does not contain the published APK’s private signing key.

Android only allows an installed app to be updated with a compatible signature. An APK rebuilt locally with a different key cannot update the published APK. Future published builds need to preserve the original signing key outside the repository. Do not commit `.keystore`/`.jks` files, passwords or local settings.

## Publish a launcher release

Increase the Android version code for each new APK and set its version name. Run the release checks above and sign the APK with the existing key. Replace `downloads/TV-Home-Apple-Style.apk` and `downloads/SHA256.txt`, while keeping the same asset names. Add English and Russian notes in `docs/releases/X.Y.Z.md` and commit the files to `main`.

Push the tag `launcher-vX.Y.Z` for that commit, replacing `X.Y.Z` with the APK's version before its optional `-portable` suffix. The workflow checks the APK hash, package name, version and signing certificate before attaching the APK and checksum to the release. [Launcher releases](https://github.com/SlayerApp/tv-home-launcher/releases)

When updating the Windows and macOS installers, refresh the expected APK hash in `windows-installer/Build.ps1` and `macos-installer/script/build_and_run.sh`, then rebuild those installers with the same signed APK.

## Device validation

When changing native settings routes, test them on an actual TV and update `TESTED_TVS.en.md` and its Russian counterpart. Successful compilation for API 26+ does not prove that Home, Bluetooth or restart navigation works on every firmware.
