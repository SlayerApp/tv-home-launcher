# TV Home Installer for Windows

[English](README.en.md) | [Русский](README.md)

Run `TV-Home-Installer.exe` on Windows 10 or 11. It embeds the same 3.6 APK used for USB installation on Android TV and Google TV with Android 8–14. The installer UI and the USB text guide are in Russian. The **Инструкция** button opens the guide, and **APK для флешки** saves the APK and guide to the selected folder. [Downloads](../README.md#download-and-install)

1. Put the PC and TV on the same network. Open **About** on the TV and press OK seven times on **Build** or **Android TV OS build**. Go back to **Developer options** and enable network or wireless debugging. Find the TV's IPv4 address in its connected network settings. The default ADB port is 5555. Enter `IP:port` when the TV displays a different connection port. [Android developer options](https://developer.android.com/studio/debug/dev-options)
2. For wireless debugging by pairing code, use **Сопряжение…**. Enter the address and code from the TV pairing dialog. After pairing, enter the connection address and port from the main wireless debugging screen.
3. Click **Установить лаунчер** to install. By default the tool opens and verifies the new Home before disabling the stock Home. If Android reports several possible stock Home apps, click **Подключить** and select one from the list.
4. To restore the stock Home, click **Включить старый**. When the stock Home is enabled, the same button reads **Выключить старый**.

For USB installation, open the APK using the TV's file manager. Afterwards, connect the TV in the Windows installer and click **Выключить старый**. This step requires authorized ADB. Stock Home control and network debugging depend on the TV firmware. [Android ADB documentation](https://developer.android.com/tools/adb#connect-to-a-device-over-wi-fi)

ADB lets the computer connect to the TV. ADB 37.0.1 is bundled in the EXE and extracted on first use. Installation works offline over the local network. [Bundled ADB components and licenses](vendor/README.md)

To build from a repository checkout, run `windows-installer/Build.ps1` in PowerShell. The script validates the APK from `downloads/` or `export/`, compiles the installer using the local .NET Framework compiler, and writes the EXE and TXT to `export/`.
