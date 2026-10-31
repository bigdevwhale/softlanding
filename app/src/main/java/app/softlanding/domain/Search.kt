package app.softlanding.domain

import app.softlanding.data.DAY
import app.softlanding.data.Entry
import app.softlanding.data.Food
import app.softlanding.data.Meal
import app.softlanding.data.Portion
import app.softlanding.data.Source
import kotlin.math.ln
import kotlin.math.max

object Search {
    fun norm(s: String) = s.lowercase().replace('ё', 'е').replace(Regex("[^\\p{L}\\p{N}%]+"), " ").trim()

    private fun words(f: Food) = norm(listOf(f.name, f.en, f.aliases.replace(',', ' '), f.brand).joinToString(" ")).split(' ').filter { it.isNotEmpty() }

    /** How well a food answers a query; 0 is no match. Every query word has to start some word of the food. */
    fun score(f: Food, q: String, now: Long): Double {
        val qs = norm(q).split(' ').filter { it.isNotEmpty() }
        if (qs.isEmpty()) return 0.0
        val ws = words(f)
        var s = 0.0
        for (t in qs) {
            val best = ws.maxOfOrNull { w ->
                when {
                    w == t -> 3.0
                    w.startsWith(t) -> 2.0 + t.length.toDouble() / w.length
                    t.length >= 4 && w.startsWith(t.dropLast(1)) -> 1.6 // «гречку» finds «гречка»
                    t.length >= 3 && w.contains(t) -> 1.0
                    else -> 0.0
                }
            } ?: 0.0
            if (best == 0.0) return 0.0
            s += best
        }
        val nq = norm(q)
        val head = norm(f.name).startsWith(nq) || norm(f.en).startsWith(nq)
        if (head) s += 2
        // «яйцо» is the egg, not the egg white.
        if (norm(f.name) == nq || norm(f.en) == nq || f.aliases.split(',').any { norm(it) == nq }) s += 3
        // Raw and dry forms only when asked for: people mostly log what's on the plate.
        val k = f.key.orEmpty()
        if ((k.endsWith("_d") || k.endsWith("_r")) && qs.none { it.startsWith("сух") || it.startsWith("сыр") || it == "dry" || it == "raw" }) s -= 1.2
        s += habit(f, now)
        // Short plain names first: «Яблоко» before «Яблочный пирог с корицей».
        s -= norm(f.name).split(' ').size * .1
        return s
    }

    /** What use makes of a food: how often, how lately, and a star. */
    fun habit(f: Food, now: Long): Double {
        var s = ln(1.0 + f.uses) * .8
        if (f.lastUsed > 0) s += max(0.0, 1.5 - (now - f.lastUsed).toDouble() / (14 * DAY))
        if (f.fav) s += 1.5
        if (f.source != Source.BUILTIN) s += .4
        return s
    }

    fun find(foods: List<Food>, q: String, now: Long, limit: Int = 40): List<Food> =
        foods.asSequence().filter { !it.hidden }.map { it to score(it, q, now) }.filter { it.second > 0 }
            .sortedByDescending { it.second }.take(limit).map { it.first }.toList()

    /** What this person usually eats at this meal, newest habits first. */
    fun usual(foods: Map<Long, Food>, entries: List<Entry>, meal: Meal, today: Long, limit: Int = 12): List<Food> {
        val w = HashMap<Long, Double>()
        for (e in entries) {
            val id = e.food ?: continue
            val age = today - e.day
            if (age < 0 || age > 60) continue
            val k = (if (e.meal == meal) 1.0 else .25) * (1.0 / (1 + age / 7.0))
            w[id] = (w[id] ?: 0.0) + k
        }
        val used = w.entries.sortedByDescending { it.value }.mapNotNull { foods[it.key] }.filter { !it.hidden }
        val favs = foods.values.filter { it.fav && !it.hidden && it !in used }
        return (used + favs).take(limit)
    }

    /** The meal that fits the clock. */
    fun mealAt(hour: Int): Meal = when (hour) {
        in 4..10 -> Meal.BREAKFAST
        in 11..15 -> Meal.LUNCH
        in 16..17 -> Meal.SNACK
        in 18..22 -> Meal.DINNER
        else -> Meal.SNACK
    }
}

/** One piece of a quick line: «гречка 200», «2 яйца», «кофе», «300 ккал». */
data class Bit(val text: String, val amount: Double?, val unit: Amount)

enum class Amount { GRAMS, PIECES, KCAL, PORTION, NONE }

/** One line typed fast, cut into pieces; each piece then finds its food. */
object QuickLine {
    private val SPLIT = Regex("\\s*(?:[,;\\n+]|\\s+и\\s+|\\s+and\\s+)\\s*")
    private val NUM = Regex("(\\d+(?:[.,]\\d+)?)\\s*(кг|kg|г|гр|грамм\\w*|g|gr|grams?|мл|ml|л|l|шт\\w*|pcs?|pieces?|ккал|kcal|кал|cal|порц\\w*|portions?|servings?|x|х)?(?=\\s|$)", RegexOption.IGNORE_CASE)

    fun looksLikeList(s: String) = SPLIT.containsMatchIn(s.trim()) || NUM.containsMatchIn(s)

    fun parse(s: String): List<Bit> = s.split(SPLIT).map { it.trim() }.filter { it.isNotEmpty() }.map { part ->
        val m = NUM.find(part)
        if (m == null) Bit(part, null, Amount.NONE)
        else {
            var n = m.groupValues[1].replace(',', '.').toDouble()
            val u = m.groupValues[2].lowercase()
            val unit = when {
                u.isEmpty() -> if (n <= 12 && n == Math.floor(n)) Amount.PIECES else Amount.GRAMS
                u in setOf("кг", "kg") -> { n *= 1000; Amount.GRAMS }
                u in setOf("л", "l") -> { n *= 1000; Amount.GRAMS }
                u.startsWith("ккал") || u == "kcal" || u == "кал" || u == "cal" -> Amount.KCAL
                u.startsWith("шт") || u.startsWith("pc") || u.startsWith("piece") || u == "x" || u == "х" -> Amount.PIECES
                u.startsWith("порц") || u.startsWith("portion") || u.startsWith("serving") -> Amount.PORTION
                else -> Amount.GRAMS
            }
            Bit(part.removeRange(m.range).replace(Regex("\\s+"), " ").trim(), n, unit)
        }
    }

    /** Grams for a piece once its food is known: pieces and portions use the food's first measure. */
    fun grams(b: Bit, f: Food): Double {
        val ps = Portion.parse(f.portions)
        val piece = ps.firstOrNull { it.unit == "PC" || it.unit == "pc" }?.grams ?: ps.firstOrNull()?.grams
        return when (b.unit) {
            Amount.GRAMS -> b.amount ?: 100.0
            Amount.PIECES -> (b.amount ?: 1.0) * (piece ?: 100.0)
            Amount.PORTION -> (b.amount ?: 1.0) * (if (f.isDish && f.servings > 0) f.cooked / f.servings else piece ?: 100.0)
            Amount.KCAL -> if (f.kcal > 0) (b.amount ?: 0.0) * 100 / f.kcal else 0.0
            Amount.NONE -> f.lastGrams.takeIf { it > 0 } ?: piece ?: 100.0
        }
    }
}
