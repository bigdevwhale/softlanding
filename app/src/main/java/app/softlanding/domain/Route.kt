package app.softlanding.domain

import android.content.Context

/** A village on the route, and the recipe its people give to whoever lands softly by it. */
data class Village(
    val index: Int,
    val key: String,
    val region: Int,
    val ru: String,
    val en: String,
    val lineRu: String,
    val lineEn: String,
    val recipeRu: String,
    val recipeEn: String,
    val servings: Int,
    val ingredients: List<Pair<String, Double>>,
    val stepsRu: List<String>,
    val stepsEn: List<String>,
    /** The weight of the finished dish, water and all. */
    val cooked: Double = 0.0,
) {
    fun name(ru: Boolean) = if (ru) this.ru else en
    fun line(ru: Boolean) = if (ru) lineRu else lineEn
    fun recipe(ru: Boolean) = if (ru) recipeRu else recipeEn
    fun steps(ru: Boolean) = if (ru) stepsRu else stepsEn
    val harbour get() = region == 3
}

object Route {
    @Volatile private var cache: List<Village>? = null

    fun villages(ctx: Context): List<Village> = cache ?: synchronized(this) {
        cache ?: ctx.assets.open("route.tsv").bufferedReader(Charsets.UTF_8).use { parse(it.readLines()) }.also { cache = it }
    }

    fun parse(lines: List<String>): List<Village> = lines.filter { it.isNotBlank() && !it.startsWith("#") }.mapIndexed { i, l ->
        val c = l.split('\t')
        Village(
            index = i, key = c[0], region = c[1].toInt(), ru = c[2], en = c[3], lineRu = c[4], lineEn = c[5],
            recipeRu = c[6], recipeEn = c[7], servings = c[8].toInt(),
            ingredients = c[9].split(',').map { kv -> kv.substringBefore(':') to kv.substringAfter(':').toDouble() },
            stepsRu = c[10].split('|'), stepsEn = c[11].split('|'), cooked = c.getOrNull(12)?.toDoubleOrNull() ?: 0.0,
        )
    }

    /** The four seasons the route is flown in, one per lap. */
    fun season(lap: Int) = Season.entries[lap % Season.entries.size]
}

enum class Season { SUMMER, AUTUMN, WINTER, SPRING }
