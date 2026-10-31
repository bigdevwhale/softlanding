package app.softlanding.domain

import app.softlanding.data.Entry
import app.softlanding.data.HOUR
import app.softlanding.data.Nutr
import app.softlanding.data.Weigh
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

object Days {
    /** The flight day a moment belongs to: the day begins at [start] o'clock, not at midnight. */
    fun of(t: Long, start: Int, zone: ZoneId = ZoneId.systemDefault()): Long =
        Instant.ofEpochMilli(t - start * HOUR).atZone(zone).toLocalDate().toEpochDay()

    fun date(d: Long): LocalDate = LocalDate.ofEpochDay(d)

    /** How far along the waking day the clock is, 7:00 → 0 and 22:00 → 1. */
    fun progress(t: Long, zone: ZoneId = ZoneId.systemDefault()): Float {
        val z = Instant.ofEpochMilli(t).atZone(zone)
        val h = z.hour + z.minute / 60f
        val hh = if (h < 4) h + 24 else h
        return ((hh - 7f) / 15f).coerceIn(0f, 1f)
    }

    fun hour(t: Long, zone: ZoneId = ZoneId.systemDefault()): Int = Instant.ofEpochMilli(t).atZone(zone).hour
}

/** How a day ended. */
enum class Landing {
    /** In the meadow: the norm, give or take. */
    SOFT,
    /** Too much ballast: down early, in a farmer's field. */
    EARLY,
    /** Too light: carried past the meadow. */
    DRIFT,
    /** Nothing written down: the flight went into fog. */
    FOG,
    /** A fair day: the norm is off. */
    FAIR,
}

data class Flight(val day: Long, val eaten: Nutr, val landing: Landing, val wind: Boolean, val norm: Norm, val count: Int,
                  /** The absolute index of the village landed at (soft) or last passed (otherwise); -1 is the launch field. */
                  val at: Int)

/** Everything the journey shows, replayed from the records every time; nothing about it is stored. */
data class Journey(
    val flights: List<Flight>,
    val soft: Int,
    /** Days in a row with something written down, counting today once it has a record. */
    val streak: Int,
    /** Soft landings (and fairs) in a row, up to yesterday. */
    val softStreak: Int,
    val bestSoftStreak: Int,
    val winds: Int,
) {
    /** The absolute index of the village the balloon now stands at; -1 is the launch field. */
    val at get() = soft - 1
    val next get() = soft
    val envelopes get() = Envelopes.unlocked(soft)
    val byDay: Map<Long, Flight> by lazy { flights.associateBy { it.day } }

    companion object {
        fun replay(
            entries: List<Entry>, fairs: Set<Long>, weighs: List<Weigh>, profile: Profile, manual: Int,
            started: Long, today: Long,
        ): Journey {
            val byDay = entries.groupBy { it.day }
            val first = minOf(started, byDay.keys.minOrNull() ?: started)
            val trend = Weights.trend(weighs)
            val flights = ArrayList<Flight>()
            var soft = 0
            var streakRun = 0
            var softRun = 0
            var best = 0
            var winds = 0
            var d = first
            while (d < today) {
                val es = byDay[d].orEmpty()
                val eaten = Nutr.sum(es.map { it.n })
                val w = Weights.at(trend, d) ?: profile.weight
                val norm = Norm.of(profile, manual, w)
                val landing = when {
                    d in fairs -> Landing.FAIR
                    es.isEmpty() -> Landing.FOG
                    norm.soft(eaten.kcal) -> Landing.SOFT
                    eaten.kcal > norm.hi -> Landing.EARLY
                    else -> Landing.DRIFT
                }
                val wind = landing == Landing.SOFT && Gauges.of(eaten, norm).wind
                if (landing == Landing.SOFT) soft++
                if (wind) winds++
                streakRun = if (landing == Landing.FOG) 0 else streakRun + 1
                softRun = when (landing) {
                    Landing.SOFT, Landing.FAIR -> softRun + 1
                    Landing.FOG -> softRun
                    else -> 0
                }
                best = maxOf(best, softRun)
                flights += Flight(d, eaten, landing, wind, norm, es.size, soft - 1)
                d++
            }
            val todayLogged = byDay[today].orEmpty().isNotEmpty()
            return Journey(flights, soft, streakRun + if (todayLogged) 1 else 0, softRun, best, winds)
        }
    }
}

/** Envelope patterns, opened by the count of soft landings. */
object Envelopes {
    val AT = listOf(0, 3, 7, 12, 18, 25, 31, 40, 50, 65, 80, 100)
    fun unlocked(soft: Int) = AT.count { it <= soft }
    fun nextAt(soft: Int) = AT.firstOrNull { it > soft }
}

object Weights {
    /** A smoothed line through the weigh-ins, so one salty dinner doesn't look like a kilo gained. */
    fun trend(ws: List<Weigh>): List<Pair<Long, Double>> {
        var t: Double? = null
        return ws.sortedBy { it.day }.map { w -> val v = t?.let { it + (w.kg - it) * .25 } ?: w.kg; t = v; w.day to v }
    }

    fun at(trend: List<Pair<Long, Double>>, day: Long): Double? = trend.lastOrNull { it.first <= day }?.second ?: trend.firstOrNull()?.second
}
