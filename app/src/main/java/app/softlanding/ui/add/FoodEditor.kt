package app.softlanding.ui.add

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.DocumentScanner
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.softlanding.R
import app.softlanding.container
import app.softlanding.data.Food
import app.softlanding.data.Portion
import app.softlanding.data.Source
import app.softlanding.domain.Label
import app.softlanding.domain.LabelGuess
import app.softlanding.ui.Home
import app.softlanding.ui.Nav
import app.softlanding.ui.Overlay
import app.softlanding.ui.components.Btn
import app.softlanding.ui.components.Card
import app.softlanding.ui.components.Chip
import app.softlanding.ui.components.Field
import app.softlanding.ui.components.Flow
import app.softlanding.ui.components.IconBtn
import app.softlanding.ui.components.Kind
import app.softlanding.ui.components.LocalBus
import app.softlanding.ui.components.Page
import app.softlanding.ui.components.Section
import app.softlanding.ui.components.grams
import app.softlanding.ui.components.kcal
import app.softlanding.ui.components.one
import app.softlanding.ui.theme.C
import app.softlanding.ui.theme.T
import kotlinx.coroutines.launch

private fun num(s: String) = s.replace(',', '.').toDoubleOrNull()

/** A food by hand: values per 100 g, filled from a photo of the label or typed. */
@Composable
fun FoodEditor(h: Home, o: Overlay.FoodEdit, nav: Nav) {
    val ctx = LocalContext.current
    val c = ctx.container
    val bus = LocalBus.current
    val scope = rememberCoroutineScope()
    val f0 = o.food
    var name by remember { mutableStateOf(f0?.title(h.ru) ?: o.name) }
    var brand by remember { mutableStateOf(f0?.brand.orEmpty()) }
    fun init(d: Double?) = d?.let { one(it).replace('.', ',') }.orEmpty()
    var kc by remember { mutableStateOf(init(f0?.kcal ?: o.guess?.kcal)) }
    var p by remember { mutableStateOf(init(f0?.p ?: o.guess?.p)) }
    var fa by remember { mutableStateOf(init(f0?.f ?: o.guess?.f)) }
    var cb by remember { mutableStateOf(init(f0?.c ?: o.guess?.c)) }
    var ml by remember { mutableStateOf(f0?.ml ?: false) }
    val portions = remember { mutableStateListOf<Pair<String, String>>().apply { addAll(Portion.parse(f0?.portions.orEmpty()).map { it.label(h.ru) to grams(it.grams) }) } }
    var reading by remember { mutableStateOf(false) }

    fun fill(g: LabelGuess) {
        g.kcal?.let { kc = init(it) }; g.p?.let { p = init(it) }; g.f?.let { fa = init(it) }; g.c?.let { cb = init(it) }
        bus.say(ctx.getString(if (g.any) R.string.label_filled else R.string.label_none))
    }

    fun read(uri: android.net.Uri) {
        reading = true
        scope.launch {
            val text = runCatching { Scanner.text(ctx, uri) }.getOrDefault("")
            reading = false
            fill(Label.read(text))
        }
    }

    val shot = remember { Scanner.shotUri(ctx) }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok -> if (ok) read(shot) }
    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri -> uri?.let(::read) }

    val k = num(kc)
    val pp = num(p) ?: 0.0
    val ff = num(fa) ?: 0.0
    val cc = num(cb) ?: 0.0
    val est = pp * 4 + ff * 9 + cc * 4
    val ok = name.isNotBlank() && (k != null || est > 0)

    fun save() {
        val kcalV = k ?: est
        val ps = portions.mapNotNull { (u, g) -> num(g)?.takeIf { it > 0 }?.let { Portion(unitKey(u), it) } }
        val base = f0 ?: Food(name = "", kcal = 0.0, p = 0.0, f = 0.0, c = 0.0, source = if (o.guess != null) Source.LABEL else if (o.barcode != null) Source.SCAN else Source.USER)
        // Renaming a built-in food in English keeps its Russian name, and the other way round.
        val named = if (base.source == Source.BUILTIN && !h.ru) base.copy(en = name.trim()) else base.copy(name = name.trim())
        val food = named.copy(brand = brand.trim(), kcal = kcalV, p = pp, f = ff, c = cc, ml = ml, portions = Portion.join(ps), barcode = base.barcode ?: o.barcode)
        scope.launch {
            val id = if (food.id > 0) { c.foods.update(food); food.id } else c.foods.insert(food)
            val saved = c.foods.get(id) ?: food
            val then = o.then
            if (then != null) nav.go(Overlay.Amount(saved, then.first, then.second)) else nav.close()
        }
    }

    Page(stringResource(if (f0 == null) R.string.new_food else R.string.edit_food), nav::close, actions = {
        if (f0 != null) IconBtn(Icons.Rounded.DeleteOutline, stringResource(R.string.delete), {
            c.write { foods.update(f0.copy(hidden = true)) }
            nav.close()
        }, tint = C.red, bg = Color.Transparent)
    }, bottom = {
        Btn(stringResource(R.string.save), ::save, Modifier.fillMaxWidth(), enabled = ok)
    }) {
        o.barcode?.let { Text(stringResource(R.string.barcode_n, it), style = T.Small, modifier = Modifier.padding(bottom = 8.dp)) }
        Card(Modifier.fillMaxWidth(), color = C.goldL, border = C.goldL) {
            Text(stringResource(R.string.label_title), style = T.Strong)
            Text(stringResource(R.string.label_hint), style = T.Small, modifier = Modifier.padding(top = 2.dp))
            Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Btn(if (reading) stringResource(R.string.label_reading) else stringResource(R.string.label_shoot), { runCatching { camera.launch(shot) }.onFailure { bus.say(ctx.getString(R.string.no_camera)) } },
                    Modifier.weight(1f), small = true, icon = Icons.Rounded.DocumentScanner, enabled = !reading)
                Btn(stringResource(R.string.label_gallery), { gallery.launch("image/*") }, small = true, kind = Kind.SECONDARY, icon = Icons.Rounded.Image, enabled = !reading)
            }
        }
        Section(stringResource(R.string.f_name))
        Field(name, { name = it }, stringResource(R.string.f_name_hint), Modifier.fillMaxWidth())
        Field(brand, { brand = it }, stringResource(R.string.f_brand_hint), Modifier.fillMaxWidth().padding(top = 8.dp))
        Section(stringResource(if (ml) R.string.f_per_100ml else R.string.f_per_100g))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NumField(kc, { kc = it }, stringResource(R.string.kcal), Modifier.weight(1f))
            NumField(p, { p = it }, stringResource(R.string.protein), Modifier.weight(1f))
        }
        Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NumField(fa, { fa = it }, stringResource(R.string.fat), Modifier.weight(1f))
            NumField(cb, { cb = it }, stringResource(R.string.carbs), Modifier.weight(1f))
        }
        if (k != null && est > 0 && !Label.fits(pp, ff, cc, k)) Text(stringResource(R.string.f_mismatch, kcal(est)), style = T.Small.copy(color = C.redD), modifier = Modifier.padding(top = 8.dp))
        if (k == null && est > 0) Text(stringResource(R.string.f_from_macros, kcal(est)), style = T.Small, modifier = Modifier.padding(top = 8.dp))
        Flow(Modifier.padding(top = 10.dp)) {
            Chip(stringResource(R.string.f_solid), !ml, { ml = false })
            Chip(stringResource(R.string.f_liquid), ml, { ml = true })
        }
        Section(stringResource(R.string.f_portions))
        Text(stringResource(R.string.f_portions_hint), style = T.Small)
        portions.forEachIndexed { i, (u, g) ->
            Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Field(u, { portions[i] = it to g }, stringResource(R.string.f_portion_name), Modifier.weight(1.4f))
                Field(g, { portions[i] = u to it }, stringResource(if (ml) R.string.ml else R.string.g), Modifier.weight(1f), number = true)
                IconBtn(Icons.Rounded.Close, stringResource(R.string.delete), { portions.removeAt(i) }, bg = Color.Transparent, tint = C.muted)
            }
        }
        Btn(stringResource(R.string.f_add_portion), { portions += (if (h.ru) "шт" else "pc") to "" }, Modifier.padding(top = 8.dp), kind = Kind.GHOST, icon = Icons.Rounded.Add, small = true)
        Column(Modifier.padding(top = 8.dp)) {}
    }
}

/** «шт» or «pc» go back to their key, so the measure reads right in either language. */
private fun unitKey(u: String): String {
    val t = u.trim()
    app.softlanding.data.Measure.entries.firstOrNull { it.ru.equals(t, true) || it.en.equals(t, true) || it.name.equals(t, true) }?.let { return it.name }
    if (t.equals("порция", true) || t.equals("serving", true)) return "serving"
    return t.ifBlank { "pc" }
}

@Composable
fun NumField(v: String, on: (String) -> Unit, label: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(label, style = T.Small, modifier = Modifier.padding(bottom = 4.dp, start = 4.dp))
        Field(v, { s -> on(s.filter { it.isDigit() || it == ',' || it == '.' }.take(7)) }, "0", Modifier.fillMaxWidth(), number = true)
    }
}
