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

Current portable app: `com.gathin.tvhome`, versionCode 12, versionName `3.2-portable2`, minSdk 26, targetSdk 34. There are no CPU-architecture-specific native libraries.

## Signing and updates

The current configuration uses a local Android debug signing config. A build made on another computer will have a different signature. The source does not contain the published APK’s private signing key.

Android only allows an installed app to be updated with a compatible signature. An APK rebuilt locally with a different key cannot update the published APK. Future published builds need to preserve the original signing key outside the repository. Do not commit `.keystore`/`.jks` files, passwords or local settings.

## Device validation

When changing native settings routes, test them on an actual TV and update `TESTED_TVS.en.md` and its Russian counterpart. Successful compilation for API 26+ does not prove that Home, Bluetooth or restart navigation works on every firmware.
