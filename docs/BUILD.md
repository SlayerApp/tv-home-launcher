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

Параметры текущего переносного приложения: `com.gathin.tvhome`, versionCode 17, versionName `3.5-portable5`, minSdk 26, targetSdk 34. Нативных библиотек с привязкой к архитектуре процессора нет.

## Подпись и обновления

Текущая конфигурация использует локальный Android debug signing config. При сборке на другом компьютере подпись будет другой. Исходники не содержат приватный ключ готового APK.

Android допускает обновление установленного приложения только при совместимой подписи. Локально пересобранный APK с другим ключом не является обновлением опубликованного файла. Для следующих опубликованных выпусков требуется сохранять прежний ключ вне репозитория. Не добавляйте `.keystore`/`.jks`, пароли и локальные настройки в Git.

## Проверка модели

Изменяя системные переходы, проверяйте их на реальном ТВ и обновляйте `TESTED_TVS.md`. Факт сборки на API 26+ не доказывает работу Home, Bluetooth или перезагрузки на каждой прошивке.
