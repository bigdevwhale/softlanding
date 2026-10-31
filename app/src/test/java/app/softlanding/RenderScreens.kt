package app.softlanding

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.createAndroidComposeRule
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

    private fun shot(name: String, content: @Composable () -> Unit) {
        rule.mainClock.autoAdvance = false
        rule.setContent { SoftTheme { content() } }
        rule.waitUntil(15_000) { rule.mainClock.advanceTimeBy(100); true }
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

    @Test fun welcome() = shot("0-welcome") { Onboarding() }
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
