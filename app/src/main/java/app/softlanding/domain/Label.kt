package app.softlanding.domain

import kotlin.math.abs

/** What a photographed nutrition table seems to say, per 100 g. Anything missing stays null for the person to fill. */
data class LabelGuess(val kcal: Double?, val p: Double?, val f: Double?, val c: Double?) {
    val any get() = listOf(kcal, p, f, c).any { it != null }
}

/**
 * Reads a nutrition table from recognised text. The recogniser reads Latin letters, so Russian words come out mangled
 * («белки» may read as «6enkn»): the keywords match both alphabets and the usual confusions, and when words fail,
 * the numbers are checked against each other: protein×4 + fat×9 + carbs×4 has to come close to the calories.
 */
object Label {
    private val NUM = Regex("(\\d{1,4}(?:[.,]\\d{1,2})?)")
    private val KCAL = Regex("(kcal|[kк][kк][aа][lлnjі1]|ккал|kкал)")
    private val KJ = Regex("(kj|кдж|[kк][dд][jжж])")
    private val PROT = Regex("(protein|prot[eé]|[б6b][еe][лn][кk]|белок|бeлк)")
    private val FAT = Regex("(\\bfat|fett|grasa|[жx][иuin][рpr]|жиры)")
    private val CARB = Regex("(carbohydrate|carb|glucid|[уy][гr][лn][еe][вb]|углев)")
    private val OF_WHICH = Regex("^\\s*(of which|из них|в т\\.?\\s*ч|incl|including|sugars|сахар|saturates|насыщ)")

    fun read(text: String): LabelGuess {
        val t = text.lowercase().replace('\n', ' ').replace(Regex("(\\d),(\\d)"), "$1.$2").replace(Regex("\\s+"), " ")
        val kcal = kcal(t)
        val p = after(t, PROT)
        val f = after(t, FAT)
        val c = after(t, CARB)
        val byWords = LabelGuess(kcal, p, f, c)
        if (kcal != null && p != null && f != null && c != null && fits(p, f, c, kcal)) return byWords
        // Words didn't settle it: look for three numbers in a row that add up to the calories (Russian tables go Б, Ж, У).
        if (kcal != null) triple(t, kcal)?.let { (a, b, cc) -> return LabelGuess(kcal, a, b, cc) }
        if (kcal == null && p != null && f != null && c != null) return byWords.copy(kcal = p * 4 + f * 9 + c * 4)
        return byWords
    }

    fun fits(p: Double, f: Double, c: Double, kcal: Double): Boolean {
        val est = p * 4 + f * 9 + c * 4
        return abs(est - kcal) <= maxOf(12.0, kcal * .15)
    }

    private fun kcal(t: String): Double? {
        for (m in KCAL.findAll(t)) {
            // «250 кДж / 60 ккал» or «60 ккал»: the number right before; «ккал 60» or «энергия, ккал: 60»: right after.
            val before = NUM.findAll(t.substring(maxOf(0, m.range.first - 12), m.range.first)).lastOrNull()?.groupValues?.get(1)?.toDoubleOrNull()
            if (before != null && before in 1.0..950.0) return before
            val tail = t.substring(m.range.last + 1, minOf(t.length, m.range.last + 18))
            val after = NUM.find(tail)?.groupValues?.get(1)?.toDoubleOrNull()
            if (after != null && after in 1.0..950.0) return after
        }
        // Only kilojoules found: convert.
        for (m in KJ.findAll(t)) {
            val before = NUM.findAll(t.substring(maxOf(0, m.range.first - 12), m.range.first)).lastOrNull()?.groupValues?.get(1)?.toDoubleOrNull()
            if (before != null && before in 4.0..4000.0) return before / 4.184
        }
        return null
    }

    private fun after(t: String, word: Regex): Double? {
        for (m in word.findAll(t)) {
            val tail = t.substring(m.range.last + 1, minOf(t.length, m.range.last + 26))
            if (OF_WHICH.containsMatchIn(tail)) continue
            val n = NUM.find(tail)?.groupValues?.get(1)?.toDoubleOrNull() ?: continue
            if (n <= 100) return n
        }
        return null
    }

    private fun triple(t: String, kcal: Double): Triple<Double, Double, Double>? {
        val ns = NUM.findAll(t).mapNotNull { it.groupValues[1].toDoubleOrNull() }.filter { it <= 100 }.toList()
        for (i in 0..ns.size - 3) {
            val (a, b, c) = Triple(ns[i], ns[i + 1], ns[i + 2])
            if (a + b + c in 1.0..100.0 && fits(a, b, c, kcal)) return Triple(a, b, c)
        }
        return null
    }
}
