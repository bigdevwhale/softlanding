package app.softlanding.ui.dish

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material.icons.rounded.Search
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.softlanding.R
import app.softlanding.container
import app.softlanding.data.Food
import app.softlanding.data.Nutr
import app.softlanding.data.SharedDish
import app.softlanding.data.Source
import app.softlanding.domain.Search
import app.softlanding.ui.Home
import app.softlanding.ui.Nav
import app.softlanding.ui.Overlay
import app.softlanding.ui.add.FoodRow
import app.softlanding.ui.add.NumField
import app.softlanding.ui.add.defaultAmount
import app.softlanding.ui.components.Btn
import app.softlanding.ui.components.Card
import app.softlanding.ui.components.Chip
import app.softlanding.ui.components.Field
import app.softlanding.ui.components.Flow
import app.softlanding.ui.components.IconBtn
import app.softlanding.ui.components.Page
import app.softlanding.ui.components.Round
import app.softlanding.ui.components.Row2
import app.softlanding.ui.components.Section
import app.softlanding.ui.components.Sheet
import app.softlanding.ui.components.grams
import app.softlanding.ui.components.kcal
import app.softlanding.ui.components.one
import app.softlanding.ui.theme.C
import app.softlanding.ui.theme.T
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.launch

private fun num(s: String) = s.replace(',', '.').toDoubleOrNull()

/** A dish from what went into the pot; weigh the finished pot once and every serving after is simple. */
@Composable
fun DishEditor(h: Home, o: Overlay.Dish, nav: Nav) {
    val ctx = LocalContext.current
    val c = ctx.container
    val scope = rememberCoroutineScope()
    val d0 = h.byId[o.id]
    var name by remember { mutableStateOf(d0?.title(h.ru).orEmpty()) }
    val items = remember { mutableStateListOf<Pair<Food, String>>().apply { addAll(h.parts(o.id).map { it.first to grams(it.second) }) } }
    val raw0 = h.parts(o.id).sumOf { it.second }
    var cooked by remember { mutableStateOf(d0?.cooked?.takeIf { it > 0 && kotlin.math.abs(it - raw0) > .5 }?.let { grams(it) }.orEmpty()) }
    var pot by remember { mutableStateOf(0.0) }
    var servings by remember { mutableStateOf(d0?.servings?.takeIf { it > 0 }?.toString().orEmpty()) }
    var q by remember { mutableStateOf("") }

    val parts = items.mapNotNull { (f, g) -> num(g)?.let { f to it } }
    val total = Nutr.sum(parts.map { (f, g) -> f.per(g) })
    val raw = parts.sumOf { it.second }
    val weighed = num(cooked)?.minus(pot)?.takeIf { it > 0 }
    val finished = weighed ?: raw
    val per100 = if (finished > 0) total * (100 / finished) else Nutr.ZERO
    val sv = servings.toIntOrNull()?.takeIf { it > 0 }

    fun save() {
        val base = d0 ?: Food(name = "", kcal = 0.0, p = 0.0, f = 0.0, c = 0.0, source = Source.DISH)
        val dish = base.copy(name = name.trim(), cooked = weighed ?: 0.0, servings = sv ?: 0)
        scope.launch {
            val id = c.saveDish(dish, parts)
            val then = o.then
            val saved = c.foods.get(id)
            if (then != null && saved != null) nav.go(Overlay.Amount(saved, then.first, then.second)) else nav.close()
        }
    }

    Page(stringResource(if (d0 == null) R.string.new_dish else R.string.edit_dish), nav::close, actions = {
        if (d0 != null) {
            IconBtn(Icons.Rounded.QrCode2, stringResource(R.string.share_qr), { nav.go(Overlay.Qr(d0.id)) }, bg = Color.Transparent)
            IconBtn(Icons.Rounded.DeleteOutline, stringResource(R.string.delete), { c.write { foods.update(d0.copy(hidden = true)) }; nav.close() }, tint = C.red, bg = Color.Transparent)
        }
    }, bottom = {
        Btn(stringResource(R.string.save), ::save, Modifier.fillMaxWidth(), enabled = name.isNotBlank() && parts.isNotEmpty())
    }) {
        Field(name, { name = it }, stringResource(R.string.dish_name_hint), Modifier.fillMaxWidth().padding(top = 6.dp))
        d0?.village?.let { v -> Text(stringResource(R.string.dish_from_village, h.villages.firstOrNull { it.key == v }?.name(h.ru).orEmpty()), style = T.Small, modifier = Modifier.padding(top = 6.dp)) }

        Section(stringResource(R.string.dish_ingredients))
        items.forEachIndexed { i, (f, g) ->
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(Modifier.weight(1f)) {
                    Text(f.title(h.ru), style = T.Body, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(kcal(f.per(num(g) ?: 0.0).kcal) + " " + stringResource(R.string.kcal), style = T.Small)
                }
                Field(g, { s -> items[i] = f to s.filter { it.isDigit() || it == ',' || it == '.' } }, "0", Modifier.width(92.dp), number = true,
                    trail = { Text(stringResource(if (f.ml) R.string.ml else R.string.g), style = T.Small) })
                IconBtn(Icons.Rounded.Close, stringResource(R.string.delete), { items.removeAt(i) }, bg = Color.Transparent, tint = C.muted, size = 36.dp)
            }
        }
        Field(q, { q = it }, stringResource(R.string.dish_add_hint), Modifier.fillMaxWidth().padding(top = 8.dp), lead = Icons.Rounded.Search)
        if (q.isNotBlank()) Card(Modifier.fillMaxWidth().padding(top = 6.dp), pad = 0.dp) {
            val found = Search.find(h.foods.filter { it.id != o.id }, q, h.now, 6)
            if (found.isEmpty()) Text(stringResource(R.string.nothing_found), style = T.Small, modifier = Modifier.padding(16.dp))
            for (f in found) FoodRow(f, h.ru, { items += f to grams(defaultAmount(f, h.ru).first); q = "" }, null)
        }

        Section(stringResource(R.string.dish_finished))
        Text(stringResource(R.string.dish_finished_hint), style = T.Small)
        Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NumField(cooked, { cooked = it }, stringResource(R.string.dish_weight), Modifier.weight(1f))
            NumField(servings, { servings = it.filter(Char::isDigit).take(2) }, stringResource(R.string.dish_servings), Modifier.weight(1f))
        }
        if (h.plates.isNotEmpty()) Flow(Modifier.padding(top = 8.dp)) {
            for (p in h.plates) Chip(stringResource(R.string.minus_plate, p.name, grams(p.grams)), pot == p.grams, { pot = if (pot == p.grams) 0.0 else p.grams }, color = C.skyD)
        }

        Card(Modifier.fillMaxWidth().padding(top = 14.dp), color = C.meadowL, border = C.meadowL) {
            Row2(stringResource(R.string.dish_whole, grams(finished)), kcal(total.kcal) + " " + stringResource(R.string.kcal), strong = true)
            Row2(stringResource(R.string.per_100), kcal(per100.kcal) + " " + stringResource(R.string.kcal) + " · " + stringResource(R.string.pfc_short, one(per100.p), one(per100.f), one(per100.c)))
            if (sv != null && finished > 0) Row2(stringResource(R.string.dish_per_serving, grams(finished / sv)), kcal(total.kcal / sv) + " " + stringResource(R.string.kcal))
            if (weighed == null && parts.isNotEmpty()) Text(stringResource(R.string.dish_raw_note), style = T.Small, modifier = Modifier.padding(top = 6.dp))
        }
    }
}

/** A dish as a QR code for another phone with Soft Landing: they scan it from the add screen. */
@Composable
fun QrSheet(d: Food, nav: Nav) {
    val data = remember(d) { SharedDish.encode(d) }
    val m = remember(data) { QRCodeWriter().encode(data, BarcodeFormat.QR_CODE, 0, 0, mapOf(EncodeHintType.MARGIN to 1, EncodeHintType.CHARACTER_SET to "UTF-8")) }
    Sheet(nav::close, d.name) {
        Text(stringResource(R.string.qr_hint), style = T.Small)
        Box(Modifier.fillMaxWidth().padding(vertical = 16.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxWidth(.75f).aspectRatio(1f).background(Color.White, Round).padding(14.dp)) {
                val cell = size.width / m.width
                for (y in 0 until m.height) for (x in 0 until m.width) if (m.get(x, y)) drawRect(C.ink, Offset(x * cell, y * cell), Size(cell + .5f, cell + .5f))
            }
        }
        Text(stringResource(R.string.qr_values, kcal(d.kcal), one(d.p), one(d.f), one(d.c)), style = T.Small, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
    }
}
