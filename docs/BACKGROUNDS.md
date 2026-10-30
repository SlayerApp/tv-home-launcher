# Темы фона

[Русский](BACKGROUNDS.md) | [English](BACKGROUNDS.en.md)

Реализовано в версии 3.4. Откройте **Настройки → Фон**. Вкл. показывает выбранную сцену; Выкл. возвращает прежнее оформление Home. Выбор сохраняется при обновлении APK. На новом телевизоре по умолчанию Выкл. Положение плиток и заставка ожидания сохраняются.

**Время суток:** Автоматически — по местным часам ТВ: утро 06–11, день 11–17, вечер 17–22, ночь 22–06. Можно выбрать фиксированное время. Сеть и геопозиция для фонов не нужны.

## Разрешение

Все 28 установленных фоновых файлов — JPEG 3840×2160. Это качественное увеличение согласованных сгенерированных иллюстраций; новая детализация native 4К не синтезировалась. Лаунчер загружает размер под доступный экран: на 1080p — уменьшенный, при доступном Android-окне 4К — полный. Раскладка от размера файла не меняется.

На проверенном MiTV-MOEU1 панель 3840×2160, но окно приложений 1920×1080. Прямое чтение ресурса прошивки показало `config_maxUiWidth=1920`; сброс и запрос 3840×2160 оставили 1080p. Настоящий 4К-интерфейс на этой прошивке включить не удалось. Реальный 4К-интерфейс на другом ТВ пока не испытан. Применение максимума в Android описано в [DisplayContent](https://android.googlesource.com/platform/frameworks/base/+/e0f778e6a0d7/services/core/java/com/android/server/wm/DisplayContent.java). Root и изменения прошивки не использовались.

## Реальные снимки ТВ

![Настройки фона](screenshots/backgrounds.png)

![Ночная тема на ТВ](screenshots/home-night.png)

![Райский пляж на ТВ](screenshots/home-beach.png)

## Семь сцен

Ниже дневные примеры из настоящих файлов 4К. У каждой темы также есть утро, вечер и ночь в `app/src/main/assets/backgrounds`.

### Лес и озеро

![Лес и озеро](../app/src/main/assets/backgrounds/lake_day.jpg)

### Горы и туман

![Горы и туман](../app/src/main/assets/backgrounds/mountains_day.jpg)

### Морской берег

![Морской берег](../app/src/main/assets/backgrounds/coast_day.jpg)

### Райский пляж

![Райский пляж](../app/src/main/assets/backgrounds/beach_day.jpg)

### Новый год

![Новый год](../app/src/main/assets/backgrounds/new_year_day.jpg)

### Хэллоуин в лесу

![Хэллоуин в лесу](../app/src/main/assets/backgrounds/halloween_day.jpg)

### После звонка — Джонни рядом с Марией

![После звонка — Джонни рядом с Марией](../app/src/main/assets/backgrounds/after_bell_day.jpg)

Образы героев игровой темы: [After the Bell](https://afterthebellgame.com/), [App Store](https://apps.apple.com/ru/app/id6822703005).

[Задания генерации](background-production-prompts.json).
