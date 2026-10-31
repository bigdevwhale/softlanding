package app.softlanding.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverter

const val MINUTE = 60_000L
const val HOUR = 60 * MINUTE
const val DAY = 24 * HOUR

/** Where a food came from: the built-in table, a barcode, a photographed label, typed by hand, a dish, or the online search. */
enum class Source { BUILTIN, SCAN, LABEL, USER, DISH, ONLINE }

enum class Meal { BREAKFAST, LUNCH, DINNER, SNACK }

/**
 * Anything that can be eaten, always counted per 100 g (or 100 ml).
 * A dish is a food too: its values come from its ingredients and the weight of the finished pot.
 */
@Entity(tableName = "foods", indices = [Index("barcode"), Index("key")])
data class Food(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** The built-in table's key, so updates of the table can find the row again. */
    val key: String? = null,
    /** The name; for built-in foods the Russian one, with [en] beside it. */
    val name: String,
    val en: String = "",
    val brand: String = "",
    val barcode: String? = null,
    val kcal: Double,
    val p: Double,
    val f: Double,
    val c: Double,
    /** Portions as `unit:grams` pairs; a unit is a [Measure] name or any custom word. */
    val portions: String = "",
    val ml: Boolean = false,
    val source: Source = Source.USER,
    val aliases: String = "",
    val group: String = "",
    val uses: Int = 0,
    val lastUsed: Long = 0,
    /** The amount eaten last time, offered first next time. */
    val lastGrams: Double = 0.0,
    val fav: Boolean = false,
    val hidden: Boolean = false,
    /** A dish: the weight of the whole finished pot, and how many servings it makes. */
    val cooked: Double = 0.0,
    val servings: Int = 0,
    /** A dish taken from a village's recipe. */
    val village: String? = null,
) {
    val isDish get() = source == Source.DISH
    fun title(ru: Boolean) = if (!ru && en.isNotBlank()) en else name
    fun per(grams: Double) = Nutr(kcal * grams / 100, p * grams / 100, f * grams / 100, c * grams / 100)
}

/** One line of a dish: so many grams of a food. */
@Entity(tableName = "ingredients", indices = [Index("dish")])
data class Ingredient(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dish: Long,
    val food: Long,
    val grams: Double,
)

/** Something eaten: a snapshot of the food at the time, so editing the food later never rewrites the past. */
@Entity(tableName = "entries", indices = [Index("day")])
data class Entry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** The flight day (epoch day, shifted by the hour the day starts). */
    val day: Long,
    val meal: Meal,
    val at: Long,
    val food: Long? = null,
    val name: String,
    val grams: Double,
    val kcal: Double,
    val p: Double = 0.0,
    val f: Double = 0.0,
    val c: Double = 0.0,
    /** How the amount was put, for the list: "2 pc", "1 plate". Empty means plain grams. */
    val how: String = "",
    val ml: Boolean = false,
) {
    val n get() = Nutr(kcal, p, f, c)
}

/** A plate or a bowl with its weight, taken off when food is weighed on it. */
@Entity(tableName = "plates")
data class Plate(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val grams: Double,
)

/** A morning on the scales. */
@Entity(tableName = "weighs")
data class Weigh(
    @PrimaryKey val day: Long,
    val kg: Double,
)

/** A fair day: the norm is off and the balloon lands at a fair. */
@Entity(tableName = "fairs")
data class Fair(
    @PrimaryKey val day: Long,
)

data class Nutr(val kcal: Double = 0.0, val p: Double = 0.0, val f: Double = 0.0, val c: Double = 0.0) {
    operator fun plus(o: Nutr) = Nutr(kcal + o.kcal, p + o.p, f + o.f, c + o.c)
    operator fun times(k: Double) = Nutr(kcal * k, p * k, f * k, c * k)
    companion object {
        val ZERO = Nutr()
        fun sum(xs: Iterable<Nutr>) = xs.fold(ZERO) { a, b -> a + b }
    }
}

/** The standard household measures. */
enum class Measure(val ru: String, val en: String) {
    PC("шт", "pc"), SLICE("ломтик", "slice"), GLASS("стакан", "glass"), CUP("чашка", "cup"),
    TBSP("ст. л.", "tbsp"), TSP("ч. л.", "tsp"), PLATE("тарелка", "plate"), BOWL("миска", "bowl"),
    HANDFUL("горсть", "handful"), PACK("упаковка", "pack");

    companion object {
        fun of(s: String) = entries.firstOrNull { it.name.equals(s, true) }
    }
}

data class Portion(val unit: String, val grams: Double) {
    fun label(ru: Boolean): String = when {
        unit.equals("serving", true) -> if (ru) "порция" else "serving"
        else -> Measure.of(unit)?.let { if (ru) it.ru else it.en } ?: unit
    }

    companion object {
        fun parse(s: String): List<Portion> = s.split(',').mapNotNull { p ->
            val i = p.lastIndexOf(':')
            if (i <= 0) null else p.substring(i + 1).trim().toDoubleOrNull()?.takeIf { it > 0 }?.let { Portion(p.substring(0, i).trim(), it) }
        }

        fun join(xs: List<Portion>) = xs.joinToString(",") { "${it.unit.replace(",", " ").replace(":", " ")}:${fmt(it.grams)}" }

        private fun fmt(d: Double) = if (d == Math.floor(d)) d.toLong().toString() else d.toString()
    }
}

class Converters {
    @TypeConverter fun source(s: Source): String = s.name
    @TypeConverter fun source(s: String): Source = runCatching { Source.valueOf(s) }.getOrDefault(Source.USER)
    @TypeConverter fun meal(m: Meal): String = m.name
    @TypeConverter fun meal(s: String): Meal = runCatching { Meal.valueOf(s) }.getOrDefault(Meal.SNACK)
}
