# TV Home Launcher

[English](README.en.md) | [Русский](README.md)

A lightweight Android TV / Google TV home launcher with an Apple TV-inspired interface.

**Portable build: 3.2-portable2.** Requires Android 8.0 or later. This is a launcher app, not Android or tvOS firmware. The current custom settings interface is in Russian; documentation is available in English and Russian.

## Download and install

- [Download the APK](downloads/TV-Home-Apple-Style.apk?raw=1)
- [Installation guide](docs/INSTALL.en.md)
- [SHA-256 checksum](downloads/SHA256.txt)

Copy the APK to a USB drive, open it with a file manager on the TV, and install it. Then select “Главный экран” (Home) as the default Home app if the firmware supports this. Some models require a separate, model-specific setup to change the default launcher.

Streaming apps, accounts, VPN subscriptions and an AirPlay receiver are not bundled. The launcher does not automatically disable system apps.

## Features

- Four large first-row tiles: kino.watch, YouTube, Kinopoisk and START; other installed TV apps appear below.
- Automatic discovery of newly installed TV apps and control over tile visibility.
- Inputs, VPN and network controls in a shared top capsule, with a separate settings button.
- A TV tile for the built-in tuner and custom labels for physical HDMI ports.
- A light settings menu with links to native Bluetooth, network, picture and sound settings.
- Weather with approximate city detection and automatic text sizing for longer city names.
- A restart shortcut that navigates to the native settings menu. The user confirms the actual restart.

The VPN button opens Happ, which must be installed separately. The TV’s original screensaver and its timers are not changed automatically.

## Screenshots

Captured from the main 3.2 build on a Xiaomi MiTV-MOEU1. The portable build uses the same interface with an embedded theme. The screenshots show the Russian interface.

**Home screen**

![Home screen with four tiles and weather](docs/screenshots/home.png)

**Settings**

![Launcher settings menu](docs/screenshots/settings.png)

**Inputs**

![HDMI and TV input chooser](docs/screenshots/inputs.png)

## Tested TVs

| Model | System | Status |
| --- | --- | --- |
| Xiaomi Mini LED 55 2025, MiTV-MOEU1 (lalaland) | Android TV 14 / API 34 | Interface and core functions tested in the main 3.2 build; details below |
| Other Android TV / Google TV models | Declared minimum: Android 8 / API 26 | Installation and operation have not been tested on real devices yet |

**Build distinction:** device checks on the Xiaomi used the main 3.2 build. The portable 3.2-portable2 APK in this repository is built separately and includes its theme and Home tile. Installation on a second TV has not been tested yet. The minimum Android version in the manifest does not mean that every model has been tested.

See [TESTED_TVS.en.md](docs/TESTED_TVS.en.md) for actual checks and limitations.

## Weather and permissions

Weather requests start only after consent in the interface. IPWhois receives the public IP address to estimate the city. Open-Meteo and the fallback provider wttr.in receive an approximate location and can see request network metadata. GPS is not used. A VPN may change the detected city. Weather can be disabled.

Automatic navigation to the restart menu requires separately enabling “Помощник системного меню” (System menu helper) in Android Accessibility settings. Android grants this service access to screen content and menu control. The implementation acts only after the restart shortcut is pressed, only inside native settings, and for up to 20 seconds. It does not intercept remote keys or press the restart confirmation. Remote navigation commands are supported on Android 13+. Native menu labels and layout depend on the firmware.

AirPlay availability, VPN connectivity and playback in third-party players are separate TV or app functions, not provided by the launcher itself.

## Source and building

`app/src` contains the portable build’s source and JVM tests. Building requires JDK 17 and Android SDK 34. Commands and signing details: [BUILD.en.md](docs/BUILD.en.md).

Current build validation: 307 JVM tests passed, Android lint reported 0 errors and 99 warnings, and the release APK was built successfully. These are automated code checks, not testing on additional TV models.

## Upstream and licenses

Adapted from [Gathin23/cleanyourtv](https://github.com/Gathin23/cleanyourtv), base commit `095b71ad93d99ea28d94d97b06385709d354b795`.

- [MIT license and original notice](LICENSE)
- [Outfit font license](FONT-LICENSE-OFL.txt)
- [Upstream notices](NOTICE.md)

This project is not affiliated with Apple, Xiaomi or the developers of the third-party apps shown in the interface.
