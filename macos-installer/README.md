# TV Home Installer для macOS

Приложение рассчитано на macOS 15 и новее, Apple Silicon и Intel. В сборку входят ADB 37.0.1, APK TV Home Launcher 3.6 и инструкция для USB. Для установки по сети Mac и Android TV должны находиться в одной локальной сети.

1. Распакуйте ZIP и откройте «TV Home Installer.app».
2. Включите отладку на Android TV, введите IP устройства и нажмите «Подключить». Для беспроводной отладки с кодом используйте «Сопряжение…».
3. Нажмите «Установить лаунчер». После проверки нового экрана можно назначить его кнопке Home через «Выключить штатный экран». Кнопка «Вернуть штатный экран» восстанавливает системный Home.

Для XGIMI Elfin доступна установка с USB. В установщике нажмите «APK для флешки», затем откройте APK в файловом менеджере проектора. Доступность ADB и смены Home зависит от прошивки. [Поддержка XGIMI Elfin](https://global.xgimi.com/pages/elfin-support)

## Сборка

На Mac с Xcode Command Line Tools, Python 3 и .NET SDK 10 выполните `bash macos-installer/script/build_and_run.sh --build-only` из корня репозитория. ZIP и его контрольная сумма появятся в `macos-installer/dist/`. Вариант `--verify` проверяет запуск окна и встроенные компоненты. Запуск без аргументов собирает и открывает приложение.

Сборка использует локальную подпись ad hoc. Для публичного распространения с автоматическим доверием Gatekeeper потребуется Developer ID и нотарификация Apple. [Требования Apple](https://developer.apple.com/documentation/security/notarizing-macos-software-before-distribution)
