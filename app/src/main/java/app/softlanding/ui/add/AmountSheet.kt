package app.softlanding.ui.add

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.softlanding.R
import app.softlanding.container
import app.softlanding.data.Meal
import app.softlanding.data.Portion
import app.softlanding.ui.Home
import app.softlanding.ui.Nav
import app.softlanding.ui.Overlay
import app.softlanding.ui.components.Btn
import app.softlanding.ui.components.Chip
import app.softlanding.ui.components.IconBtn
import app.softlanding.ui.components.Keypad
import app.softlanding.ui.components.Kind
import app.softlanding.ui.components.Sheet
import app.softlanding.ui.components.Typed
import app.softlanding.ui.components.grams
import app.softlanding.ui.components.kcal
import app.softlanding.ui.components.one
import app.softlanding.ui.flight.mealName
import app.softlanding.ui.theme.C
import app.softlanding.ui.theme.T

/** How much: grams, or a count of a measure (pieces, glasses, servings); a plate on the scales can be taken off. */
@Composable
fun AmountSheet(h: Home, o: Overlay.Amount, nav: Nav) {
    val c = LocalContext.current.container
    val food = h.byId[o.food.id] ?: o.food
    val ru = h.ru
    val unitG = stringResource(if (food.ml) R.string.ml else R.string.g)
    val serving = if (food.isDish && food.servings > 0 && food.cooked > 0) food.cooked / food.servings else 0.0
    val measures = remember(food) {
        buildList {
            addAll(Portion.parse(food.portions).filter { !(food.isDish && it.unit == "serving") })
            if (serving > 0) add(Portion("serving", serving))
        }
    }
    val editing = o.entry > 0
    val start = remember {
        when {
            o.grams > 0 -> -1 to grams(o.grams)
            food.lastGrams > 0 -> -1 to grams(food.lastGrams)
            measures.isNotEmpty() -> 0 to "1"
            else -> -1 to "100"
        }
    }
    var measure by remember { mutableIntStateOf(start.first) }
    var v by remember { mutableStateOf(start.second) }
    var fresh by remember { mutableStateOf(true) }
    var meal by remember { mutableStateOf(o.meal) }
    var plate by remember { mutableLongStateOf(0L) }
    val plateG = h.plates.firstOrNull { it.id == plate }?.grams ?: 0.0
    val n = v.replace(',', '.').toDoubleOrNull() ?: 0.0
    val raw = if (measure >= 0) n * measures[measure].grams else n
    val g = (raw - if (measure < 0) plateG else 0.0).coerceAtLeast(0.0)
    val nut = food.per(g)
    val norm = h.norm(o.day)
    val before = h.eaten(o.day).kcal - (h.entries.firstOrNull { it.id == o.entry }?.kcal ?: 0.0)
    val after = norm.kcal - before - nut.kcal

    val servingLabel = stringResource(R.string.serving)
    fun label(p: Portion) = if (p.unit == "serving") servingLabel else p.label(ru)
    val how = if (measure >= 0) "${one(n).replace('.', ',')} ${label(measures[measure])}" else ""

    Sheet(nav::close, food.title(ru)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(if (food.ml) R.string.per_100ml else R.string.per_100g, kcal(food.kcal)) + " · " +
                stringResource(R.string.pfc_short, one(food.p), one(food.f), one(food.c)), style = T.Small, modifier = Modifier.weight(1f))
            if (food.id > 0) {
                IconBtn(if (food.fav) Icons.Rounded.Star else Icons.Rounded.StarBorder, stringResource(R.string.favourite), { c.write { foods.update(food.copy(fav = !food.fav)) } },
                    tint = if (food.fav) C.gold else C.muted, bg = C.paper, size = 40.dp)
                IconBtn(Icons.Rounded.Edit, stringResource(R.string.edit_food), {
                    nav.go(if (food.isDish) Overlay.Dish(food.id) else Overlay.FoodEdit(food))
                }, tint = C.muted, bg = C.paper, size = 40.dp)
            }
        }

        // The amount, big.
        Column(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(v.replace('.', ',').ifEmpty { "0" }, style = T.Hero)
                Text(" " + if (measure >= 0) label(measures[measure]) else unitG, style = T.Heading.copy(color = C.muted))
            }
            val sub = buildList {
                if (measure >= 0) add("= ${grams(g)} $unitG")
                if (plateG > 0 && measure < 0) add(stringResource(R.string.net_of_plate, grams(g), grams(plateG)))
            }
            if (sub.isNotEmpty()) Text(sub.joinToString(" · "), style = T.Small)
            Row(Modifier.padding(top = 6.dp), verticalAlignment = Alignment.Bottom) {
                Text(kcal(nut.kcal), style = T.Title.copy(color = C.red))
                Text(" " + stringResource(R.string.kcal) + "  ·  " + stringResource(R.string.pfc_short, one(nut.p), one(nut.f), one(nut.c)), style = T.Small)
            }
            Text(
                if (after >= 0) stringResource(R.string.after_left, kcal(after)) else stringResource(R.string.after_over, kcal(-after)),
                style = T.Small.copy(color = if (before + nut.kcal > norm.hi) C.red else C.meadowD), textAlign = TextAlign.Center,
            )
        }

        // Grams or a measure.
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Chip(unitG, measure < 0, {
                if (measure >= 0) { v = grams(raw); measure = -1; fresh = true }
            })
            measures.forEachIndexed { i, p ->
                Chip("${label(p)} · ${grams(p.grams)} $unitG", measure == i, {
                    v = "1"; measure = i; fresh = true; plate = 0
                })
            }
        }
        // By eye, for when there are no scales; and plates for when there are.
        if (measure < 0) Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            for ((l, gr) in listOf(R.string.eye_palm to 100, R.string.eye_fist to 150, R.string.eye_handful to 30, R.string.eye_spoon to 15)) {
                Chip(stringResource(l), false, { v = gr.toString(); fresh = true; plate = 0 }, color = C.inkSoft)
            }
            for (p in h.plates) Chip(stringResource(R.string.minus_plate, p.name, grams(p.grams)), plate == p.id, { plate = if (plate == p.id) 0 else p.id }, color = C.skyD)
        }

        Keypad({ k ->
            v = Typed.press(if (fresh && k != '<') "" else v, k, 4, if (measure >= 0) 2 else 1)
            fresh = false
        }, Modifier.padding(top = 12.dp))

        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            for (m in Meal.entries) Chip(stringResource(mealName(m)), m == meal, { meal = m })
        }

        Row(Modifier.fillMaxWidth().padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            if (editing) {
                IconBtn(Icons.Rounded.DeleteOutline, stringResource(R.string.delete), {
                    c.write { entries.get(o.entry)?.let { entries.delete(it) } }
                    nav.close()
                }, tint = C.red, bg = C.redL, size = 52.dp)
                Btn(stringResource(R.string.save), {
                    c.write { entries.get(o.entry)?.let { regram(it, g, meal, how) } }
                    nav.close()
                }, Modifier.weight(1f), enabled = g > 0)
            } else {
                Btn(stringResource(R.string.add_n_kcal, kcal(nut.kcal)), {
                    c.write { eat(keep(food), g, meal, o.day, how) }
                    nav.go(o.back)
                }, Modifier.weight(1f), enabled = g > 0)
            }
        }
        if (!editing && o.back != null) Btn(stringResource(R.string.add_and_close), {
            c.write { eat(keep(food), g, meal, o.day, how) }
            nav.close()
        }, Modifier.fillMaxWidth().padding(top = 4.dp), kind = Kind.GHOST, small = true, enabled = g > 0)
    }
}

