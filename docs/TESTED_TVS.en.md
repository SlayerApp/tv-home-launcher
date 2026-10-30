# Tested TVs

[English](TESTED_TVS.en.md) | [Русский](TESTED_TVS.md)

Recorded on October 30, 2026.

## Xiaomi Mini LED 55 2025

- Model: MiTV-MOEU1; hardware name: lalaland.
- System: Android 14 / API 34, factory RU region.
- Device checks used the main 3.2 build with an external theme. The portable build additionally embeds its theme and Home tile.

| Check | Actual result |
| --- | --- |
| New Home and Home tile | Open the new launcher; returning from the TV source was checked |
| Original Home flicker | The user confirmed that flicker disappeared after the original launcher was reversibly disabled |
| Held volume and Home from screensaver | Confirmed by the user in the previous main 3.1 build; key interception remains disabled |
| Launch after reboot | Confirmed on main 3.1; an actual reboot was not performed after the 3.2 update |
| Bluetooth and remote | Native Remotes & Accessories opened and showed a connected Xiaomi RC |
| TV tile | Opens native LiveTV; one Home press returns to the launcher. Actual channel video was not assessed |
| Inputs | HDMI 1/2/3 shown without a preassigned Apple TV label |
| HDMI names | Saving, reloading after activity recreation and removing a label were checked; the temporary test label was removed |
| Weather | City detected automatically; actual current wttr.in data appeared and updated |
| Long city name | After the fix, the descender is not clipped; the main tile layout is preserved |
| Restart shortcut | Native System opened and Restart was focused; confirmation was not pressed |
| Screensaver and sleep | System settings preserved; screen timeout 300000 ms, sleep_timeout=-1 |

This firmware did not handle the standard Bluetooth action, so its native accessories SliceActivity was used. System menu navigation required a separate bounded helper. Other firmware may behave differently.

## Not yet tested

- The portable APK on a second TV or other Android TV / Google TV models.
- All native settings routes on Android 8–13 and different firmware.
- HDMI video, all TV channels and playback in different third-party players.
- Practical VPN and iPhone AirPlay operation as part of this release.

There are no confirmed tests on Samsung Tizen or LG webOS; the Android APK is not intended for those systems.

## Automated checks

Portable build: 307 JVM tests, 0 lint errors (99 warnings), successful APK assembly. Signature, ZIP integrity, embedded theme and license files were checked. These checks do not replace installation on a specific model.
