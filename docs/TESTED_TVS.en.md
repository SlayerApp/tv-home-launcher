# Tested TVs

[English](TESTED_TVS.en.md) | [Русский](TESTED_TVS.md)

Recorded on October 30, 2026.

## Xiaomi Mini LED 55 2025

- Model: MiTV-MOEU1; hardware name: lalaland.
- System: Android 14 / API 34, factory RU region.
- Core device checks used main 3.2 with an external theme. RU/EN switching, language persistence through an update and new screenshots were checked in main 3.3 and again in 3.4. The portable build additionally embeds its theme and Home tile.

| Check | Actual result |
| --- | --- |
| RU/EN language (3.3) | Settings, Home, weather, app visibility and HDMI dialogs displayed in English; the choice survived pm install-r; Russian restored through the UI |
| State after the 3.3 update | The same 21 disabled packages, our default Home, only SystemMenuService enabled, and preserved screensaver/sleep timers |
| Backgrounds (3.4) | All 7 themes opened; four time variants, Automatic and Off/On checked on the TV. Theme/enabled state survived an APK update |
| 4K files (3.4) | All 28 bundled JPEG files verified at 3840×2160. They are upscaled approved illustrations. This Xiaomi’s UI remains 1920×1080 because firmware config_maxUiWidth is 1920 |
| New settings languages (3.4) | English Background settings and Home checked, then Russian restored; After the Bell, On, Automatic selected |
| TV/Home movement (3.5) | Both utility tiles moved on the Xiaomi; menus offer no removal, saved order survived a launcher restart, and the previous layout was restored |
| New Home and Home tile | Open the new launcher; returning from the TV source was checked |
| Original Home flicker | The user confirmed that flicker disappeared after the original launcher was reversibly disabled |
| Held volume and Home from screensaver | Confirmed by the user in the previous main 3.1 build; key interception remains disabled |
| Launch after reboot | Confirmed on main 3.1; an actual reboot was not performed after the 3.2–3.4 updates |
| Bluetooth and remote | Native Remotes & Accessories opened and showed a connected Xiaomi RC |
| TV tile | Opens native LiveTV; one Home press returns to the launcher. Actual channel video was not assessed |
| Inputs | HDMI 1/2/3 shown without a preassigned Apple TV label |
| HDMI names | Saving, reloading after activity recreation and removing a label were checked; the temporary test label was removed |
| Weather | City detected automatically; actual current wttr.in data appeared and updated |
| Long city name | After the fix, the descender is not clipped; the main tile layout is preserved |
| Restart shortcut | Native System opened and Restart was focused; confirmation was not pressed |
| Screensaver and sleep | System settings preserved; screen timeout 300000 ms, sleep_timeout=-1 |

This firmware did not handle the standard Bluetooth action, so its native accessories SliceActivity was used. System menu navigation required a separate bounded helper. Other firmware may behave differently.

## KIVI 4K Android TV — portable 3.5

- Android reports KIVI 4K Android TV, device bangbae, Android 11 / API 30, build RTO8.260427.001. A separate commercial panel SKU was not determined.
- The actual portable 3.5-portable5 / code 17 APK was installed with data preserved. Android reports a 1920×1080 UI and density 320. All 28 bundled wallpaper files are 3840×2160.

| Check | Actual result |
| --- | --- |
| New Home | Firmware ignored a simple default assignment. After separately approved, reversible stock Home disabling, the system Home opens our launcher directly |
| Actual reboot | New Home appeared automatically; moved tiles, language/background selection and all 17 disabled packages persisted |
| TV and Home (3.5) | Both tiles actually moved; their menus offer only dock and movement actions, without removal. Original layout restored after the test |
| RU/EN | English Home and settings displayed on the TV; Russian restored through the interface |
| Backgrounds | All 7 theme previews, 4 periods, Automatic and Off/On checked; Halloween displayed on Home. Selection survived the APK update and reboot |
| Bluetooth/remote | Native Remotes & Accessories opened and showed KIVI RC60; pairings unchanged |
| Network | Native Wi-Fi/network page opened; connection unchanged |
| Inputs | HDMI 1/2/3/4 and composite input listed; actual HDMI video not assessed |
| TV tile | Native com.mediatek.wwtv.tvcenter opened with cable channel UI; Home returned to the launcher. Video/audio quality not assessed |
| Applications | YouTube, VK Video and Kinopoisk launched and returned via Home; actual streaming playback not assessed. Happ preserved, VPN not enabled |
| Weather | Actual weather data and city displayed on Home; EN/RU display checked |
| Physical KIVI remote | Owner confirmed direct Home without stock screen flicker and continuous volume changes while held |
| KIVI background processes | No running processes found for 14 packages in the cleanup list. Anoki ACR remained resident as SYSTEM PERSISTENT after disable-user and force-stop; full termination was not confirmed |
| Separate cleanup | 15 packages, including stock Home, disabled with owner consent; 2 were disabled beforehand. No application data erased; VK Video preserved |
| Recovery | Public Restore-Kivi.ps1 Home scope actually restored stock Home, then new Home was reinstated. All scope checked only with WhatIf; wrong-model guard checked on Xiaomi |
| Screensaver/services | Backdrop, sleep/screen timers and the original accessibility selection preserved. Key interception and System menu helper were not enabled on KIVI |

Full-screen inspection of all 28 backgrounds, actual VPN/AirPlay operation and exact restart navigation on Android 11 were not tested. Held volume and flicker-free Home were confirmed by the KIVI owner; Home from its screensaver has not been checked yet.

[Restore the original KIVI Home](RESTORE_KIVI.en.md).

## Update 3.6 — checked on 1 November 2026

The final portable 3.6-portable6 / code 18 APK was installed on two KIVI bangbae TVs with Android 11. The separate main 3.6 / code 17 build was installed on the Xiaomi above. All three retained their direct Home handler, disabled-package selection, screensaver, sleep timers and previously enabled services; application data was preserved and no new services or permissions were enabled.

The additional KIVI was used for these actual regression checks:

- TV returned from the dock to the grid without hiding it or moving Home; each utility tile then moved independently using arrows and OK.
- Empty dock positions and the selected TV/Home positions survived a real reboot. The owner's earlier layout was restored afterwards.
- Removing YouTube from Home unchecked its Settings entry; checking it again restored the tile in its saved position. No app was uninstalled.
- TV and Home menus have Remove from dock and Move, but no Remove from Home.
- The owner confirmed that the brief native TV-screen flash on physical Home disappeared after the launcher stopped the pending window transition. Key filtering and global animation settings were unchanged.

Held-volume behaviour on the additional KIVI was not separately confirmed. The previous Anoki ACR limitation and untested video/VPN/AirPlay cases below still apply.

## Not yet tested

- Android TV / Google TV models other than the Xiaomi and KIVI listed above.
- All native settings routes on Android 8–13 and different firmware.
- HDMI video, all TV channels and playback in different third-party players.
- Practical VPN and iPhone AirPlay operation as part of this release.

There are no confirmed tests on Samsung Tizen or LG webOS; the Android APK is not intended for those systems.

## Automated checks

Portable build: 325 JVM tests, 0 lint errors (104 warnings), successful APK assembly. Signature, ZIP integrity, embedded theme and license files were checked. These checks do not replace installation on a specific model.
