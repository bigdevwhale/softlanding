# Google Play — всё для ручной публикации

Тексты лежат в `listing/<язык>/*.txt` (копируйте как есть), графика — в `graphics/`.
Ниже — что вписать в каждое поле Play Console и в каком порядке.

## 0. Перед стартом

- **Нужен AAB, не APK.** Релизный workflow кладёт в GitHub Release ещё и `SoftLanding-v<версия>.aab`.
  Первая версия для Play — **v1.0.1**: AAB добавляется к уже вышедшему релизу повторным запуском
  Actions → Release → Run workflow → `1.0.1`, без нового релиза. Секреты `SIGNING_*` в репозитории заданы;
  без них сборка подписывается debug-ключом, а такой файл Play отклонит.
- **Языки.** AAB собирается без разбиения по языкам (`bundle.language.enableSplit = false`), иначе Play
  ставит только язык телефона и переключение языка в настройках приложения перестаёт работать.
  Не удаляйте этот блок.
- **Регистрация имени пакета.** Play Console при создании приложения пишет «You can't use this package
  name because it hasn't been registered». Порядок
  ([справка Google](https://support.google.com/googleplay/android-developer/answer/16761053)):
  1. Play Console → **Android developer verification** → **Package names** → **Register package name** → `app.softlanding`.
  2. **Select key** → ключ с SHA-256 `589b0e71720b20d1c8c56840bae13f39910267fb3390b29a770a90eb0386922e`
     (сертификат CN=Soft Landing, O=Stillcraft — им подписаны все релизы).
  3. **Upload APK** → скопировать сниппет, собрать релизный APK с файлом `assets/adi-registration.properties`,
     в котором этот сниппет, подписать тем же ключом и загрузить. Ключ есть только в секретах GitHub,
     так что APK собирает workflow **Package registration proof** (Actions → Run workflow → вставить сниппет),
     APK — в артефактах запуска.
  4. Дождаться письма о регистрации, потом создавать приложение.
- **Подпись (Play App Signing).** При первой загрузке выберите **«Использовать свой ключ»** и загрузите ключ
  из того же keystore через PEPK (инструкция в самом диалоге). Так `app.softlanding` остаётся за одним
  зарегистрированным ключом, а APK из GitHub и версия из Play обновляют друг друга.
  Если оставить «Google создаст ключ подписи», версии из Play и GitHub друг на друга не встанут.
- **versionCode** берётся из номера запуска workflow (`github.run_number`), он только растёт — Play это устраивает.
- **Новый личный аккаунт разработчика** (создан после ноября 2023): перед выходом в production нужен
  закрытый тест — минимум 12 тестировщиков, которые не выходят из теста 14 дней подряд.
- **Политика конфиденциальности** — `PRIVACY.md` в корне репозитория. После push ссылка:
  `https://github.com/bigdevwhale/softlanding/blob/main/PRIVACY.md`
  (или перенесите текст на сайт Stillcraft).

## 1. Создание приложения

| Поле | Значение |
|---|---|
| App name | `Soft Landing: Calorie Counter` |
| Default language | English (United States) – en-US |
| App or game | App |
| Free or paid | Free |
| Declarations | обе галочки (Developer Program Policies, US export laws) |

## 2. Store listing → Main store listing

| Поле | en-US | ru-RU (Add translations → Russian) |
|---|---|---|
| App name (≤30) | `listing/en-US/title.txt` | `listing/ru-RU/title.txt` |
| Short description (≤80) | `listing/en-US/short_description.txt` | `listing/ru-RU/short_description.txt` |
| Full description (≤4000) | `listing/en-US/full_description.txt` | `listing/ru-RU/full_description.txt` |
| App icon 512×512 | `graphics/icon-512.png` | та же |
| Feature graphic 1024×500 | `graphics/feature-graphic-en.png` | `graphics/feature-graphic-ru.png` |
| Phone screenshots (2–8) | `graphics/screenshots-en/1…8.png` | `graphics/screenshots-ru/1…8.png` |
| Tablet screenshots | можно не загружать | — |
| Video | пусто | — |

Скриншоты 1080×1920 собраны `tools/play.py` из рендеров README (`docs/screenshots/<язык>`):
Play не берёт картинки длиннее двух ширин, а рендеры 1080×2341, поэтому экран уходит за нижний край
под подписью. Иконка — логотип README без прозрачных скруглённых углов (Play скругляет сам).
Как пересобрать:

```bash
./gradlew testDebugUnitTest -Prender --tests '*RenderScreens*' && python tools/shots.py
python tools/play.py   # flight, add, amount, map, village, journal, dish, hangar
```

## 3. Store settings

| Поле | Значение |
|---|---|
| App category | Application → **Health & Fitness** |
| Tags (до 5) | Calorie counter, Diet & nutrition, Weight loss, Health & fitness, Recipes |
| Email address | *ваш публичный email* — обязательно, его увидят все |
| Website | `https://bigdevwhale.github.io/stillcraft/apps/softlanding/` |
| Phone | пусто |
| External marketing | можно оставить включённым |

**Теги** — из списка Play; если какого-то названия нет дословно, берите ближайшее (Food & drink вместо
Recipes, Nutrition вместо Diet & nutrition). Calorie counter и Diet & nutrition — суть приложения,
Weight loss — линия веса с целью, Recipes — рецепты деревень и блюда.

**Имя разработчика** (Settings → Developer account) — **Stillcraft**, одинаковое для всех приложений:
тогда Play сам покажет под карточкой «Другие приложения этого разработчика».

## 4. App content (Policy → App content)

**Privacy policy** — URL выше.

**App access** — *All functionality is available without any access restrictions.*

**Ads** — *No, my app does not contain ads.*

**Content rating** (анкета IARC)
- Email — ваш; категория — **All Other App Types**.
- **Алкоголь, табак, наркотики**: в справочнике продуктов есть пиво, вино и водка как строки с калориями.
  На вопрос о *references to alcohol* честнее ответить **Yes** (упоминание), на *use / encouragement* — **No**:
  употребление не показывается и не поощряется. На рейтинг это обычно не влияет; если анкета спрашивает
  только «depicts or encourages», ответ — **No**.
- Остальное — **No**: насилие, кровь, страх, сексуальный контент, ненормативная лексика, азартные игры,
  грубый юмор, цифровые покупки, браузер.
- **Users interact / share content** — **No**: сервера, чата и профилей нет; CSV уходит через системное
  «Поделиться», QR блюда только показывается на экране.
- **Shares location** — **No**.
- Ожидаемый результат: Everyone / PEGI 3 / 0+.

**Target audience and content**
- Target age: **18 and over**. Счётчик калорий с целью по весу — взрослая тема; Play внимательнее
  смотрит на приложения про вес, которые нацелены на подростков. Политика это и говорит:
  «meant for adults». Можно отметить и **16–17**, но лишних вопросов будет больше.
- *Could your store listing unintentionally appeal to children?* → **No**: воздушный шар и деревни
  нарисованы, но это дневник питания, без детских героев.

**News apps** — **No.**

**COVID-19 contact tracing and status apps** — My app is not a publicly available COVID-19 app.

**Data safety**

Разработчику не уходит ничего: нет серверов, аналитики, отчётов о сбоях. Дневник, вес, профиль и блюда —
в локальной базе. Но по определению Google «сбор» — любая передача с устройства, поэтому декларируем двое
получателей:

1. **Open Food Facts** — штрихкод неизвестной упаковки или слова поиска, по HTTPS, без идентификаторов;
   выключается в настройках («Онлайн-каталог»).
2. **ML Kit в сервисах Google Play** (сканер кодов и распознавание этикеток). Google в
   [ML Kit data disclosure](https://developers.google.com/ml-kit/android-data-disclosure) прямо пишет,
   что SDK отправляет диагностику (модель устройства, версию ОС, пакет и версию приложения, задержки,
   коды ошибок) и идентификатор установки — «for diagnostics and usage analytics», по HTTPS, третьим
   лицам не передаёт. В сканере включён `enableAutoZoom()`, для него Google добавляет id сессии сканирования
   и координаты рамки — это та же диагностика. Снимки и распознанный текст не отправляются.

Ответы:
- *Does your app collect or share any of the required user data types?* → **Yes**.
- *Is all of the user data collected by your app encrypted in transit?* → **Yes**.
- *Do you provide a way for users to request that their data is deleted?* → **No** (на нашем сервере
  ничего нет; всё остальное удаляется вместе с приложением).
- Data types:

| Тип | Collected | Shared | Ephemeral | Required / Optional | Purpose |
|---|---|---|---|---|---|
| **App activity → In-app search history** (штрихкоды и слова поиска в Open Food Facts) | Yes | No | **Yes** | **Optional** | App functionality |
| **App info and performance → Diagnostics** (ML Kit) | Yes | No | No | Required | Analytics |
| **Device or other IDs** (идентификатор установки ML Kit) | Yes | No | No | Required | Analytics |

- **Не отмечать:** Health and fitness (вес, питание), Personal info (возраст, пол, рост), Photos — с телефона
  не уходят. Системное резервное копирование Android делает система, а не приложение, его не декларируем.
- Итог в карточке: «Collects: App activity, App info and performance, Device or other IDs»,
  «No data shared with third parties», «Data is encrypted in transit».
- Почему Open Food Facts — «not shared»: запрос делает сам пользователь ради функции, которую он вызвал
  (найти продукт), — это в исключениях Google для «sharing». Так же задекларирована погода в Липовке.
- Вариант смелее — не декларировать ML Kit, раз это сервисы Google Play, а не встроенный SDK. Но Google
  в своей таблице для ML Kit не делает такого исключения, и ревью сверяет декларацию с SDK в сборке.
  Рекомендую таблицу выше.

**Government apps** — No.

**Financial features** — *My app doesn't provide any financial features.*

**Health apps** — отметьте **Nutrition and weight management** (дневник питания, норма калорий, вес).
Health Connect, медицинские функции, клинические решения и прочее — не отмечать. Если форма
спросит про медицинское изделие — **No**: это дневник питания, а не медицинский совет (так и написано
в конце описания и в политике).

**Advertising ID** — *No* (рекламы и SDK аналитики нет, разрешения `AD_ID` в сборке нет).

**Permissions** — особых деклараций не нужно. В релизной сборке: `INTERNET`, `POST_NOTIFICATIONS`,
а также `WAKE_LOCK`, `ACCESS_NETWORK_STATE`, `RECEIVE_BOOT_COMPLETED`, `FOREGROUND_SERVICE` от WorkManager
(без типа службы, так что формы Foreground service не будет) и служебное
`app.softlanding.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` от androidx.core. Камеры нет: штрихкоды читает
сканер Google, этикетки снимает приложение камеры через `IMAGE_CAPTURE`. Точных будильников нет.

## 5. Pricing & distribution

- Countries / regions: все доступные.
- Free, без встроенных покупок.

## 6. Релиз

1. Testing → **Internal testing** (или Closed testing, если аккаунт новый) → Create new release.
2. Загрузить `SoftLanding-v1.0.1.aab` из GitHub Release.
3. Release name: `1.0.1`. Release notes:

```
<en-US>
(listing/en-US/release_notes.txt)
</en-US>
<ru-RU>
(listing/ru-RU/release_notes.txt)
</ru-RU>
```

4. Review release → Start rollout. Первая проверка Google обычно занимает от нескольких дней до недели.
5. После установки из Play проверьте: Настройки → Язык → Русский/English — экран должен
   переключиться сразу; штрихкод сканируется (сканер и распознавание этикеток скачиваются сервисами
   Google Play вместе с приложением); фото этикетки читается; поиск находит продукт в Open Food Facts.

## Следующие версии

- `release_notes.txt` переписать под новую версию (для первой там «First release on Google Play»).
- Скриншоты пересобирать, только если поменялись показанные экраны.

## После публикации

Добавьте в README бейдж и ссылку `https://play.google.com/store/apps/details?id=app.softlanding`.
