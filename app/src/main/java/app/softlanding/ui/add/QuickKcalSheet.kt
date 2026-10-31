package app.softlanding.ui.add

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.softlanding.R
import app.softlanding.container
import app.softlanding.data.Meal
import app.softlanding.ui.Home
import app.softlanding.ui.Nav
import app.softlanding.ui.Overlay
import app.softlanding.ui.components.Btn
import app.softlanding.ui.components.Chip
import app.softlanding.ui.components.Field
import app.softlanding.ui.components.Keypad
import app.softlanding.ui.components.Sheet
import app.softlanding.ui.components.Typed
import app.softlanding.ui.flight.mealName
import app.softlanding.ui.theme.C
import app.softlanding.ui.theme.T

/** Just a number: a café, a guest table, no time. */
@Composable
fun QuickKcalSheet(h: Home, o: Overlay.Quick, nav: Nav) {
    val ctx = LocalContext.current
    val c = ctx.container
    var v by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var meal by remember { mutableStateOf(o.meal) }
    val k = v.toDoubleOrNull() ?: 0.0
    Sheet(nav::close, stringResource(R.string.quick_title)) {
        Text(stringResource(R.string.quick_hint), style = T.Small)
        Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.Bottom) {
            Text(v.ifEmpty { "0" }, style = T.Hero)
            Text(" " + stringResource(R.string.kcal), style = T.Heading.copy(color = C.muted))
        }
        Field(name, { name = it }, stringResource(R.string.quick_name_hint), Modifier.fillMaxWidth())
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            for (m in Meal.entries) Chip(stringResource(mealName(m)), m == meal, { meal = m })
        }
        Keypad({ key -> v = Typed.press(v, key, 4, 0) }, Modifier.padding(top = 12.dp), dot = false)
        Btn(stringResource(R.string.add), {
            c.write { quick(k, name.trim().ifEmpty { ctx.getString(R.string.quick_entry) }, meal, o.day) }
            nav.close()
        }, Modifier.fillMaxWidth().padding(top = 14.dp), enabled = k > 0)
    }
}
