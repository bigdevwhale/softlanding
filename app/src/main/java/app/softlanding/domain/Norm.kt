package app.softlanding.domain

import app.softlanding.data.Nutr
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

enum class Sex { F, M }

enum class Activity(val k: Double) { SITTING(1.2), LIGHT(1.375), ACTIVE(1.55), SPORT(1.725) }

/** Who is flying: enough to work out a daily norm. [pace] is kilograms a week towards [goal]. */
data class Profile(
    val sex: Sex = Sex.F,
    val age: Int = 30,
    val height: Int = 168,
    val weight: Double = 70.0,
    val goal: Double = 65.0,
    val activity: Activity = Activity.LIGHT,
    val pace: Double = 0.5,
) {
    /** -1 to lose, 0 to keep, 1 to gain. */
    val direction get() = when {
        abs(goal - weight) < 0.5 -> 0
        goal < weight -> -1
        else -> 1
    }
}

/** The day's norm and the meadow around it: land anywhere between [lo] and [hi] and it's a soft landing. */
data class Norm(val kcal: Int, val protein: Int, val fat: Int, val carbs: Int, val lo: Int, val hi: Int, val bmr: Int, val tdee: Int) {
    fun soft(eaten: Double) = eaten >= lo && eaten <= hi

    companion object {
        /** Mifflin–St Jeor, an activity factor and 7700 kcal per kilogram, never below a safe floor. */
        fun of(p: Profile, manual: Int = 0, weight: Double = p.weight): Norm {
            val bmr = 10 * weight + 6.25 * p.height - 5 * p.age + if (p.sex == Sex.M) 5 else -161
            val tdee = bmr * p.activity.k
            val shift = p.pace * 7700 / 7 * p.direction
            val floor = if (p.sex == Sex.M) 1500.0 else 1200.0
            val kcal = if (manual > 0) manual.toDouble() else max(floor, tdee + shift)
            return split(kcal.roundToInt(), weight, p.direction, bmr.roundToInt(), tdee.roundToInt())
        }

        fun split(kcal: Int, weight: Double, direction: Int, bmr: Int = 0, tdee: Int = 0): Norm {
            // Protein holds the muscle while losing: 1.6 g per kilo, a little less when keeping, capped for big bodies.
            val perKg = when (direction) { -1 -> 1.6; 1 -> 1.7; else -> 1.4 }
            val protein = minOf(weight * perKg, kcal * .35 / 4, 200.0)
            val fat = kcal * .3 / 9
            val carbs = max(0.0, (kcal - protein * 4 - fat * 9) / 4)
            val (l, h) = if (direction == 0) .92 to 1.08 else .9 to 1.05
            return Norm(kcal, protein.roundToInt(), fat.roundToInt(), carbs.roundToInt(), (kcal * l).roundToInt(), (kcal * h).roundToInt(), bmr, tdee)
        }
    }
}

/** How a day's macros sit against the norm: each gauge 0..1+ of its target. */
data class Gauges(val p: Double, val f: Double, val c: Double) {
    /** Enough protein and fat in reason: a tailwind. */
    val wind get() = p >= .9 && f <= 1.25

    companion object {
        fun of(n: Nutr, norm: Norm) = Gauges(n.p / max(1, norm.protein), n.f / max(1, norm.fat), n.c / max(1, norm.carbs))
    }
}
