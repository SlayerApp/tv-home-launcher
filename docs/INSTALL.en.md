# Installation on another Android TV

[English](INSTALL.en.md) | [Русский](INSTALL.ru.txt)

File: `TV-Home-Apple-Style.apk`.

Requirements: Android TV / Google TV running Android 8.0 or later. The app appears as “Главный экран” (Home).

1. Copy the APK to a USB drive and connect it to the TV.
2. Open the APK using a file manager. If Android prompts you, allow that file manager to install unknown apps, then select Install.
3. Open “Главный экран”. The four-column layout, input/VPN/network capsule, settings button and light settings menu are embedded. No separate theme file or Home shortcut APK is needed.
4. Select this app as the default Home app in the TV’s settings, if that option is available. Names and locations vary by firmware.
5. If the Home button still opens the original launcher, a model-specific setup may be needed. This APK does not disable system apps. Do not apply another TV’s cleanup or disable lists without checking the model.

## Language

Open **Settings → Language → Русский / English**. The choice updates Home, launcher settings and dialogs, and survives APK updates. Native TV settings keep the TV’s system language.

## Backgrounds

Open **Settings → Background**. Off restores the previous look. Seven themes include paradise beach, New Year, Halloween forest and After the Bell with Johnny beside Maria. All 28 images are 3840×2160 upscaled illustrations. Automatic follows the TV clock: morning 06–11, day 11–17, evening 17–22, night 22–06. Manual time selection is also available. Android determines UI resolution; the tested Xiaomi firmware limits menus to 1080p. The standby screensaver is preserved.

## Apps, inputs and VPN

kino.watch, YouTube, Kinopoisk, START, Apple TV, Happ and any AirPlay receiver must be installed and configured separately. Accounts, passwords and VPN subscriptions are not included. New TV apps appear automatically; visibility is controlled by “Show apps”. The VPN button opens Happ if installed.

The TV tile opens the built-in tuner. If no tuner is available, the launcher tries the available TV or input selection route. Home returns to the launcher configured as the default Home app.

“HDMI names” lets you label any physical HDMI port or remove its label. Default labels use HDMI numbers, without a preassigned Apple TV name. These names are saved in the launcher and do not overwrite the TV’s system labels.

## Weather

Weather appears in the top header without moving the app tiles. No API key or account is required by this build’s Open-Meteo and fallback wttr.in integration.

On first use, consent is requested for IPWhois to receive the public IP address to estimate the city, and for Open-Meteo and wttr.in to receive approximate location and request network metadata. GPS is not used. A VPN may change the detected city. The weather settings let you refresh the city or hide weather. If consent is declined, weather requests are not sent.

Sources: [Open-Meteo](https://open-meteo.com/), [wttr.in](https://wttr.in/), [IPWhois](https://ipwhois.io/).

## Bluetooth and restart

“Bluetooth and remote” opens the available native accessories menu.

For automatic navigation from “Restart” to the native System menu, enable “System menu helper” in Android Accessibility settings when prompted. Android grants access to screen content and menu control. After an explicit shortcut press, the helper navigates native settings for up to 20 seconds and focuses Restart. The user confirms the restart. It does not intercept remote keys. Permission stays enabled until the service is disabled manually. Without it, the shortcut offers ordinary system settings. Precise remote-command navigation is supported on Android 13+; other firmware layouts may differ.

## Test status

The portable APK has not been installed on a second TV yet. The interface and core functions were checked in the main build on a Xiaomi Mini LED 55 2025, MiTV-MOEU1, Android 14. See [tested models and limitations](TESTED_TVS.en.md).

Based on Gathin23/cleanyourtv under the MIT license. The original copyright notice and font license are included in the APK assets.
