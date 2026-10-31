# Soft Landing · Мягкая посадка

A calorie counter where every day is a hot-air balloon flight. In the morning the balloon is light and high; everything you eat is ballast. Land softly on the meadow by evening (your daily norm, give or take) and you reach the next village, which gives you a recipe. Eat too much and you come down early in a farmer's field; too little and the wind carries you past.

Счётчик калорий, где каждый день — полёт на воздушном шаре. Мягкая посадка на луг (норма ± коридор) ведёт в следующую деревню, а там дают рецепт.

## Fast to log
- **Barcodes and QR**: Google's code scanner (no camera permission), EAN/UPC and the «Честный знак» DataMatrix; unknown codes go to Open Food Facts and stay on the phone.
- **Label photo**: the nutrition table is read on the phone (ML Kit) and checked against itself (P×4 + F×9 + C×4 ≈ kcal).
- **One line**: «гречка 200, 2 яйца, капучино» → three entries in one tap.
- 249 everyday foods with household measures, «как вчера», usual foods for each meal, one-tap add with the last amount, undo.
- Dishes from ingredients with the weight of the finished pot and servings; plates and pots taken off the scales; share a dish as a QR code.
- Quick calories, eyeball portions (palm, fist, handful), a day that starts at 4 am, CSV export.

## The game
- 31 villages in three lands and Cloud Harbour, a recipe in each, laps through the seasons.
- Fair days (two a month) keep the streak; fog (no records) only pauses the soft run.
- Twelve envelope patterns in the hangar, stamps with a tailwind for enough protein, the aeronaut's logbook, the weight goal on the route.

## Build
`./gradlew assembleDebug` (installs as app.softlanding.dev); `-Pdemo` builds app.softlanding.demo with three weeks of flights;
`./gradlew testDebugUnitTest -Prender --tests '*RenderScreens*'` then `python tools/shots.py` for screenshots; `python tools/res.py` writes the strings.

Fonts: Prata, Manrope, Caveat (SIL OFL). MIT licence.
