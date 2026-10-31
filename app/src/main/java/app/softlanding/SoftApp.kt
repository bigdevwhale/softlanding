package app.softlanding

import android.app.Application
import android.content.Context
import app.softlanding.data.DemoSeed
import app.softlanding.data.Builtin
import app.softlanding.data.Entry
import app.softlanding.data.Fair
import app.softlanding.data.Food
import app.softlanding.data.Ingredient
import app.softlanding.data.LandingDb
import app.softlanding.data.Meal
import app.softlanding.data.Nutr
import app.softlanding.data.Off
import app.softlanding.data.Portion
import app.softlanding.data.SettingsStore
import app.softlanding.data.Source
import app.softlanding.data.Weigh
import app.softlanding.domain.Days
import app.softlanding.domain.Village
import app.softlanding.work.Reminder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Something just went into the basket: the flight screen drops the balloon and shows the line. */
data class Loaded(val names: List<String>, val kcal: Double, val ids: List<Long>, val stamp: Long = System.nanoTime())

class AppContainer(context: Context) {
    val app: Context = context.applicationContext
    val db = LandingDb.create(app)
    val foods = db.foods()
    val entries = db.entries()
    val misc = db.misc()
    val settings = SettingsStore(app)
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** True while the table of foods (and the demo) goes into the database, so the first screen doesn't jump. */
    val seeding = MutableStateFlow(true)

    val loaded = MutableStateFlow<Loaded?>(null)

    fun write(block: suspend AppContainer.() -> Unit) {
        scope.launch { block() }
    }

    suspend fun today(now: Long = System.currentTimeMillis()) = Days.of(now, settings.now().dayStart)

    /** Puts the built-in foods in, and adds any the table gained since the last version. */
    suspend fun seedFoods() {
        val table = Builtin.read(app)
        if (foods.builtinCount() >= table.size) return
        val have = foods.list().mapNotNull { it.key }.toSet()
        foods.insertAll(table.filter { it.key !in have })
    }

    /** Something eaten: a snapshot goes into the day, and the food remembers it was used. */
    suspend fun eat(food: Food, grams: Double, meal: Meal, day: Long, how: String = "", announce: Boolean = true): Long {
        val n = food.per(grams)
        val now = System.currentTimeMillis()
        val title = food.title(app.ru)
        val id = entries.insert(Entry(day = day, meal = meal, at = now, food = food.id.takeIf { it > 0 }, name = title,
            grams = grams, kcal = n.kcal, p = n.p, f = n.f, c = n.c, how = how, ml = food.ml))
        if (food.id > 0) foods.used(food.id, now, grams)
        if (announce) loaded.value = Loaded(listOf(title), n.kcal, listOf(id))
        return id
    }

    /** Several foods at once, from a quick line or a copied meal. */
    suspend fun eatAll(xs: List<Triple<Food, Double, String>>, meal: Meal, day: Long) {
        val ids = xs.map { (f, g, how) -> eat(f, g, meal, day, how, announce = false) }
        loaded.value = Loaded(xs.map { it.first.title(app.ru) }, xs.sumOf { it.first.per(it.second).kcal }, ids)
    }

    /** Just a number of calories, for when there's no time. */
    suspend fun quick(kcal: Double, name: String, meal: Meal, day: Long, n: Nutr? = null) {
        val id = entries.insert(Entry(day = day, meal = meal, at = System.currentTimeMillis(), name = name, grams = 0.0, kcal = kcal,
            p = n?.p ?: 0.0, f = n?.f ?: 0.0, c = n?.c ?: 0.0))
        loaded.value = Loaded(listOf(name), kcal, listOf(id))
    }

    suspend fun copy(es: List<Entry>, meal: Meal, day: Long) {
        val now = System.currentTimeMillis()
        val ids = es.mapIndexed { i, e -> entries.insert(e.copy(id = 0, day = day, meal = meal, at = now + i)) }
        loaded.value = Loaded(es.map { it.name }, es.sumOf { it.kcal }, ids)
    }

    /** Changes the amount of a logged entry, keeping its proportions. */
    suspend fun regram(e: Entry, grams: Double, meal: Meal = e.meal, how: String = "") {
        val k = if (e.grams > 0) grams / e.grams else 1.0
        entries.update(e.copy(grams = grams, kcal = e.kcal * k, p = e.p * k, f = e.f * k, c = e.c * k, meal = meal, how = how))
    }

    /** A food found by its barcode: first on the phone, then in Open Food Facts (and then kept on the phone). */
    suspend fun byBarcode(code: String, ru: Boolean): Food? {
        foods.byBarcode(code)?.let { return it }
        if (!settings.now().online) return null
        val f = withContext(Dispatchers.IO) { runCatching { Off.product(code, ru) }.getOrNull() } ?: return null
        val id = foods.insert(f.copy(source = Source.SCAN))
        return foods.get(id)
    }

    /** Keeps a food found online so it works offline next time. */
    suspend fun keep(f: Food): Food {
        if (f.id > 0) return f
        f.barcode?.let { code -> foods.byBarcode(code)?.let { return it } }
        val id = foods.insert(f)
        return foods.get(id) ?: f.copy(id = id)
    }

    /** Saves a dish from its ingredients and the weight of the finished pot (or the raw total if not weighed). */
    suspend fun saveDish(dish: Food, items: List<Pair<Food, Double>>): Long {
        val total = Nutr.sum(items.map { (f, g) -> f.per(g) })
        val raw = items.sumOf { it.second }
        val cooked = dish.cooked.takeIf { it > 0 } ?: raw
        val k = if (cooked > 0) 100 / cooked else 0.0
        val serving = if (dish.servings > 0) cooked / dish.servings else 0.0
        val f = dish.copy(kcal = total.kcal * k, p = total.p * k, f = total.f * k, c = total.c * k, cooked = cooked, source = Source.DISH,
            portions = if (serving > 0) Portion.join(listOf(Portion("serving", serving))) else "")
        val id = if (f.id > 0) { foods.update(f); f.id } else foods.insert(f)
        foods.clearIngredients(id)
        foods.addIngredients(items.map { (food, g) -> Ingredient(dish = id, food = food.id, grams = g) })
        return id
    }

    /** A village recipe becomes the person's own dish. */
    suspend fun cook(v: Village, ru: Boolean): Long {
        foods.list().firstOrNull { it.village == v.key && !it.hidden }?.let { return it.id }
        val items = v.ingredients.mapNotNull { (k, g) -> foods.byKey(k)?.let { it to g } }
        return saveDish(Food(name = v.recipe(ru), kcal = 0.0, p = 0.0, f = 0.0, c = 0.0, servings = v.servings, village = v.key, cooked = v.cooked, source = Source.DISH), items)
    }

    /** Takes back what was just added. */
    suspend fun undo(ids: List<Long>) {
        for (id in ids) entries.get(id)?.let { entries.delete(it) }
    }

    suspend fun fair(day: Long, on: Boolean) = if (on) misc.fair(Fair(day)) else misc.unfair(Fair(day))

    suspend fun weigh(day: Long, kg: Double) {
        misc.weigh(Weigh(day, kg))
        // The newest weight drives the norm.
        val last = misc.weighList().maxByOrNull { it.day }
        if (last != null) settings.update { it.copy(profile = it.profile.copy(weight = last.kg)) }
    }

    fun remind(on: Boolean) {
        write { settings.update { it.copy(remind = on) } }
        Reminder.schedule(app, on)
    }
}

class SoftApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        container.scope.launch {
            try {
                container.seedFoods()
                if (BuildConfig.DEMO) DemoSeed.seedIfEmpty(this@SoftApp, container)
            } finally {
                container.seeding.value = false
            }
            if (container.settings.now().remind) Reminder.schedule(this@SoftApp, true)
        }
    }
}

val Context.container: AppContainer get() = (applicationContext as SoftApp).container

val Context.ru: Boolean get() = resources.configuration.locales[0].language == "ru"
