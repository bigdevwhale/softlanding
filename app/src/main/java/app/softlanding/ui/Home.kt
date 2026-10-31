package app.softlanding.ui

import app.softlanding.data.Entry
import app.softlanding.data.Food
import app.softlanding.data.Ingredient
import app.softlanding.data.Meal
import app.softlanding.data.Nutr
import app.softlanding.data.Plate
import app.softlanding.data.Settings
import app.softlanding.data.Weigh
import app.softlanding.domain.Days
import app.softlanding.domain.Flight
import app.softlanding.domain.Journey
import app.softlanding.domain.Landing
import app.softlanding.domain.Norm
import app.softlanding.domain.Route
import app.softlanding.domain.Season
import app.softlanding.domain.Village
import app.softlanding.domain.Weights
import app.softlanding.ui.art.SceneState

/** Everything the screens read, worked out once per change. */
class Home(
    val s: Settings,
    val foods: List<Food>,
    val entries: List<Entry>,
    val weighs: List<Weigh>,
    val fairs: Set<Long>,
    val plates: List<Plate>,
    val ingredients: List<Ingredient>,
    val villages: List<Village>,
    val now: Long,
    val ru: Boolean,
) {
    val today = Days.of(now, s.dayStart)
    val byId: Map<Long, Food> = foods.associateBy { it.id }
    val byDay: Map<Long, List<Entry>> = entries.groupBy { it.day }
    val trend = Weights.trend(weighs)
    val journey = Journey.replay(entries, fairs, weighs, s.profile, s.manualKcal, s.started, today)

    fun norm(day: Long): Norm = Norm.of(s.profile, s.manualKcal, if (day >= today) s.profile.weight else Weights.at(trend, day) ?: s.profile.weight)
    val normToday = norm(today)

    fun day(d: Long) = byDay[d].orEmpty()
    fun meal(d: Long, m: Meal) = day(d).filter { it.meal == m }
    fun eaten(d: Long) = Nutr.sum(day(d).map { it.n })

    fun village(abs: Int): Village = villages[Math.floorMod(abs, villages.size)]
    fun lap(abs: Int) = if (abs < 0) 0 else abs / villages.size
    val season: Season get() = Route.season(lap(journey.next))
    val nextVillage get() = village(journey.next)

    /** A flight that's over, or null for today and the future. */
    fun flight(d: Long): Flight? = journey.byDay[d]

    fun scene(d: Long, hour: Int, progress: Float): SceneState {
        val f = flight(d)
        val n = norm(d)
        val e = eaten(d).kcal
        val alt = ((n.kcal - e) / n.kcal).toFloat()
        val season = Route.season(lap(f?.at ?: journey.next))
        return SceneState(alt, if (f != null) 1f else progress, f?.landing ?: if (d in fairs) Landing.FAIR else null, s.envelope, season,
            if (f != null) 19 else hour, ((n.kcal - n.lo).toFloat() / n.kcal))
    }

    /** Fair days left this month (two a month). */
    fun fairsLeft(day: Long): Int {
        val m = Days.date(day)
        return 2 - fairs.count { Days.date(it).let { d -> d.year == m.year && d.month == m.month } }
    }

    /** Flights that ended since the last look, for the landing ceremony. */
    val unseen: List<Flight> get() = journey.flights.filter { it.day > s.seen && it.day >= s.started && it.landing != Landing.FOG }

    val lastWeigh: Weigh? = weighs.maxByOrNull { it.day }
    val weighDue get() = lastWeigh == null || today - lastWeigh.day >= 7

    /** The ingredients of a dish with their foods. */
    fun parts(dish: Long): List<Pair<Food, Double>> = ingredients.filter { it.dish == dish }.mapNotNull { i -> byId[i.food]?.let { it to i.grams } }

    /** The dish already cooked from a village recipe, if any. */
    fun dishOf(v: Village): Food? = foods.firstOrNull { it.village == v.key && !it.hidden }

    fun reached(v: Village, lap: Int = 0): Boolean = journey.soft > lap * villages.size + v.index
}
