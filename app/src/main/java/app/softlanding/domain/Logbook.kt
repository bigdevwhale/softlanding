package app.softlanding.domain

/** The aeronaut's evening line for each day, picked by the day so it never changes on a second look. */
object Logbook {
    private val WEATHER = listOf(
        "ветер с юга" to "wind from the south", "ясно" to "clear sky", "лёгкая дымка" to "a light haze",
        "кучевые облака" to "fair-weather clouds", "ветер с запада" to "wind from the west", "тихо" to "still air",
        "высокие перистые облака" to "high cirrus", "тёплый воздух снизу" to "warm air rising",
    )

    private val SOFT = listOf(
        "Сели у деревни %s. Мягко, на самую траву." to "Landed by %s. Softly, right on the grass.",
        "%s. Корзина коснулась луга так, что никто не проснулся." to "%s. The basket touched the meadow without waking anyone.",
        "Причалили у деревни %s. Нас встретили с хлебом." to "Moored by %s. They met us with bread.",
        "%s. Посадка ровная, как по линейке." to "%s. A landing as straight as a ruler.",
        "Луг у деревни %s. Сложили купол до темноты." to "The meadow by %s. Folded the envelope before dark.",
    )

    private val EARLY = listOf(
        "Груза было много, сели раньше, в поле у фермера. Он угостил молоком." to "Too much ballast; came down early in a farmer's field. He offered us milk.",
        "Сели в овсы, не долетев до деревни. Хозяйка поворчала и позвала ужинать." to "Came down in the oats short of the village. The farmer's wife grumbled and asked us to supper.",
        "Шар отяжелел к обеду. Ночуем у стога, деревня видна вдали." to "The balloon grew heavy by lunch. Spending the night by a haystack, the village in sight.",
        "Сели на пастбище. Коровы окружили корзину и смотрели до вечера." to "Came down in a pasture. The cows surrounded the basket and stared till evening.",
    )

    private val DRIFT = listOf(
        "Слишком легко: луг проплыл внизу, и нас унесло дальше. Сели у реки, голодные." to "Too light: the meadow slid past below and we drifted on. Landed by a river, hungry.",
        "Шар не хотел снижаться. Ночевали в роще, ужин был из сухарей." to "The balloon wouldn't come down. Spent the night in a copse; supper was rusks.",
        "Перелетели деревню. Сверху она была красивая, но ночевать в ней не пришлось." to "Flew past the village. It was lovely from above, but we didn't sleep there.",
    )

    private val FOG = listOf(
        "Туман. Записей нет." to "Fog. No records.",
        "Летели вслепую. Журнал пуст." to "Flew blind. The log is empty.",
    )

    private val FAIR = listOf(
        "Ярмарка! Сели прямо на площадь, норму сегодня не считали." to "A fair! Landed right in the square and nobody counted anything today.",
        "Праздник. Шар привязали к карусели." to "A holiday. The balloon was tied to the carousel.",
    )

    private val WIND = " Белков хватило — завтра попутный ветер." to " Enough protein: a tailwind tomorrow."

    fun line(f: Flight, village: String, ru: Boolean): String {
        // Walks each list by the day, so two days in a row never get the same line.
        var salt = 0
        fun pick(xs: List<Pair<String, String>>) = xs[Math.floorMod(f.day * 3 + salt++ * 5, xs.size.toLong()).toInt()].let { if (ru) it.first else it.second }
        val body = when (f.landing) {
            Landing.SOFT -> pick(SOFT).format(village)
            Landing.EARLY -> pick(EARLY)
            Landing.DRIFT -> pick(DRIFT)
            Landing.FOG -> pick(FOG)
            Landing.FAIR -> pick(FAIR)
        }
        if (f.landing == Landing.FOG) return body
        val wx = pick(WEATHER).replaceFirstChar { it.uppercase() }
        return "$body $wx." + if (f.wind) (if (ru) WIND.first else WIND.second) else ""
    }
}
