package app.softlanding.data

import android.content.Context
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/** The built-in table of everyday foods (assets/foods.tsv). */
object Builtin {
    fun read(ctx: Context): List<Food> = ctx.assets.open("foods.tsv").bufferedReader(Charsets.UTF_8).use { parse(it.readLines()) }

    fun parse(lines: List<String>): List<Food> = lines.filter { it.isNotBlank() && !it.startsWith("#") }.map { l ->
        val c = l.split('\t') + List(12) { "" }
        Food(
            key = c[0], group = c[1], name = c[2], en = c[3],
            kcal = c[4].toDouble(), p = c[5].toDouble(), f = c[6].toDouble(), c = c[7].toDouble(),
            portions = c[8], ml = c[9] == "1", aliases = listOf(c[10], c[11]).filter { it.isNotBlank() }.joinToString(","),
            source = Source.BUILTIN,
        )
    }
}

/** Codes off a pack: an EAN/UPC barcode, or the Russian «Честный знак» DataMatrix, which carries the GTIN after «01». */
object Codes {
    fun gtin(raw: String): String? {
        val s = raw.trim().replace("\u001d", "")
        if (s.all { it.isDigit() } && s.length in 8..14) return if (s.length == 14 && s.startsWith("0")) s.drop(1) else s
        val m = Regex("^\\(?01\\)?(\\d{14})").find(s) ?: return null
        val g = m.groupValues[1]
        return if (g.startsWith("0")) g.drop(1) else g
    }

    /** A dish passed from another phone: `softlanding:dish?...`. */
    fun isDish(raw: String) = raw.startsWith(SharedDish.PREFIX)
}

/** A dish as a QR code: name, values per 100 g, the finished weight and servings. */
object SharedDish {
    const val PREFIX = "softlanding:dish?"

    fun encode(f: Food): String {
        fun e(s: String) = URLEncoder.encode(s, "UTF-8")
        fun n(d: Double) = "%.1f".format(java.util.Locale.ROOT, d)
        return PREFIX + "n=${e(f.name)}&k=${n(f.kcal)}&p=${n(f.p)}&f=${n(f.f)}&c=${n(f.c)}&w=${n(f.cooked)}&s=${f.servings}"
    }

    fun decode(raw: String): Food? = runCatching {
        val q = raw.removePrefix(PREFIX).split('&').associate { it.substringBefore('=') to java.net.URLDecoder.decode(it.substringAfter('='), "UTF-8") }
        Food(
            name = q.getValue("n"), kcal = q.getValue("k").toDouble(), p = q.getValue("p").toDouble(), f = q.getValue("f").toDouble(),
            c = q.getValue("c").toDouble(), cooked = q["w"]?.toDoubleOrNull() ?: 0.0, servings = q["s"]?.toIntOrNull() ?: 0, source = Source.DISH,
        )
    }.getOrNull()
}

/** Open Food Facts: the open catalogue of packaged food. Only the barcode or the typed words are sent. */
object Off {
    private const val FIELDS = "code,product_name,product_name_ru,product_name_en,generic_name,brands,nutriments,serving_quantity,quantity"

    fun product(code: String, ru: Boolean): Food? {
        val j = get("https://world.openfoodfacts.org/api/v2/product/$code.json?fields=$FIELDS") ?: return null
        if (j.optInt("status") != 1) return null
        return food(j.optJSONObject("product") ?: return null, code, ru)
    }

    fun search(q: String, ru: Boolean): List<Food> {
        val u = "https://world.openfoodfacts.org/cgi/search.pl?search_terms=${URLEncoder.encode(q, "UTF-8")}&search_simple=1&action=process&json=1&page_size=24" +
            "&fields=$FIELDS" + if (ru) "&lc=ru&cc=ru" else ""
        val j = get(u) ?: return emptyList()
        val arr = j.optJSONArray("products") ?: return emptyList()
        return (0 until arr.length()).mapNotNull { i -> arr.optJSONObject(i)?.let { food(it, it.optString("code"), ru) } }
    }

    private fun food(p: JSONObject, code: String, ru: Boolean): Food? {
        val n = p.optJSONObject("nutriments") ?: return null
        fun v(k: String) = n.optDouble(k).takeIf { !it.isNaN() }
        val kcal = v("energy-kcal_100g") ?: v("energy_100g")?.let { it / 4.184 } ?: return null
        val name = listOf(if (ru) "product_name_ru" else "product_name_en", "product_name", "generic_name", "product_name_ru", "product_name_en")
            .map { p.optString(it).trim() }.firstOrNull { it.isNotEmpty() } ?: return null
        val serving = p.optDouble("serving_quantity").takeIf { !it.isNaN() && it > 0 }
        val pack = Regex("(\\d+(?:[.,]\\d+)?)\\s*(г|g|мл|ml)\\b", RegexOption.IGNORE_CASE).find(p.optString("quantity"))?.groupValues?.get(1)?.replace(',', '.')?.toDoubleOrNull()
        val ml = Regex("(мл|ml|л\\b|\\bl\\b)", RegexOption.IGNORE_CASE).containsMatchIn(p.optString("quantity"))
        val portions = listOfNotNull(serving?.let { Portion("serving", it) }, pack?.takeIf { it != serving }?.let { Portion("PACK", it) })
        return Food(
            name = name, brand = p.optString("brands").substringBefore(',').trim(), barcode = code.ifBlank { null },
            kcal = kcal, p = v("proteins_100g") ?: 0.0, f = v("fat_100g") ?: 0.0, c = v("carbohydrates_100g") ?: 0.0,
            portions = Portion.join(portions), ml = ml, source = Source.ONLINE,
        )
    }

    private fun get(u: String): JSONObject? {
        val c = URL(u).openConnection() as HttpURLConnection
        c.connectTimeout = 8000
        c.readTimeout = 12000
        c.setRequestProperty("User-Agent", "SoftLanding/1.0 (Android; https://github.com/bigdevwhale/softlanding)")
        return try {
            if (c.responseCode == 404) null else JSONObject(c.inputStream.bufferedReader().use { it.readText() })
        } finally {
            c.disconnect()
        }
    }
}
