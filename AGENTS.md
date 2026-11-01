# TV Home Launcher

- Это публичные исходники переносного Android TV launcher, пакет `com.gathin.tvhome`. Java 17, minSdk26, compile/target34.
- Сначала читать этот файл; детали сборки в `docs/BUILD.md`, фактические испытания в `docs/TESTED_TVS.md`.
- Основной интерфейс: четыре плитки в нижнем dock, немного виден второй ряд; источники/VPN/сеть и отдельная шестерёнка сверху. Погода не меняет геометрию сетки.
- `Config` использует `assets/portable-theme.json` только при отсутствии внешней темы. Домик встроен; внешний companion не нужен.
- Не включать перехват клавиш, не менять заставку/таймеры и не отключать OEM-пакеты автоматически. Удержание громкости зависит от сохранения key_filter=false.
- Погода требует согласия до сетевых запросов; не встраивать аккаунты, подписки, адреса устройств и геоданные пользователей.
- SystemMenuService — только ограниченный переход внутри native Settings после явной кнопки. Никогда не нажимать Reboot/подтверждение и не перехватывать клавиши.
- Проверки: `gradlew testReleaseUnitTest lintRelease assembleRelease`. Не называть успешную компиляцию испытанием на реальном телевизоре.
- Не добавлять ключи подписи, local.properties, кэши, личные настройки, диагностические снимки и резервные копии устройств.
- Основу и лицензии Gathin23/cleanyourtv и Outfit сохранять. Не переносить сюда частный журнал настройки первого телевизора.
- Документация доступна на русском и английском: README.md/README.en.md, docs/BUILD.md/BUILD.en.md, TESTED_TVS.md/TESTED_TVS.en.md, INSTALL.ru.txt/INSTALL.en.md. При изменениях поведения обновлять обе версии и сохранять ссылки переключения языка.
- Языки интерфейса RU/EN выбираются в AppleSettingsActivity; LauncherLocale хранит выбор отдельно и даёт приоритет над темой. Native Settings остаются на языке системы. Общие кастомные строки держать в values/apple_strings.xml и values-ru/apple_strings.xml.
- Фоны реализованы в Backgrounds/SceneBackgroundView/AppleBackgroundsActivity:7тем×4времени суток,28JPEG3840×2160, defaultOFF, отдельныеprefs/revision. Декодировать только необходимый размер viewport; не менять геометрию UI, системное разрешение и заставку из приложения. На проверенном Xiaomi системный UI ограничен1080p прошивкой; называть4К именно файлы изображений, не доказанную работу native4КUI. docs/BACKGROUNDS.md и .en.md содержат фактические результаты.

- README.md — основная английская страница, README.ru.md — русский вариант, README.en.md сохраняет прежнюю английскую ссылку. Обновлять все три согласованно.
- В3.6 контекстный Move переносит одну выбранную плитку стрелками, OK сохраняет. Remove from dock возвращает плитку в grid, Remove from Home скрывает обычное приложение как чекбокс AppleApps. ТВ/домик обязательны: можно двигать/убирать из дока, скрывать нельзя. Явные пустые dock slots хранятся в Order, compact старые записи читаются. Не сворачивать vacancies при начале переноса. Main/portable сохраняют разные представления Home.
- Portable3.5 реально проверен на KIVI bangbae/Android11, main3.5 на Xiaomi/API34. Факты/ограничения в TESTED_TVS RUEN. tools/Restore-Kivi.ps1 — отдельный профиль восстановления только проверенного KIVI/API30 с обязательным адресом пользователя; не включать его в APK или автоматический setup.

- 3.6: 325 tests/0 lint errors/104 warnings; installed on two KIVI Android11 and Xiaomi Android14. Verified device cases and limits are in TESTED_TVS RU/EN. Preserve the signing identity, owner settings, key_filter=false and original screensaver.
