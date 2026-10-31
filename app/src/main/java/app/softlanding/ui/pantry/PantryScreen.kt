package app.softlanding.ui.pantry

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.softlanding.R
import app.softlanding.container
import app.softlanding.data.Plate
import app.softlanding.data.Source
import app.softlanding.domain.Days
import app.softlanding.domain.Search
import app.softlanding.ui.Home
import app.softlanding.ui.Nav
import app.softlanding.ui.Overlay
import app.softlanding.ui.add.FoodRow
import app.softlanding.ui.components.Btn
import app.softlanding.ui.components.Card
import app.softlanding.ui.components.Chip
import app.softlanding.ui.components.Empty
import app.softlanding.ui.components.Field
import app.softlanding.ui.components.IconBtn
import app.softlanding.ui.components.Kind
import app.softlanding.ui.components.grams
import app.softlanding.ui.theme.C
import app.softlanding.ui.theme.T

private enum class Shelf { DISHES, MINE, FAVS, ALL, PLATES }

@Composable
fun PantryScreen(h: Home, nav: Nav) {
    val c = LocalContext.current.container
    var shelf by rememberSaveable { mutableStateOf(Shelf.DISHES) }
    var q by rememberSaveable { mutableStateOf("") }
    val meal = Search.mealAt(Days.hour(h.now))
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Text(stringResource(R.string.pantry_title), style = T.Title, modifier = Modifier.padding(start = 16.dp, top = 12.dp))
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            for ((s, l) in listOf(Shelf.DISHES to R.string.shelf_dishes, Shelf.MINE to R.string.shelf_mine, Shelf.FAVS to R.string.shelf_favs, Shelf.ALL to R.string.shelf_all, Shelf.PLATES to R.string.shelf_plates))
                Chip(stringResource(l), s == shelf, { shelf = s })
        }
        if (shelf == Shelf.PLATES) { Plates(h); return@Column }
        if (shelf == Shelf.ALL) Field(q, { q = it }, stringResource(R.string.search_hint), Modifier.fillMaxWidth().padding(horizontal = 16.dp), lead = Icons.Rounded.Search,
            trail = { if (q.isNotEmpty()) IconBtn(Icons.Rounded.Close, stringResource(R.string.clear), { q = "" }, bg = Color.Transparent, size = 36.dp) })
        val list = when (shelf) {
            Shelf.DISHES -> h.foods.filter { it.isDish }.sortedByDescending { it.lastUsed }
            Shelf.MINE -> h.foods.filter { it.source != Source.BUILTIN && !it.isDish }.sortedByDescending { it.lastUsed }
            Shelf.FAVS -> h.foods.filter { it.fav }
            else -> if (q.isBlank()) h.foods.sortedBy { it.title(h.ru) } else Search.find(h.foods, q, h.now, 80)
        }
        LazyColumn(Modifier.weight(1f)) {
            item {
                Row(Modifier.padding(horizontal = 16.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (shelf == Shelf.DISHES) Btn(stringResource(R.string.new_dish), { nav.go(Overlay.Dish(0)) }, small = true, icon = Icons.Rounded.Add)
                    else Btn(stringResource(R.string.new_food), { nav.go(Overlay.FoodEdit(null)) }, small = true, icon = Icons.Rounded.Add)
                }
            }
            if (list.isEmpty()) item {
                Empty(stringResource(when (shelf) {
                    Shelf.DISHES -> R.string.empty_dishes
                    Shelf.MINE -> R.string.empty_mine
                    Shelf.FAVS -> R.string.empty_favs
                    else -> R.string.nothing_found
                }))
            }
            items(list, key = { it.id }) { f ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        FoodRow(f, h.ru, { nav.go(if (f.isDish) Overlay.Dish(f.id) else Overlay.FoodEdit(f)) }, { nav.go(Overlay.Amount(f, meal, h.today)) })
                    }
                    if (f.isDish) IconBtn(Icons.Rounded.QrCode2, stringResource(R.string.share_qr), { nav.go(Overlay.Qr(f.id)) }, bg = Color.Transparent, tint = C.muted)
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun Plates(h: Home) {
    val c = LocalContext.current.container
    var name by rememberSaveable { mutableStateOf("") }
    var g by rememberSaveable { mutableStateOf("") }
    Column(Modifier.padding(horizontal = 16.dp)) {
        Text(stringResource(R.string.plates_hint), style = T.Small)
        for (p in h.plates) Card(Modifier.fillMaxWidth().padding(top = 8.dp), pad = 12.dp) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(p.name, style = T.Body, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(grams(p.grams) + " " + stringResource(R.string.g), style = T.Number)
                IconBtn(Icons.Rounded.Close, stringResource(R.string.delete), { c.write { misc.deletePlate(p) } }, bg = Color.Transparent, tint = C.muted, size = 36.dp)
            }
        }
        Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Field(name, { name = it }, stringResource(R.string.plate_name), Modifier.weight(1.4f))
            Field(g, { s -> g = s.filter { it.isDigit() }.take(4) }, stringResource(R.string.g), Modifier.weight(1f), number = true)
        }
        val gv = g.toDoubleOrNull()
        Btn(stringResource(R.string.plate_add), {
            if (gv != null && name.isNotBlank()) { c.write { misc.addPlate(Plate(name = name.trim(), grams = gv)) }; name = ""; g = "" }
        }, Modifier.fillMaxWidth().padding(top = 8.dp), kind = Kind.SECONDARY, enabled = gv != null && gv > 0 && name.isNotBlank())
    }
}
