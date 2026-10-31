package app.softlanding.ui.flight

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Celebration
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.MonitorWeight
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.softlanding.R
import app.softlanding.container
import app.softlanding.data.Entry
import app.softlanding.data.Meal
import app.softlanding.domain.Days
import app.softlanding.domain.Gauges
import app.softlanding.domain.Landing
import app.softlanding.domain.Search
import app.softlanding.ui.Home
import app.softlanding.ui.Nav
import app.softlanding.ui.Overlay
import app.softlanding.ui.art.FlightScene
import app.softlanding.ui.components.Btn
import app.softlanding.ui.components.Card
import app.softlanding.ui.components.IconBtn
import app.softlanding.ui.components.Kind
import app.softlanding.ui.components.LocalBus
import app.softlanding.ui.components.MacroBar
import app.softlanding.ui.components.Pill
import app.softlanding.ui.components.grams
import app.softlanding.ui.components.kcal
import app.softlanding.ui.theme.C
import app.softlanding.ui.theme.T
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun FlightScreen(h: Home, day: Long, nav: Nav) {
    val ctx = LocalContext.current
    val c = ctx.container
    val bus = LocalBus.current
    val isToday = day == h.today
    val norm = h.norm(day)
    val eaten = h.eaten(day)
    val left = norm.kcal - eaten.kcal
    val flight = h.flight(day)
    val fair = day in h.fairs

    // Something just went into the basket: say so, with an undo.
    val loaded by c.loaded.collectAsStateWithLifecycle()
    LaunchedEffect(loaded?.stamp) {
        val l = loaded ?: return@LaunchedEffect
        if (System.nanoTime() - l.stamp > 5_000_000_000L) return@LaunchedEffect
        val what = if (l.names.size == 1) l.names[0] else ctx.getString(R.string.n_items, l.names.size)
        bus.say(ctx.getString(R.string.loaded, what, kcal(l.kcal)), ctx.getString(R.string.undo)) { c.write { undo(l.ids) } }
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            // The sky, with the date over it.
            Box(Modifier.fillMaxWidth().height(330.dp).clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))) {
                val hour = Days.hour(h.now)
                FlightScene(h.scene(day, hour, Days.progress(h.now)), Modifier.fillMaxSize())
                // A soft shade at the top keeps the date readable over any sky.
                Box(Modifier.fillMaxWidth().height(120.dp).background(Brush.verticalGradient(listOf(Color(0x66141A26), Color(0x00141A26)))))
                Row(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconBtn(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, stringResource(R.string.prev_day), { nav.day(day - 1) }, bg = Color(0x33FFFFFF), tint = Color.White)
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(dayTitle(day, h.today, h.ru), style = T.Heading.copy(color = Color.White), maxLines = 1)
                        Text(subtitle(h, day), style = T.Small.copy(color = Color(0xEEFFFFFF)), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    if (day < h.today) IconBtn(Icons.AutoMirrored.Rounded.KeyboardArrowRight, stringResource(R.string.next_day), { nav.day(day + 1) }, bg = Color(0x33FFFFFF), tint = Color.White)
                    else IconBtn(Icons.Rounded.Settings, stringResource(R.string.settings), { nav.go(Overlay.Settings) }, bg = Color(0x33FFFFFF), tint = Color.White)
                }
                if (h.journey.streak > 1 && isToday) Row(
                    Modifier.align(Alignment.BottomStart).padding(12.dp).background(Color(0xCC1E2638), Pill).padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Rounded.LocalFireDepartment, null, tint = C.gold, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.streak_days, h.journey.streak), style = T.Small.copy(color = Color.White))
                }
            }

            Column(Modifier.padding(horizontal = 16.dp)) {
                // What's left before the meadow.
                Card(Modifier.fillMaxWidth().padding(top = 14.dp)) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(when {
                                fair -> R.string.fair_today
                                left >= 0 -> R.string.left_to_meadow
                                else -> R.string.over_meadow
                            }), style = T.Small)
                            Text(if (fair) "—" else kcal(abs(left)), style = T.Hero.copy(color = when {
                                fair -> C.gold
                                eaten.kcal > norm.hi -> C.red
                                eaten.kcal >= norm.lo -> C.meadowD
                                else -> C.ink
                            }))
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(stringResource(R.string.eaten_of, kcal(eaten.kcal), kcal(norm.kcal.toDouble())), style = T.Body.copy(color = C.inkSoft))
                            Text(stringResource(R.string.meadow_range, kcal(norm.lo.toDouble()), kcal(norm.hi.toDouble())), style = T.Small)
                        }
                    }
                    Text(status(h, day, eaten.kcal), style = T.Body.copy(color = C.inkSoft), modifier = Modifier.padding(top = 6.dp))
                    Row(Modifier.padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        MacroBar(stringResource(R.string.protein), eaten.p, norm.protein, C.protein, Modifier.weight(1f))
                        MacroBar(stringResource(R.string.fat), eaten.f, norm.fat, C.fat, Modifier.weight(1f))
                        MacroBar(stringResource(R.string.carbs), eaten.c, norm.carbs, C.carbs, Modifier.weight(1f))
                    }
                    if (Gauges.of(eaten, norm).wind && !fair) Text(stringResource(R.string.wind_ok), style = T.Small.copy(color = C.skyD), modifier = Modifier.padding(top = 8.dp))
                }

                for (m in Meal.entries) MealCard(h, day, m, nav)

                // The rest of the day's chores, quietly at the bottom.
                if (isToday && h.weighDue) Card(Modifier.fillMaxWidth().padding(top = 12.dp), onClick = { nav.go(Overlay.Weigh) }, color = C.skyL, border = C.skyL) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.MonitorWeight, null, tint = C.skyD)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(R.string.weigh_title), style = T.Strong)
                            Text(stringResource(R.string.weigh_hint), style = T.Small)
                        }
                    }
                }
                if (flight == null) {
                    val leftFairs = h.fairsLeft(day)
                    Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.Center) {
                        if (fair) Btn(stringResource(R.string.fair_cancel), { c.write { fair(day, false) } }, kind = Kind.GHOST, icon = Icons.Rounded.Celebration, small = true)
                        else if (leftFairs > 0) Btn(stringResource(R.string.fair_declare, leftFairs), {
                            c.write { fair(day, true) }
                            bus.say(ctx.getString(R.string.fair_declared))
                        }, kind = Kind.GHOST, icon = Icons.Rounded.Celebration, small = true)
                    }
                } else Text(app.softlanding.domain.Logbook.line(flight, h.village(flight.at).name(h.ru), h.ru), style = T.Log, modifier = Modifier.padding(top = 14.dp, start = 4.dp, end = 4.dp))
                Spacer(Modifier.height(110.dp))
            }
        }

        // Two ways in, always at hand: scan a pack, or type.
        Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(96.dp).background(Brush.verticalGradient(listOf(C.paper.copy(alpha = 0f), C.paper))))
        Row(Modifier.align(Alignment.BottomCenter).padding(bottom = 14.dp, start = 16.dp, end = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            val meal = if (isToday) Search.mealAt(Days.hour(h.now)) else Meal.DINNER
            Box(Modifier.size(56.dp).clip(CircleShape).background(C.ink).clickable { nav.go(Overlay.Add(meal, day, scan = true)) }, contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.QrCodeScanner, stringResource(R.string.scan), tint = C.card, modifier = Modifier.size(26.dp))
            }
            Btn(stringResource(R.string.add_food), { nav.go(Overlay.Add(meal, day)) }, Modifier.height(56.dp), icon = Icons.Rounded.Add)
        }
    }
}

@Composable
private fun MealCard(h: Home, day: Long, m: Meal, nav: Nav) {
    val c = LocalContext.current.container
    val es = h.meal(day, m)
    val sum = es.sumOf { it.kcal }
    Card(Modifier.fillMaxWidth().padding(top = 12.dp), pad = 0.dp) {
        Row(Modifier.fillMaxWidth().clickable { nav.go(Overlay.Add(m, day)) }.padding(start = 16.dp, end = 8.dp, top = 10.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(mealName(m)), style = T.Heading)
                if (es.isNotEmpty()) Text(stringResource(R.string.kcal_n, kcal(sum)), style = T.Small)
            }
            Box(Modifier.size(40.dp).clip(CircleShape).background(C.redL), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Add, stringResource(R.string.add_to, stringResource(mealName(m))), tint = C.red)
            }
        }
        for (e in es) EntryRow(e) { nav.go(Overlay.EditEntry(e.id)) }
        if (es.isEmpty()) {
            // The same meal yesterday, one tap away.
            val y = h.meal(day - 1, m)
            if (y.isNotEmpty()) Row(Modifier.fillMaxWidth().clickable {
                c.write { copy(y, m, day) }
            }.padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.ContentCopy, null, tint = C.muted, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.as_yesterday, y.joinToString(", ") { it.name }, kcal(y.sumOf { it.kcal })), style = T.Small, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Spacer(Modifier.height(6.dp))
    }
}

@Composable
fun EntryRow(e: Entry, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(e.name, style = T.Body, maxLines = 1, overflow = TextOverflow.Ellipsis)
            val amount = when {
                e.how.isNotBlank() -> e.how
                e.grams > 0 -> grams(e.grams) + " " + stringResource(if (e.ml) R.string.ml else R.string.g)
                else -> stringResource(R.string.quick_entry)
            }
            Text(amount, style = T.Small)
        }
        Text(kcal(e.kcal), style = T.Number)
    }
}

fun mealName(m: Meal) = when (m) {
    Meal.BREAKFAST -> R.string.meal_breakfast
    Meal.LUNCH -> R.string.meal_lunch
    Meal.DINNER -> R.string.meal_dinner
    Meal.SNACK -> R.string.meal_snack
}

@Composable
private fun dayTitle(day: Long, today: Long, ru: Boolean): String = when (day) {
    today -> stringResource(R.string.today)
    today - 1 -> stringResource(R.string.yesterday)
    else -> Days.date(day).format(DateTimeFormatter.ofPattern(if (ru) "d MMMM, EEEE" else "EEEE, d MMMM", if (ru) Locale.forLanguageTag("ru") else Locale.ENGLISH))
}

@Composable
private fun subtitle(h: Home, day: Long): String {
    val f = h.flight(day)
    return if (f != null) stringResource(landingName(f.landing))
    else stringResource(R.string.flying_to, h.nextVillage.name(h.ru))
}

fun landingName(l: Landing) = when (l) {
    Landing.SOFT -> R.string.l_soft
    Landing.EARLY -> R.string.l_early
    Landing.DRIFT -> R.string.l_drift
    Landing.FOG -> R.string.l_fog
    Landing.FAIR -> R.string.l_fair
}

/** One line about how the flight goes. */
@Composable
private fun status(h: Home, day: Long, eaten: Double): String {
    val n = h.norm(day)
    val f = h.flight(day)
    if (f != null) return stringResource(when (f.landing) {
        Landing.SOFT -> R.string.st_done_soft
        Landing.EARLY -> R.string.st_done_early
        Landing.DRIFT -> R.string.st_done_drift
        Landing.FOG -> R.string.st_done_fog
        Landing.FAIR -> R.string.st_done_fair
    })
    if (day in h.fairs) return stringResource(R.string.st_fair)
    return when {
        eaten == 0.0 -> stringResource(R.string.st_empty)
        eaten > n.hi -> stringResource(R.string.st_over, kcal(eaten - n.hi))
        eaten >= n.lo -> stringResource(R.string.st_meadow)
        else -> {
            val share = (eaten / n.kcal * 100).roundToInt()
            stringResource(R.string.st_flying, share)
        }
    }
}
