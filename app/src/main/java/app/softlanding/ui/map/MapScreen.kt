package app.softlanding.ui.map

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.softlanding.R
import app.softlanding.container
import app.softlanding.data.Nutr
import app.softlanding.domain.Envelopes
import app.softlanding.domain.Season
import app.softlanding.ui.Home
import app.softlanding.ui.Nav
import app.softlanding.ui.Overlay
import app.softlanding.ui.art.Patterns
import app.softlanding.ui.art.Stamp
import app.softlanding.ui.art.balloon
import app.softlanding.ui.art.regionColor
import app.softlanding.ui.components.Btn
import app.softlanding.ui.components.Card
import app.softlanding.ui.components.Page
import app.softlanding.ui.components.Pill
import app.softlanding.ui.components.Round
import app.softlanding.ui.components.Section
import app.softlanding.ui.components.Sheet
import app.softlanding.ui.components.grams
import app.softlanding.ui.components.kcal
import app.softlanding.ui.components.one
import app.softlanding.ui.theme.C
import app.softlanding.ui.theme.T
import kotlin.math.max
import kotlin.math.sin

fun seasonName(s: Season) = when (s) {
    Season.SUMMER -> R.string.season_summer
    Season.AUTUMN -> R.string.season_autumn
    Season.WINTER -> R.string.season_winter
    Season.SPRING -> R.string.season_spring
}

fun regionName(r: Int) = when (r) {
    0 -> R.string.region_0
    1 -> R.string.region_1
    2 -> R.string.region_2
    else -> R.string.region_3
}

@Composable
fun MapScreen(h: Home, nav: Nav) {
    val vs = h.villages
    val lap = h.lap(h.journey.next)
    val done = h.journey.soft - lap * vs.size
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding().padding(horizontal = 16.dp)) {
        Text(stringResource(R.string.map_title), style = T.Title, modifier = Modifier.padding(top = 12.dp))
        Text(stringResource(R.string.map_lap, lap + 1, stringResource(seasonName(h.season)).lowercase()), style = T.Small)

        Card(Modifier.fillMaxWidth().padding(top = 12.dp)) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text("$done", style = T.Hero.copy(color = C.meadowD))
                Text(" / ${vs.size}", style = T.Heading.copy(color = C.muted), modifier = Modifier.padding(bottom = 6.dp))
                Spacer(Modifier.weight(1f))
                Column(horizontalAlignment = Alignment.End) {
                    Text(stringResource(R.string.map_next), style = T.Small)
                    Text(h.nextVillage.name(h.ru), style = T.Heading)
                }
            }
            Box(Modifier.padding(top = 8.dp).fillMaxWidth().height(8.dp).clip(Pill).background(C.meadowL)) {
                Box(Modifier.fillMaxWidth(done.toFloat() / vs.size).height(8.dp).clip(Pill).background(C.meadow))
            }
            Text(stringResource(R.string.map_rule), style = T.Small, modifier = Modifier.padding(top = 8.dp))
        }

        WeightGoal(h)

        // The hangar: envelopes earned by landings.
        Card(Modifier.fillMaxWidth().padding(top = 12.dp), onClick = { nav.go(Overlay.Hangar) }) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Canvas(Modifier.size(56.dp, 70.dp)) { balloon(size.width / 2, 2f, size.width * .36f, h.s.envelope) }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.hangar), style = T.Heading)
                    Text(stringResource(R.string.hangar_count, h.journey.envelopes, Patterns.COUNT), style = T.Small)
                    Envelopes.nextAt(h.journey.soft)?.let { Text(stringResource(R.string.hangar_next, it - h.journey.soft), style = T.Small) }
                }
            }
        }

        // The route: villages one after another down a winding line.
        var region = -1
        for (v in vs) {
            if (v.region != region) {
                region = v.region
                Section(stringResource(regionName(region)))
            }
            val abs = lap * vs.size + v.index
            val reached = h.journey.soft > abs
            val next = abs == h.journey.next
            val x = sin(v.index * .9f) * .32f
            Box(Modifier.fillMaxWidth().height(78.dp).clickable { nav.go(Overlay.Village(abs)) }) {
                Canvas(Modifier.fillMaxSize()) {
                    // Each row draws its own halves of the line, so the line passes under every stamp.
                    fun xAt(i: Int) = size.width * (.5f + sin(i * .9f) * .32f)
                    val me = Offset(xAt(v.index), size.height / 2)
                    val effect = PathEffect.dashPathEffect(floatArrayOf(14f, 12f))
                    if (v.index > 0 && vs[v.index - 1].region == v.region)
                        drawLine(if (reached) regionColor(v.region) else C.lineD, Offset((xAt(v.index - 1) + me.x) / 2, 0f), me, strokeWidth = 5f, pathEffect = effect)
                    if (v.index < vs.size - 1 && vs[v.index + 1].region == v.region)
                        drawLine(if (h.journey.soft > abs + 1) regionColor(v.region) else C.lineD, me, Offset((xAt(v.index + 1) + me.x) / 2, size.height), strokeWidth = 5f, pathEffect = effect)
                }
                Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                    val left = x < 0
                    Box(Modifier.weight(.5f + x)) {
                        if (!left) VillageLabel(h, v, reached, next, Modifier.align(Alignment.CenterEnd).padding(end = 34.dp), TextAlign.End)
                    }
                    Box(Modifier.weight(.5f - x)) {
                        if (left) VillageLabel(h, v, reached, next, Modifier.align(Alignment.CenterStart).padding(start = 34.dp), TextAlign.Start)
                    }
                }
                Box(Modifier.align(Alignment.CenterStart).fillMaxWidth(.5f + x).offset(x = 0.dp), contentAlignment = Alignment.CenterEnd) {
                    Node(h, v.index, v.region, reached, next, h.journey.byDay.values.any { it.at == abs && it.wind })
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun Node(h: Home, index: Int, region: Int, reached: Boolean, next: Boolean, wind: Boolean) {
    Box(Modifier.offset(x = 26.dp).size(52.dp), contentAlignment = Alignment.Center) {
        when {
            reached -> Stamp(h.villages[index], wind, Modifier.size(52.dp).background(C.card, CircleShape), h.ru, label = false)
            next -> {
                Box(Modifier.size(46.dp).clip(CircleShape).background(C.redL).border(2.dp, C.red, CircleShape))
                Canvas(Modifier.size(30.dp, 38.dp)) { balloon(size.width / 2, 1f, size.width * .36f, h.s.envelope) }
            }
            else -> Box(Modifier.size(22.dp).clip(CircleShape).background(C.card).border(2.dp, regionColor(region).copy(alpha = .5f), CircleShape))
        }
    }
}

@Composable
private fun VillageLabel(h: Home, v: app.softlanding.domain.Village, reached: Boolean, next: Boolean, modifier: Modifier, align: TextAlign) {
    Column(modifier, horizontalAlignment = if (align == TextAlign.End) Alignment.End else Alignment.Start) {
        Text(v.name(h.ru), style = T.Strong.copy(color = if (reached || next) C.ink else C.muted), textAlign = align, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(if (reached) v.recipe(h.ru) else if (next) stringResource(R.string.map_flying_here) else stringResource(R.string.map_locked_short), style = T.Small, textAlign = align,
            maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun WeightGoal(h: Home) {
    val p = h.s.profile
    val start = h.weighs.minByOrNull { it.day }?.kg ?: return
    val now = h.trend.lastOrNull()?.second ?: return
    if (p.direction == 0 && kotlin.math.abs(start - p.goal) < .5) return
    val total = start - p.goal
    val gone = start - now
    val k = if (total != 0.0) (gone / total).toFloat().coerceIn(0f, 1f) else 1f
    Card(Modifier.fillMaxWidth().padding(top = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.goal_title), style = T.Heading)
                Text(stringResource(R.string.goal_line, one(start), one(now), one(p.goal)), style = T.Small)
            }
            Text(stringResource(R.string.goal_left, one(max(0.0, kotlin.math.abs(now - p.goal)))), style = T.Strong.copy(color = C.skyD))
        }
        Box(Modifier.padding(top = 8.dp).fillMaxWidth().height(8.dp).clip(Pill).background(C.skyL)) {
            Box(Modifier.fillMaxWidth(k).height(8.dp).clip(Pill).background(C.skyD))
        }
    }
}

@Composable
fun VillageSheet(h: Home, abs: Int, nav: Nav) {
    val c = LocalContext.current.container
    val v = h.village(abs)
    val reached = h.journey.soft > abs
    val byKey = h.foods.associateBy { it.key }
    val parts = v.ingredients.mapNotNull { (k, g) -> byKey[k]?.let { it to g } }
    val total = Nutr.sum(parts.map { (f, g) -> f.per(g) })
    Sheet(nav::close, v.name(h.ru)) {
        Column(Modifier.verticalScroll(rememberScrollState())) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Stamp(v, false, Modifier.size(70.dp), h.ru, faded = !reached)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(stringResource(regionName(v.region)), style = T.Tiny)
                    Text(v.line(h.ru), style = T.Log)
                }
            }
            if (!reached) {
                Card(Modifier.fillMaxWidth().padding(top = 12.dp), color = C.paper) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Lock, null, tint = C.muted)
                        Spacer(Modifier.width(10.dp))
                        Text(stringResource(R.string.village_locked, abs - h.journey.soft + 1), style = T.Body, modifier = Modifier.weight(1f))
                    }
                }
                Text(v.recipe(h.ru), style = T.Heading.copy(color = C.muted), modifier = Modifier.padding(top = 12.dp))
            } else {
                Row(Modifier.padding(top = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.MenuBook, null, tint = C.goldD, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(v.recipe(h.ru), style = T.Heading)
                }
                Text(stringResource(R.string.village_serving, v.servings, kcal(total.kcal / v.servings), one(total.p / v.servings)), style = T.Small, modifier = Modifier.padding(top = 4.dp))
                Section(stringResource(R.string.dish_ingredients))
                for ((f, g) in parts) Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                    Text(f.title(h.ru), style = T.Body, modifier = Modifier.weight(1f))
                    Text(grams(g) + " " + stringResource(if (f.ml) R.string.ml else R.string.g), style = T.Body.copy(color = C.inkSoft))
                }
                Section(stringResource(R.string.village_steps))
                v.steps(h.ru).forEachIndexed { i, s ->
                    Row(Modifier.padding(vertical = 4.dp)) {
                        Text("${i + 1}", style = T.Strong.copy(color = C.red), modifier = Modifier.width(22.dp))
                        Text(s, style = T.Body, modifier = Modifier.weight(1f))
                    }
                }
                val dish = h.dishOf(v)
                if (dish == null) Btn(stringResource(R.string.village_cook), { c.write { cook(v, h.ru) } }, Modifier.fillMaxWidth().padding(top = 14.dp), icon = Icons.Rounded.MenuBook)
                else Row(Modifier.padding(top = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Check, null, tint = C.meadowD)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.village_in_pantry), style = T.Body, modifier = Modifier.weight(1f))
                    Btn(stringResource(R.string.open), { nav.go(Overlay.Dish(dish.id)) }, small = true, kind = app.softlanding.ui.components.Kind.SECONDARY)
                }
            }
        }
    }
}

@Composable
fun HangarPage(h: Home, nav: Nav) {
    val c = LocalContext.current.container
    val open = h.journey.envelopes
    Page(stringResource(R.string.hangar), nav::close) {
        Text(stringResource(R.string.hangar_hint), style = T.Small)
        for (row in (0 until Patterns.COUNT).chunked(3)) Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            for (i in row) {
                val ok = i < open
                val on = i == h.s.envelope
                Column(
                    Modifier.weight(1f).clip(Round).background(if (on) C.redL else C.card).border(if (on) 2.dp else 1.dp, if (on) C.red else C.line, Round)
                        .clickable(enabled = ok) { c.write { settings.update { it.copy(envelope = i) } } }.padding(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(Modifier.fillMaxWidth().aspectRatio(.8f), contentAlignment = Alignment.Center) {
                        Canvas(Modifier.fillMaxSize()) {
                            balloon(size.width / 2, size.height * .04f, size.width * .3f, i)
                            if (!ok) drawRect(Color(0xCCFFFCF6))
                        }
                        if (!ok) Icon(Icons.Rounded.Lock, null, tint = C.muted)
                    }
                    Text(if (ok) stringResource(R.string.hangar_open) else stringResource(R.string.hangar_at, Envelopes.AT[i]), style = T.Small, textAlign = TextAlign.Center)
                }
            }
        }
    }
}
