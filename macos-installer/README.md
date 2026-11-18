# TV Home Installer для macOS

Приложение рассчитано на macOS 15 и новее, Apple Silicon и Intel. В сборку 1.0.2 входят ADB 37.0.1, APK TV Home Launcher 3.7.1 и инструкция для USB. Для установки по сети Mac и Android TV должны находиться в одной локальной сети.

[Скачать приложение и SHA-256](https://github.com/SlayerApp/tv-home-launcher/releases/tag/macos-installer-v1.0.2)

1. Распакуйте ZIP и откройте «TV Home Installer.app».
2. Включите отладку на Android TV, введите IP устройства и нажмите «Подключить». Для беспроводной отладки с кодом используйте «Сопряжение…».
3. Нажмите «Установить лаунчер». После проверки нового экрана можно назначить его кнопке Home через «Выключить штатный экран». Кнопка «Вернуть штатный экран» восстанавливает системный Home.

С флажком «Добавить VPN» установщик при необходимости загружает официальный APK клиента для ARM. Первая загрузка требует интернета. Кнопка «APK для флешки» также может сохранить этот файл. Профиль импортируется на телевизоре через VPN. [Документация клиента для Android TV](https://www.happ.su/main/ru/faq/android-tv)

Работу лаунчера на XGIMI Elfin подтвердил владелец проектора. Для установки с USB в установщике нажмите «APK для флешки», затем откройте APK в файловом менеджере проектора. Доступность ADB и смены Home зависит от прошивки. [Поддержка XGIMI Elfin](https://global.xgimi.com/pages/elfin-support)

## Сборка

На Mac с Xcode Command Line Tools, Python 3 и .NET SDK 10 выполните `bash macos-installer/script/build_and_run.sh --build-only` из корня репозитория. ZIP и его контрольная сумма появятся в `macos-installer/dist/`. Вариант `--verify` проверяет запуск окна и встроенные компоненты. Запуск без аргументов собирает и открывает приложение.

Для сборки с клиентом внутри запустите `TVHOME_MAC_VPN_APK=/путь/Happ.apk bash macos-installer/script/build_and_run.sh --build-only` с официальным APK версии 4.10.1. Скрипт проверит SHA-256 и вложит APK в подписанное приложение. Архив `TV-Home-Installer-macOS-Offline.zip` появится в `macos-installer/dist/offline/`.

Сборка использует локальную подпись ad hoc. Для публичного распространения с автоматическим доверием Gatekeeper потребуется Developer ID и нотарификация Apple. [Требования Apple](https://developer.apple.com/documentation/security/notarizing-macos-software-before-distribution)

Если macOS блокирует скачанную сборку, проверьте её SHA-256. Для проверенного файла macOS позволяет подтвердить первый запуск в разделе «Системные настройки → Конфиденциальность и безопасность». [Инструкция Apple](https://support.apple.com/en-ie/102445)
