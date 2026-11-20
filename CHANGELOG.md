# Changelog

<!--
Telegram: end each version's section with a hidden block for the StillCraft chat. Start it with a
line "<!-- tg", then 2-4 short lines in Russian for users (what is new, what is fixed), no markdown,
then close it like any HTML comment. The release notes carry it, and the chat bot posts those lines.
-->

## [1.1.0] - 2026-11-20

### Added
- **Fiber (#2):** a line of its own under protein, fat and carbs, aiming at 14 g per 1000 kcal (20 g at least). The built-in table knows the fiber of all its 249 foods, the online search brings it in, dishes add it up, and a food's editor has a field for it. Days written before keep no fiber; the count starts with new records.
- **A home-screen widget (#4):** today's calories with a bar, protein, fat, carbs and fiber; a tap opens the app, and it follows every record.
- **What made it up (#1):** a tap on protein, fat, carbs or fiber lists the day's foods with their grams and share, the biggest first.
- **A past day's recipe (#7):** a day landed softly shows the recipe of that morning's village, ready to take, even if the morning card was closed.

### Changed
- **A village's dish keeps its steps (#6):** «How to cook» is there in the dish, not only what goes in, and «A recipe from …» opens the village on the route (#8).

### Fixed
- **The weight line (#5)** no longer crawls up from an old value after days without weighing: it catches up with the scales. The line under the chart tells the last real weigh-in and the change from the start.

<!-- tg
Новое: клетчатка с нормой на день и виджет КБЖУ для рабочего стола
Нажми на белки, жиры или углеводы – покажу, какими продуктами они набраны
Рецепт деревни теперь можно найти в прошлом дне, а в блюде сохраняется «Как готовить» и ссылка на деревню
Исправлено: линия веса больше не уходит вверх, когда вес снижается
-->

## [1.0.2] - 2026-11-17

### Fixed
- **The weight goal on the route tells the truth:** «now» is your last weigh-in, not the smoothed line, and the starting weight can be set in the profile in settings; left empty, it is the first weigh-in as before (#3).

<!-- tg
В карточке цели на «Маршруте» текущий вес теперь равен последнему взвешиванию, а не сглаженному значению.
Вес на старте можно задать самому в настройках профиля.
-->

## [1.0.1] - 2026-10-31

### Changed
- Label reading now comes from Google Play services, like the barcode scanner: the APK is 5.5 MB instead of 48 MB.
- Plainer Russian on the welcome screen.

<!-- tg skip -->

## [1.0.0] - 2026-10-31

### Added
- The day as a balloon flight: what's left of the norm is the height. Land in the norm corridor for a soft landing, or come down early, drift past, fly into fog, or tie up at a fair (two a month).
- A route of 31 villages through three lands to Cloud Harbour, each with a recipe that becomes your own dish in one tap; laps through the seasons.
- Barcode, QR and DataMatrix scanning with Google's scanner; Open Food Facts for unknown packs.
- Label photos read on the phone and checked against the calories.
- One-line entries ("buckwheat 200, 2 eggs, cappuccino"), "same as yesterday", usual foods per meal, one-tap add and undo.
- 249 everyday foods with household measures; your own foods; dishes with the finished-pot weight, servings, scanned ingredients and QR sharing.
- Plates and pots taken off the scales, eyeball measures, quick calories.
- The hangar with twelve envelopes, the logbook with a month of landings and the aeronaut's notes, the weight line and goal.
- Mifflin–St Jeor norm with activity and pace, or your own number; a day that starts at 4 am; an evening reminder; CSV export.
- English and Russian.

<!-- tg
Новое приложение — «Мягкая посадка»: счётчик калорий, где каждый день — полёт на воздушном шаре
Попал в свою норму — мягкая посадка в следующей деревне, и там дают рецепт
Записывать быстро: штрихкод, фото этикетки или одной строкой «гречка 200, 2 яйца»
-->
