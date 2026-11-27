# Сборка

[Русский](BUILD.md) | [English](BUILD.en.md)

Требуются JDK 17, Android SDK Platform 34 и доступ к Google/Maven Central при первой загрузке зависимостей. Проект использует Android Gradle Plugin 8.5.2 и Gradle Wrapper.

Укажите установленный SDK в переменной `ANDROID_HOME` либо в своём `local.properties`. Этот локальный файл не добавляется в Git.

Windows:

```powershell
.\gradlew.bat testReleaseUnitTest lintRelease assembleRelease
```

macOS / Linux:

```sh
chmod +x gradlew
./gradlew testReleaseUnitTest lintRelease assembleRelease
```

Результат: `app/build/outputs/apk/release/app-release.apk`.

Параметры переносной сборки `com.gathin.tvhome` версии `3.7.1-portable8` включают versionCode 22, minSdk 26 и targetSdk 34.

## Подпись и обновления

Текущая конфигурация использует локальный Android debug signing config. При сборке на другом компьютере подпись будет другой. Исходники не содержат приватный ключ готового APK.

Android допускает обновление установленного приложения только при совместимой подписи. Локально пересобранный APK с другим ключом не является обновлением опубликованного файла. Для следующих опубликованных выпусков требуется сохранять прежний ключ вне репозитория. Не добавляйте `.keystore`/`.jks`, пароли и локальные настройки в Git.

## Публикация выпуска лаунчера

Для нового APK увеличьте код версии и задайте название версии. Выполните проверки сборки выше и подпишите APK прежним ключом. Замените `downloads/TV-Home-Apple-Style.apk` и `downloads/SHA256.txt`, сохранив имена файлов. Добавьте описание на английском и русском в `docs/releases/X.Y.Z.md` и закоммитьте файлы в `main`.

Отправьте в GitHub тег `launcher-vX.Y.Z` для этого коммита. Вместо `X.Y.Z` укажите версию APK до суффикса `-portable`, если он есть. GitHub проверит хеш, имя пакета, версию и подпись APK и приложит к выпуску APK и контрольную сумму. [Выпуски лаунчера](https://github.com/SlayerApp/tv-home-launcher/releases)

## Проверка модели

Изменяя системные переходы, проверяйте их на реальном ТВ и обновляйте `TESTED_TVS.md`. Факт сборки на API 26+ не доказывает работу Home, Bluetooth или перезагрузки на каждой прошивке.
