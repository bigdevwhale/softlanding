package app.softlanding.data

import android.content.Context
import app.softlanding.AppContainer
import app.softlanding.domain.Activity
import app.softlanding.domain.Days
import app.softlanding.domain.Norm
import app.softlanding.domain.Profile
import app.softlanding.domain.Route
import app.softlanding.domain.Sex
import kotlin.random.Random

/** Three weeks of flights for the demo build and the screenshots: mostly soft, a few rough, one fog and one fair. */
object DemoSeed {
    private val MENUS = listOf(
        listOf(Meal.BREAKFAST to ("oatmeal_m" to 250.0), Meal.BREAKFAST to ("cappuccino" to 300.0), Meal.LUNCH to ("borscht" to 300.0), Meal.LUNCH to ("bread_rye" to 30.0),
            Meal.LUNCH to ("chicken_breast" to 120.0), Meal.SNACK to ("apple" to 180.0), Meal.DINNER to ("buckwheat_c" to 180.0), Meal.DINNER to ("salmon" to 120.0), Meal.DINNER to ("veg_salad" to 150.0)),
        listOf(Meal.BREAKFAST to ("syrniki" to 180.0), Meal.BREAKFAST to ("sour_cream_15" to 20.0), Meal.BREAKFAST to ("coffee_black" to 200.0), Meal.LUNCH to ("chicken_soup" to 300.0),
            Meal.LUNCH to ("rice_c" to 150.0), Meal.LUNCH to ("cutlet_chicken" to 80.0), Meal.SNACK to ("kefir_1" to 250.0), Meal.DINNER to ("omelette" to 150.0), Meal.DINNER to ("tomato" to 120.0)),
        listOf(Meal.BREAKFAST to ("egg_boiled" to 100.0), Meal.BREAKFAST to ("bread_wg" to 35.0), Meal.BREAKFAST to ("cheese_hard" to 15.0), Meal.BREAKFAST to ("tea" to 250.0),
            Meal.LUNCH to ("plov" to 280.0), Meal.LUNCH to ("cucumber" to 120.0), Meal.SNACK to ("yogurt_greek" to 140.0), Meal.SNACK to ("blueberry" to 70.0),
            Meal.DINNER to ("cod" to 150.0), Meal.DINNER to ("potato_baked" to 150.0), Meal.DINNER to ("broccoli" to 150.0)),
        listOf(Meal.BREAKFAST to ("tvorog_5" to 180.0), Meal.BREAKFAST to ("banana" to 120.0), Meal.BREAKFAST to ("latte" to 350.0), Meal.LUNCH to ("pelmeni" to 220.0),
            Meal.LUNCH to ("sour_cream_15" to 20.0), Meal.SNACK to ("almonds" to 25.0), Meal.DINNER to ("chicken_breast_g" to 150.0), Meal.DINNER to ("greek_salad" to 200.0)),
    )

    suspend fun seedIfEmpty(ctx: Context, c: AppContainer) {
        if (c.entries.count() > 0) return
        val now = System.currentTimeMillis()
        val today = Days.of(now, 4)
        val start = today - 21
        val profile = Profile(Sex.F, 32, 168, 70.6, 64.0, Activity.LIGHT, .5)
        c.settings.update { it.copy(onboarded = true, started = start, profile = profile, seen = today - 1, envelope = 2, remind = false) }
        val byKey = c.foods.list().filter { it.key != null }.associateBy { it.key!! }
        val r = Random(42)
        // The day's fate: S soft, E early (too much), D drift (too little), F fog, A fair.
        val plan = "SSESSSDSSSFSSSASSESSS"
        for ((i, ch) in plan.withIndex()) {
            val day = start + i
            if (ch == 'F') continue
            if (ch == 'A') c.misc.fair(Fair(day))
            val w = 72.0 - i * .07
            val norm = Norm.of(profile.copy(weight = w))
            val target = when (ch) {
                'S' -> norm.kcal * (.94 + r.nextDouble() * .08)
                'E' -> norm.kcal * 1.22
                'D' -> norm.kcal * .74
                else -> norm.kcal * 1.45
            }
            write(c, byKey, MENUS[i % MENUS.size], day, target, ru(ctx))
        }
        // Today: breakfast and lunch so far.
        val norm = Norm.of(profile)
        write(c, byKey, MENUS[0].filter { it.first == Meal.BREAKFAST || it.first == Meal.LUNCH }, today, norm.kcal * .55, ru(ctx))
        for ((i, kg) in listOf(72.1, 71.8, 71.9, 71.4, 71.2, 71.0, 70.9, 70.6).withIndex()) c.misc.weigh(Weigh(start + i * 3, kg))
        c.misc.addPlate(Plate(name = if (ru(ctx)) "Белая тарелка" else "White plate", grams = 340.0))
        c.misc.addPlate(Plate(name = if (ru(ctx)) "Миска" else "Bowl", grams = 280.0))
        c.misc.addPlate(Plate(name = if (ru(ctx)) "Кастрюля" else "Pot", grams = 1150.0))
        for (v in Route.villages(ctx).take(4)) c.cook(v, ru(ctx))
        byKey["yogurt_greek"]?.let { c.foods.update(it.copy(fav = true)) }
        byKey["cappuccino"]?.let { c.foods.update(it.copy(fav = true)) }
    }

    private fun ru(ctx: Context) = ctx.resources.configuration.locales[0].language == "ru"

    private suspend fun write(c: AppContainer, byKey: Map<String, Food>, menu: List<Pair<Meal, Pair<String, Double>>>, day: Long, target: Double, ru: Boolean) {
        val items = menu.mapNotNull { (m, kg) -> byKey[kg.first]?.let { Triple(m, it, kg.second) } }
        val total = items.sumOf { it.second.per(it.third).kcal }
        val k = target / total
        var t = (day + 2) * DAY + 8 * HOUR
        for ((m, f, g) in items) {
            val grams = Math.round(g * k / 5.0) * 5.0
            val n = f.per(grams)
            c.entries.insert(Entry(day = day, meal = m, at = t, food = f.id, name = f.title(ru), grams = grams, kcal = n.kcal, p = n.p, f = n.f, c = n.c, ml = f.ml))
            c.foods.used(f.id, t, grams)
            t += 40 * MINUTE
        }
    }
}
