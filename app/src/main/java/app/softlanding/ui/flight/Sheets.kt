package app.softlanding.ui.flight

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Air
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.softlanding.R
import app.softlanding.container
import app.softlanding.domain.Envelopes
import app.softlanding.domain.Flight
import app.softlanding.domain.Landing
import app.softlanding.domain.Logbook
import app.softlanding.domain.Route
import app.softlanding.ui.Home
import app.softlanding.ui.Nav
import app.softlanding.ui.Overlay
import app.softlanding.ui.art.FlightScene
import app.softlanding.ui.art.SceneState
import app.softlanding.ui.art.Stamp
import app.softlanding.ui.components.Btn
import app.softlanding.ui.components.Card
import app.softlanding.ui.components.Keypad
import app.softlanding.ui.components.Kind
import app.softlanding.ui.components.Round
import app.softlanding.ui.components.Sheet
import app.softlanding.ui.components.Typed
import app.softlanding.ui.components.kcal
import app.softlanding.ui.components.one
import app.softlanding.ui.theme.C
import app.softlanding.ui.theme.T

/** The morning after: how yesterday's flight ended, the stamp, and what the village gave. */
@Composable
fun CeremonySheet(h: Home, flights: List<Flight>, nav: Nav) {
    val c = LocalContext.current.container
    val f = flights.last()
    fun done() = c.write { settings.update { it.copy(seen = maxOf(it.seen, f.day)) } }
    val softs = flights.filter { it.landing == Landing.SOFT }
    val before = h.journey.soft - softs.size
    val newEnvelope = Envelopes.unlocked(h.journey.soft) > Envelopes.unlocked(before)
    Sheet(::done, stringResource(if (flights.size == 1) R.string.cer_title else R.string.cer_title_n)) {
        Box(Modifier.fillMaxWidth().height(170.dp).clip(Round)) {
            FlightScene(SceneState(0f, 1f, f.landing, h.s.envelope, Route.season(h.lap(f.at)), 19), Modifier.fillMaxWidth().height(170.dp))
        }
        Row(Modifier.padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(landingName(f.landing)), style = T.Title)
                Text(stringResource(R.string.cer_eaten, kcal(f.eaten.kcal), kcal(f.norm.kcal.toDouble())), style = T.Small)
            }
            if (f.landing == Landing.SOFT) Stamp(h.village(f.at), f.wind, Modifier.size(78.dp), h.ru)
        }
        Text(Logbook.line(f, h.village(f.at).name(h.ru), h.ru), style = T.Log, modifier = Modifier.padding(top = 8.dp))
        if (flights.size > 1) Text(stringResource(R.string.cer_more, flights.size - 1, softs.size), style = T.Small, modifier = Modifier.padding(top = 6.dp))
        if (softs.isNotEmpty()) Card(Modifier.fillMaxWidth().padding(top = 12.dp), color = C.goldL, border = C.goldL, onClick = { done(); nav.go(Overlay.Village(softs.last().at)) }) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.MenuBook, null, tint = C.goldD)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.cer_recipe, h.village(softs.last().at).name(h.ru)), style = T.Strong)
                    Text(h.village(softs.last().at).recipe(h.ru), style = T.Small)
                }
            }
        }
        if (newEnvelope) Row(Modifier.padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Palette, null, tint = C.red, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.cer_envelope), style = T.Body)
        }
        if (f.wind) Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Air, null, tint = C.skyD, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.cer_wind), style = T.Body)
        }
        if (f.landing == Landing.EARLY || f.landing == Landing.DRIFT) Text(stringResource(if (f.landing == Landing.EARLY) R.string.cer_early_tip else R.string.cer_drift_tip),
            style = T.Small, modifier = Modifier.padding(top = 10.dp))
        Btn(stringResource(R.string.cer_next), ::done, Modifier.fillMaxWidth().padding(top = 16.dp))
    }
}

@Composable
fun WeighSheet(h: Home, nav: Nav) {
    val c = LocalContext.current.container
    var v by remember { mutableStateOf(one(h.lastWeigh?.kg ?: h.s.profile.weight)) }
    val kg = v.toDoubleOrNull()
    Sheet(nav::close, stringResource(R.string.weigh_title)) {
        Text(stringResource(R.string.weigh_sheet_hint), style = T.Small)
        Row(Modifier.fillMaxWidth().padding(vertical = 14.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.Bottom) {
            Text(v.replace('.', ',').ifEmpty { "0" }, style = T.Hero)
            Text(" " + stringResource(R.string.kg), style = T.Heading.copy(color = C.muted))
        }
        h.lastWeigh?.let { w ->
            if (kg != null) {
                val d = kg - w.kg
                Text(stringResource(R.string.weigh_delta, (if (d > 0) "+" else "") + one(d)), style = T.Small, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            }
        }
        var fresh by remember { mutableStateOf(true) }
        Keypad({ k -> v = Typed.press(if (fresh && k != '<') "" else v, k, 3, 1); fresh = false }, Modifier.padding(top = 10.dp))
        Btn(stringResource(R.string.save), {
            if (kg != null && kg in 25.0..350.0) { c.write { weigh(h.today, kg) }; nav.close() }
        }, Modifier.fillMaxWidth().padding(top = 14.dp), enabled = kg != null && kg in 25.0..350.0)
    }
}

