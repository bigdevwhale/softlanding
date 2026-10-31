package app.softlanding.ui.journal

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.softlanding.R
import app.softlanding.domain.Days
import app.softlanding.domain.Landing
import app.softlanding.domain.Logbook
import app.softlanding.ui.Home
import app.softlanding.ui.Nav
import app.softlanding.ui.Overlay
import app.softlanding.ui.components.Btn
import app.softlanding.ui.components.Card
import app.softlanding.ui.components.IconBtn
import app.softlanding.ui.components.Kind
import app.softlanding.ui.components.RoundS
import app.softlanding.ui.components.Section
import app.softlanding.ui.components.kcal
import app.softlanding.ui.components.one
import app.softlanding.ui.flight.landingName
import app.softlanding.ui.theme.C
import app.softlanding.ui.theme.T
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

fun landingColor(l: Landing?): Color = when (l) {
    Landing.SOFT -> C.meadow
    Landing.EARLY -> C.red
    Landing.DRIFT -> C.sky
    Landing.FAIR -> C.gold
    Landing.FOG -> C.line
    null -> Color.Transparent
}

@Composable
fun JournalScreen(h: Home, nav: Nav) {
    val loc = if (h.ru) Locale.forLanguageTag("ru") else Locale.ENGLISH
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding().padding(horizontal = 16.dp)) {
        Text(stringResource(R.string.journal_title), style = T.Title, modifier = Modifier.padding(top = 12.dp))

        val j = h.journey
        Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Stat(j.streak.toString(), stringResource(R.string.stat_streak), C.red, Modifier.weight(1f))
            Stat(j.soft.toString(), stringResource(R.string.stat_soft), C.meadowD, Modifier.weight(1f))
            Stat(j.bestSoftStreak.toString(), stringResource(R.string.stat_best), C.goldD, Modifier.weight(1f))
        }

        // A month of landings, coloured.
        var shift by remember { mutableIntStateOf(0) }
        val month = Days.date(h.today).withDayOfMonth(1).plusMonths(shift.toLong())
        Card(Modifier.fillMaxWidth().padding(top = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBtn(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, stringResource(R.string.prev_month), { shift-- }, size = 36.dp)
                Text(month.month.getDisplayName(TextStyle.FULL_STANDALONE, loc).replaceFirstChar { it.uppercase() } + " " + month.year, style = T.Heading,
                    textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
                IconBtn(Icons.AutoMirrored.Rounded.KeyboardArrowRight, stringResource(R.string.next_month), { if (shift < 0) shift++ }, size = 36.dp)
            }
            val first = month.toEpochDay()
            val pad = month.dayOfWeek.value - 1
            val days = month.lengthOfMonth()
            Row(Modifier.padding(top = 8.dp)) {
                for (d in 0 until 7) Text(java.time.DayOfWeek.of(d + 1).getDisplayName(TextStyle.SHORT_STANDALONE, loc), style = T.Tiny, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
            }
            for (week in 0 until (pad + days + 6) / 7) Row(Modifier.padding(top = 4.dp)) {
                for (wd in 0 until 7) {
                    val i = week * 7 + wd - pad
                    Box(Modifier.weight(1f).aspectRatio(1f).padding(2.dp), contentAlignment = Alignment.Center) {
                        if (i in 0 until days) {
                            val day = first + i
                            val f = h.flight(day)
                            val today = day == h.today
                            val bg = landingColor(f?.landing)
                            Box(Modifier.fillMaxSize().clip(CircleShape).background(if (f != null) bg.copy(alpha = if (f.landing == Landing.FOG) 1f else .85f) else Color.Transparent)
                                .then(if (today) Modifier.border(2.dp, C.ink, CircleShape) else Modifier)
                                .clickable(enabled = day <= h.today) { nav.day(day) }, contentAlignment = Alignment.Center) {
                                Text("${i + 1}", style = T.Small.copy(color = if (f != null && f.landing != Landing.FOG) Color.White else if (day > h.today) C.faint else C.ink))
                                if (f?.wind == true) Box(Modifier.align(Alignment.TopEnd).size(7.dp).clip(CircleShape).background(C.gold))
                            }
                        }
                    }
                }
            }
            Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                for (l in listOf(Landing.SOFT, Landing.EARLY, Landing.DRIFT, Landing.FAIR)) Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(10.dp).clip(CircleShape).background(landingColor(l)))
                    Text(" " + stringResource(landingShort(l)), style = T.Tiny)
                }
            }
        }

        // The last seven days in numbers.
        val week = (1..7).map { h.today - it }.mapNotNull { h.flight(it) }.filter { it.landing != Landing.FOG && it.landing != Landing.FAIR }
        if (week.isNotEmpty()) Card(Modifier.fillMaxWidth().padding(top = 12.dp)) {
            Text(stringResource(R.string.week_title), style = T.Heading)
            Row(Modifier.padding(top = 6.dp)) {
                Column(Modifier.weight(1f)) {
                    Text(kcal(week.map { it.eaten.kcal }.average()), style = T.Title)
                    Text(stringResource(R.string.week_avg), style = T.Small)
                }
                Column(Modifier.weight(1f)) {
                    Text(one(week.map { it.eaten.p }.average()), style = T.Title.copy(color = C.protein))
                    Text(stringResource(R.string.week_protein), style = T.Small)
                }
                Column(Modifier.weight(1f)) {
                    Text("${week.count { it.landing == Landing.SOFT }}/${week.size}", style = T.Title.copy(color = C.meadowD))
                    Text(stringResource(R.string.week_soft), style = T.Small)
                }
            }
            Bars(week.reversed().map { it.eaten.kcal.toFloat() to it.norm.kcal.toFloat() }, week.reversed().map { landingColor(it.landing) })
        }

        // Weight.
        Section(stringResource(R.string.weight_title), trail = { Btn(stringResource(R.string.weigh_short), { nav.go(Overlay.Weigh) }, small = true, kind = Kind.SECONDARY) })
        if (h.weighs.size >= 2) Card(Modifier.fillMaxWidth()) {
            WeightChart(h)
            val t = h.trend
            val first = t.first().second
            val last = t.last().second
            Text(stringResource(R.string.weight_summary, one(last), (if (last - first > 0) "+" else "") + one(last - first)), style = T.Small, modifier = Modifier.padding(top = 6.dp))
        } else Text(stringResource(R.string.weight_empty), style = T.Small)

        // The logbook.
        Section(stringResource(R.string.logbook))
        val fs = h.journey.flights.filter { it.landing != Landing.FOG || it.day >= h.today - 7 }.takeLast(30).reversed()
        if (fs.isEmpty()) Text(stringResource(R.string.logbook_empty), style = T.Small)
        for (f in fs) Column(Modifier.fillMaxWidth().clip(RoundS).clickable { nav.day(f.day) }.padding(vertical = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(10.dp).clip(CircleShape).background(landingColor(f.landing)))
                Text("  " + Days.date(f.day).format(DateTimeFormatter.ofPattern("d MMM", loc)) + " · " + stringResource(landingName(f.landing)), style = T.Tiny, modifier = Modifier.weight(1f))
                if (f.landing != Landing.FOG) Text(kcal(f.eaten.kcal), style = T.Small)
            }
            Text(Logbook.line(f, h.village(f.at).name(h.ru), h.ru), style = T.Log, modifier = Modifier.padding(start = 18.dp, top = 2.dp))
        }
        Spacer(Modifier.height(24.dp))
    }
}

fun landingShort(l: Landing) = when (l) {
    Landing.SOFT -> R.string.ls_soft
    Landing.EARLY -> R.string.ls_early
    Landing.DRIFT -> R.string.ls_drift
    Landing.FAIR -> R.string.ls_fair
    Landing.FOG -> R.string.ls_fog
}

@Composable
private fun Stat(v: String, label: String, color: Color, modifier: Modifier) {
    Card(modifier, pad = 12.dp) {
        Text(v, style = T.Title.copy(color = color))
        Text(label, style = T.Small, maxLines = 2)
    }
}

@Composable
private fun Bars(xs: List<Pair<Float, Float>>, colors: List<Color>) {
    Canvas(Modifier.fillMaxWidth().height(90.dp).padding(top = 12.dp)) {
        val max = xs.maxOf { maxOf(it.first, it.second) } * 1.1f
        val w = size.width / xs.size
        xs.forEachIndexed { i, (v, n) ->
            val hgt = size.height * v / max
            drawRoundRect(colors[i], Offset(i * w + w * .2f, size.height - hgt), androidx.compose.ui.geometry.Size(w * .6f, hgt), androidx.compose.ui.geometry.CornerRadius(8f))
            val ny = size.height - size.height * n / max
            drawLine(C.ink.copy(alpha = .35f), Offset(i * w + w * .1f, ny), Offset(i * w + w * .9f, ny), strokeWidth = 3f)
        }
    }
}

@Composable
private fun WeightChart(h: Home) {
    val ws = h.weighs.sortedBy { it.day }
    val t = h.trend
    Canvas(Modifier.fillMaxWidth().height(140.dp)) {
        val d0 = ws.first().day
        val d1 = maxOf(ws.last().day, d0 + 1)
        val vals = ws.map { it.kg } + t.map { it.second } + listOf(h.s.profile.goal)
        val lo = vals.min() - .5
        val hi = vals.max() + .5
        fun x(d: Long) = size.width * (d - d0) / (d1 - d0).toFloat()
        fun y(v: Double) = (size.height * (1 - (v - lo) / (hi - lo))).toFloat()
        val gy = y(h.s.profile.goal)
        drawLine(C.meadow, Offset(0f, gy), Offset(size.width, gy), strokeWidth = 3f, pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(12f, 10f)))
        for (w in ws) drawCircle(C.skyD.copy(alpha = .35f), 7f, Offset(x(w.day), y(w.kg)))
        val p = Path().apply { t.forEachIndexed { i, (d, v) -> if (i == 0) moveTo(x(d), y(v)) else lineTo(x(d), y(v)) } }
        drawPath(p, C.skyD, style = Stroke(5f))
    }
}
