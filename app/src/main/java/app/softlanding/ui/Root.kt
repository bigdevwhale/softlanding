package app.softlanding.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.Kitchen
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.softlanding.R
import app.softlanding.container
import app.softlanding.data.Food
import app.softlanding.data.Meal
import app.softlanding.data.Settings
import app.softlanding.domain.LabelGuess
import app.softlanding.domain.Route
import app.softlanding.domain.Search
import app.softlanding.domain.Days
import app.softlanding.ru
import app.softlanding.ui.add.AddPage
import app.softlanding.ui.add.AmountSheet
import app.softlanding.ui.add.FoodEditor
import app.softlanding.ui.add.QuickKcalSheet
import app.softlanding.ui.components.Bus
import app.softlanding.ui.components.LocalBus
import app.softlanding.ui.components.Pill
import app.softlanding.ui.dish.DishEditor
import app.softlanding.ui.dish.QrSheet
import app.softlanding.ui.flight.CeremonySheet
import app.softlanding.ui.flight.FlightScreen
import app.softlanding.ui.flight.WeighSheet
import app.softlanding.ui.journal.JournalScreen
import app.softlanding.ui.map.HangarPage
import app.softlanding.ui.map.MapScreen
import app.softlanding.ui.map.VillageSheet
import app.softlanding.ui.onboarding.Onboarding
import app.softlanding.ui.pantry.PantryScreen
import app.softlanding.ui.settings.SettingsPage
import app.softlanding.ui.theme.C
import app.softlanding.ui.theme.T
import kotlinx.coroutines.delay

enum class Tab { FLIGHT, MAP, JOURNAL, PANTRY }

/** What is open over the tabs. */
sealed class Overlay {
    data class Add(val meal: Meal, val day: Long, val scan: Boolean = false) : Overlay()
    data class Amount(val food: Food, val meal: Meal, val day: Long, val entry: Long = 0, val grams: Double = 0.0, val back: Overlay? = null) : Overlay()
    data class EditEntry(val entry: Long) : Overlay()
    data class FoodEdit(val food: Food?, val barcode: String? = null, val guess: LabelGuess? = null, val name: String = "", val then: Pair<Meal, Long>? = null) : Overlay()
    data class Dish(val id: Long, val then: Pair<Meal, Long>? = null) : Overlay()
    data class Qr(val id: Long) : Overlay()
    data class Quick(val meal: Meal, val day: Long) : Overlay()
    data class Village(val abs: Int) : Overlay()
    data object Hangar : Overlay()
    data object Weigh : Overlay()
    data object Settings : Overlay()
}

class Nav(val go: (Overlay?) -> Unit, val tab: (Tab) -> Unit, val day: (Long) -> Unit) {
    fun close() = go(null)
}

/** [clock], [tab0] and [overlay0] let renders open a fixed moment on a given screen; the app passes none. */
@Composable
fun SoftRoot(s: Settings, clock: Long? = null, tab0: Tab = Tab.FLIGHT, overlay0: Overlay? = null, action: Pair<String, Long>? = null) {
    val ctx = LocalContext.current
    val c = ctx.container
    app.softlanding.ui.components.Fmt.comma = ctx.ru
    if (!s.onboarded) { Onboarding(); return }
    val foodsFlow = remember { c.foods.all() }
    val entriesFlow = remember { c.entries.all() }
    val weighsFlow = remember { c.misc.weighs() }
    val fairsFlow = remember { c.misc.fairs() }
    val platesFlow = remember { c.misc.plates() }
    val ingFlow = remember { c.foods.allIngredients() }
    val foods by foodsFlow.collectAsStateWithLifecycle(initialValue = null)
    val entries by entriesFlow.collectAsStateWithLifecycle(initialValue = null)
    val weighs by weighsFlow.collectAsStateWithLifecycle(initialValue = emptyList())
    val fairs by fairsFlow.collectAsStateWithLifecycle(initialValue = emptyList())
    val plates by platesFlow.collectAsStateWithLifecycle(initialValue = emptyList())
    val ings by ingFlow.collectAsStateWithLifecycle(initialValue = emptyList())
    var now by remember { mutableLongStateOf(clock ?: System.currentTimeMillis()) }
    LaunchedEffect(Unit) { if (clock == null) while (true) { delay(30_000); now = System.currentTimeMillis() } }
    val fs = foods ?: return
    val es = entries ?: return
    val villages = remember { Route.villages(ctx) }
    val h = remember(s, fs, es, weighs, fairs, plates, ings, now) {
        Home(s, fs, es, weighs, fairs.map { it.day }.toSet(), plates, ings, villages, if (clock == null) maxOf(now, System.currentTimeMillis()) else now, ctx.ru)
    }
    val bus = remember { Bus() }
    var tab by rememberSaveable { mutableStateOf(tab0) }
    var overlay by remember { mutableStateOf(overlay0) }
    var day by rememberSaveable { mutableLongStateOf(h.today) }
    // A new day rolls in while the app is open: follow it if we were on today.
    var lastToday by remember { mutableLongStateOf(h.today) }
    LaunchedEffect(h.today) { if (day == lastToday) day = h.today; lastToday = h.today }
    val nav = remember { Nav({ overlay = it }, { tab = it; overlay = null }, { day = it; tab = Tab.FLIGHT; overlay = null }) }

    // A launcher shortcut or the evening note: straight to adding or scanning.
    LaunchedEffect(action) {
        val meal = Search.mealAt(Days.hour(System.currentTimeMillis()))
        when (action?.first) {
            "add" -> overlay = Overlay.Add(meal, h.today)
            "scan" -> overlay = Overlay.Add(meal, h.today, scan = true)
        }
    }

    CompositionLocalProvider(LocalBus provides bus) {
        Box(Modifier.fillMaxSize().background(C.paper)) {
            Column(Modifier.fillMaxSize()) {
                Box(Modifier.weight(1f)) {
                    when (tab) {
                        Tab.FLIGHT -> FlightScreen(h, day, nav)
                        Tab.MAP -> MapScreen(h, nav)
                        Tab.JOURNAL -> JournalScreen(h, nav)
                        Tab.PANTRY -> PantryScreen(h, nav)
                    }
                }
                TabBar(tab) { tab = it; if (it == Tab.FLIGHT) day = h.today }
            }
            when (val o = overlay) {
                is Overlay.Add -> AddPage(h, o, nav)
                is Overlay.Amount -> AmountSheet(h, o, nav)
                is Overlay.EditEntry -> h.entries.firstOrNull { it.id == o.entry }?.let { e ->
                    val f = e.food?.let { h.byId[it] } ?: Food(name = e.name, kcal = if (e.grams > 0) e.kcal * 100 / e.grams else e.kcal, p = if (e.grams > 0) e.p * 100 / e.grams else e.p,
                        f = if (e.grams > 0) e.f * 100 / e.grams else e.f, c = if (e.grams > 0) e.c * 100 / e.grams else e.c, ml = e.ml)
                    AmountSheet(h, Overlay.Amount(f, e.meal, e.day, e.id, e.grams), nav)
                }
                is Overlay.FoodEdit -> FoodEditor(h, o, nav)
                is Overlay.Dish -> DishEditor(h, o, nav)
                is Overlay.Qr -> h.byId[o.id]?.let { QrSheet(it, nav) }
                is Overlay.Quick -> QuickKcalSheet(h, o, nav)
                is Overlay.Village -> VillageSheet(h, o.abs, nav)
                Overlay.Hangar -> HangarPage(h, nav)
                Overlay.Weigh -> WeighSheet(h, nav)
                Overlay.Settings -> SettingsPage(h, nav)
                null -> if (clock == null && h.unseen.isNotEmpty()) CeremonySheet(h, h.unseen, nav)
            }
            Notes(Modifier.align(Alignment.BottomCenter).padding(bottom = if (overlay == null && tab == Tab.FLIGHT) 160.dp else 84.dp))
        }
    }
}

@Composable
private fun TabBar(tab: Tab, onTab: (Tab) -> Unit) {
    Column(Modifier.fillMaxWidth().background(C.card)) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(C.line))
        Row(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 8.dp, vertical = 6.dp)) {
            for ((t, l, icon) in listOf(
                Triple(Tab.FLIGHT, R.string.tab_flight, Icons.Rounded.WbSunny),
                Triple(Tab.MAP, R.string.tab_map, Icons.Rounded.Map),
                Triple(Tab.JOURNAL, R.string.tab_journal, Icons.Rounded.AutoStories),
                Triple(Tab.PANTRY, R.string.tab_pantry, Icons.Rounded.Kitchen),
            )) {
                val on = t == tab
                Column(Modifier.weight(1f).clickable { onTab(t) }.padding(vertical = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.size(52.dp, 30.dp).background(if (on) C.redL else C.card, Pill), contentAlignment = Alignment.Center) {
                        Icon(icon, null, tint = if (on) C.red else C.muted, modifier = Modifier.size(22.dp))
                    }
                    Spacer(Modifier.height(2.dp))
                    Text(stringResource(l), style = T.Tiny.copy(color = if (on) C.ink else C.muted, letterSpacing = 0.sp), maxLines = 1)
                }
            }
        }
    }
}

/** Notes slide up from the bottom for a few seconds; an action (undo) can ride on one. */
@Composable
private fun Notes(modifier: Modifier) {
    val bus = LocalBus.current
    val n = bus.notes.firstOrNull()
    LaunchedEffect(n?.id) { if (n != null) { delay(if (n.action != null) 6500 else 3200); bus.notes.remove(n) } }
    AnimatedVisibility(n != null, modifier, enter = slideInVertically { it } + fadeIn(), exit = slideOutVertically { it } + fadeOut()) {
        val note = n ?: return@AnimatedVisibility
        Row(Modifier.padding(horizontal = 14.dp).fillMaxWidth().background(C.ink, Pill).padding(start = 18.dp, end = 6.dp).height(48.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(note.text, style = T.Body.copy(color = C.card), modifier = Modifier.weight(1f), maxLines = 2)
            if (note.action != null) Text(note.action, style = T.Button.copy(color = C.gold), modifier = Modifier.clickable { note.run?.invoke(); bus.notes.remove(note) }.padding(12.dp))
        }
    }
}
