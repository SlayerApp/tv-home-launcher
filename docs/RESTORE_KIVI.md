# Вернуть штатное меню KIVI

[Русский](RESTORE_KIVI.md) | [English](RESTORE_KIVI.en.md)

Этот файл относится к проверенному **KIVI 4K Android TV, аппаратное имя bangbae, Android 11 / API 30**. Профиль не предназначен для Xiaomi или любых других телевизоров. Сам APK лаунчера не отключает системные приложения: очистка KIVI выполнялась отдельно с согласия владельца.

Штатный главный экран только отключён. Его файлы и данные сохранены. Для возврата меню не нужны сброс телевизора, root или перепрошивка. Приложение TV Home Launcher и его настройки останутся установленными.

## Подготовка

1. Скачайте [Restore-Kivi.ps1](../tools/Restore-Kivi.ps1) через **Raw → Download raw file**. Вместе с инструкцией файл доступен и в ZIP исходников репозитория.
2. Скачайте официальные [Android SDK Platform-Tools для Windows](https://developer.android.com/tools/releases/platform-tools) и распакуйте архив. Запомните папку с `adb.exe`.
3. Включите телевизор. Компьютер и телевизор должны быть в одной домашней сети. В настройках KIVI включите отладку по USB/ADB и узнайте текущий IP-адрес. Подтвердите на ТВ запрос доступа компьютеру. USB-кабель для соединения по сети не требуется.
4. Откройте PowerShell в папке со скачанным скриптом. Если Windows блокирует именно скачанный файл, после проверки его содержимого снимите блокировку командой `Unblock-File .\Restore-Kivi.ps1`.

## Вернуть только старый главный экран

Подставьте **свой** адрес ТВ и путь к `adb.exe`; значения ниже приведены как пример:

```powershell
.\Restore-Kivi.ps1 -Address '192.168.1.50:5555' -AdbPath 'C:\platform-tools\adb.exe'
```

Скрипт проверит модель и версию Android, включит `com.google.android.tvlauncher`, назначит его главным экраном и нажмёт Home. Остальные отключённые приложения останутся отключены.

## Вернуть все приложения из профиля очистки

```powershell
.\Restore-Kivi.ps1 -Address '192.168.1.50:5555' -AdbPath 'C:\platform-tools\adb.exe' -Scope All
```

Восстанавливаются исходные состояния 15 пакетов: штатный Home, IVI, RUTUBE, KinoPub, Downloader, YouTube Music, Play Games, Netflix, Prime Video, Google Play Movies, KIVI Media, Anoki ACR, рекомендации, партнёрская настройка Home и магазинная демонстрация. VK Видео, YouTube, Кинопоиск и Happ при очистке сохранялись. Два пакета, отключённые ещё до проверки, скрипт не меняет.

Для предварительного просмотра добавьте `-WhatIf`: команды включения и переход на старый Home выполняться не будут. Подключение к ТВ и проверка модели всё равно выполняются. Если ТВ недоступен или имеет другую модель/API, скрипт остановится до изменения пакетов.

## Снова использовать новый Home

Оставьте установленным TV Home Launcher. Для этого же проверенного KIVI можно снова обратимо отключить старый главный экран:

```powershell
& 'C:\platform-tools\adb.exe' -s '192.168.1.50:5555' shell am start -n com.gathin.tvhome/.MainActivity
& 'C:\platform-tools\adb.exe' -s '192.168.1.50:5555' shell pm disable-user --user 0 com.google.android.tvlauncher
& 'C:\platform-tools\adb.exe' -s '192.168.1.50:5555' shell cmd package set-home-activity --user 0 com.gathin.tvhome/.MainActivity
& 'C:\platform-tools\adb.exe' -s '192.168.1.50:5555' shell input keyevent KEYCODE_HOME
```

После завершения настройки отладку можно выключить в настройках ТВ. При следующем восстановлении её потребуется включить снова.

Результаты фактических проверок: [проверенные телевизоры](TESTED_TVS.md).
