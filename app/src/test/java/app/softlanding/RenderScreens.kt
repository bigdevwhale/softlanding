package app.softlanding

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.test.core.app.ApplicationProvider
import app.softlanding.data.DemoSeed
import app.softlanding.data.Meal
import app.softlanding.domain.Days
import app.softlanding.ui.Overlay
import app.softlanding.ui.SoftRoot
import app.softlanding.ui.Tab
import app.softlanding.ui.art.Art
import app.softlanding.ui.onboarding.Onboarding
import app.softlanding.ui.theme.SoftTheme
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.time.LocalDate
import java.time.ZoneId

/** Every main screen over the demo flights, drawn to PNGs (run with `-Prender`), in both languages. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
abstract class ScreensBase {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()
    abstract val lang: String
    private val out by lazy { File(System.getProperty("render.out") ?: "build/art", "screens-$lang").apply { mkdirs() } }

    /** Today at two in the afternoon. */
    private val clock = LocalDate.now().atTime(14, 10).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

    @Before fun still() { Art.still = true }

    private fun seed(): app.softlanding.data.Settings = runBlocking {
        val app = ApplicationProvider.getApplicationContext<SoftApp>()
        app.container.seedFoods()
        DemoSeed.seedIfEmpty(app, app.container)
        app.container.settings.now()
    }

    private fun shot(name: String, waitFor: String? = tabLabel(), content: @Composable () -> Unit) {
        rule.mainClock.autoAdvance = false
        rule.setContent { SoftTheme { content() } }
        // The database answers on its own threads: wait until the tab bar is there.
        rule.waitUntil(20_000) { rule.mainClock.advanceTimeBy(100); waitFor == null || rule.onAllNodesWithText(waitFor).fetchSemanticsNodes().isNotEmpty() }
        rule.mainClock.advanceTimeBy(2_500)
        rule.waitForIdle()
        rule.mainClock.advanceTimeBy(800)
        rule.runOnUiThread {
            val view = rule.activity.window.decorView
            val image = android.graphics.Bitmap.createBitmap(view.width, view.height, android.graphics.Bitmap.Config.ARGB_8888)
            view.draw(android.graphics.Canvas(image))
            File(out, "$name.png").outputStream().use { image.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
        }
    }

    private fun screen(name: String, tab: Tab, overlay: ((app.softlanding.data.Settings) -> Overlay?)? = null) {
        val s = seed()
        shot(name) { SoftRoot(s, clock, tab, overlay?.invoke(s)) }
    }

    private fun today() = Days.of(clock, 4)

    private fun tabLabel() = ApplicationProvider.getApplicationContext<SoftApp>().getString(R.string.tab_pantry)

    @Test fun welcome() = shot("0-welcome", null) { Onboarding() }
    /** The launcher icon as a PNG for the README and the site. */
    @Test fun logo() {
        val ctx = ApplicationProvider.getApplicationContext<SoftApp>()
        val n = 512
        val image = android.graphics.Bitmap.createBitmap(n, n, android.graphics.Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(image)
        val clip = android.graphics.Path().apply { addRoundRect(0f, 0f, n.toFloat(), n.toFloat(), n * .22f, n * .22f, android.graphics.Path.Direction.CW) }
        canvas.clipPath(clip)
        // The adaptive icon is 108 units with a 72-unit safe zone: scale so the safe zone fills the square, plus a margin.
        val k = n / 80f * 108f
        val off = (n - k) / 2
        for (id in listOf(R.drawable.ic_launcher_background, R.drawable.ic_launcher_foreground)) {
            val d = androidx.core.content.ContextCompat.getDrawable(ctx, id)!!
            d.setBounds(off.toInt(), off.toInt(), (off + k).toInt(), (off + k).toInt())
            d.draw(canvas)
        }
        File(out.parentFile, "logo.png").outputStream().use { image.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Test fun flight() = screen("1-flight", Tab.FLIGHT)
    @Test fun add() = screen("2-add", Tab.FLIGHT) { Overlay.Add(Meal.DINNER, today()) }
    @Test fun amount() {
        val app = ApplicationProvider.getApplicationContext<SoftApp>()
        val s = seed()
        val f = runBlocking { app.container.foods.byKey("buckwheat_c")!! }
        shot("3-amount") { SoftRoot(s, clock, Tab.FLIGHT, Overlay.Amount(f, Meal.DINNER, today())) }
    }
    @Test fun map() = screen("4-map", Tab.MAP)
    @Test fun journal() = screen("5-journal", Tab.JOURNAL)
    @Test fun village() = screen("6-village", Tab.MAP) { Overlay.Village(1) }
    @Test fun pantry() = screen("7-pantry", Tab.PANTRY)
    @Test fun hangar() = screen("8-hangar", Tab.MAP) { Overlay.Hangar }
    @Test fun dish() {
        val app = ApplicationProvider.getApplicationContext<SoftApp>()
        val s = seed()
        val d = runBlocking { app.container.foods.list().first { it.village != null } }
        shot("9-dish") { SoftRoot(s, clock, Tab.PANTRY, Overlay.Dish(d.id)) }
    }
}

@Config(sdk = [34], qualifiers = "en-w411dp-h891dp-xxhdpi")
class RenderScreensEn : ScreensBase() { override val lang = "en" }

@Config(sdk = [34], qualifiers = "ru-w411dp-h891dp-xxhdpi")
class RenderScreensRu : ScreensBase() { override val lang = "ru" }
