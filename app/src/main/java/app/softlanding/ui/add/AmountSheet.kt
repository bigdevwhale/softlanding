package app.softlanding.ui.add

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
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
import app.softlanding.ui.components.Pill
import app.softlanding.ui.components.Sheet
import app.softlanding.ui.components.Typed
import app.softlanding.ui.components.grams
import app.softlanding.ui.components.kcal
import app.softlanding.ui.components.one
import app.softlanding.ui.flight.mealName
import app.softlanding.ui.theme.C
import app.softlanding.ui.theme.T

/**
 * How much. One big number, one row of units under it, a strip showing where this puts the day, one button.
 * Units are grams or a count of a measure (piece, serving, pack); a plate on the scales is just another chip.
 */
@Composable
fun AmountSheet(h: Home, o: Overlay.Amount, nav: Nav) {
    val c = LocalContext.current.container
    val food = h.byId[o.food.id] ?: o.food
    val ru = h.ru
    val unitG = stringResource(if (food.ml) R.string.ml else R.string.g)
    val serving = if (food.isDish && food.servings > 0 && food.cooked > 0) food.cooked / food.servings else 0.0
    val measures = remember(food) {
        buildList {
            if (serving > 0) add(Portion("serving", serving))
            addAll(Portion.parse(food.portions).filter { !(food.isDish && it.unit == "serving") })
        }
    }
    val editing = o.entry > 0
    val start = remember {
        when {
            o.grams > 0 -> -1 to grams(o.grams)
            food.lastGrams > 0 -> -1 to grams(food.lastGrams)
            // A serving first: a spoon of sauce, not the whole jar.
            measures.isNotEmpty() -> maxOf(0, measures.indexOfFirst { it.unit.equals("serving", true) }) to "1"
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
    fun label(p: Portion) = if (p.unit.equals("serving", true)) servingLabel else p.label(ru)
    val how = if (measure >= 0) "${one(n)} ${label(measures[measure])}" else ""

    fun add(close: Boolean) {
        c.write { eat(keep(food), g, meal, o.day, how) }
        nav.go(if (close) null else o.back)
    }

    Sheet(nav::close) {
        // Name, brand, and the meal it goes into.
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text(food.title(ru), style = T.Heading, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(listOfNotNull(food.brand.takeIf { it.isNotBlank() }, stringResource(if (food.ml) R.string.per_100ml else R.string.per_100g, kcal(food.kcal)))
                    .joinToString(" · "), style = T.Small, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (food.id > 0) {
                IconBtn(if (food.fav) Icons.Rounded.Star else Icons.Rounded.StarBorder, stringResource(R.string.favourite), { c.write { foods.update(food.copy(fav = !food.fav)) } },
                    tint = if (food.fav) C.gold else C.faint, bg = Color.Transparent, size = 40.dp)
                IconBtn(Icons.Rounded.Edit, stringResource(R.string.edit_food), {
                    nav.go(if (food.isDish) Overlay.Dish(food.id) else Overlay.FoodEdit(food))
                }, tint = C.faint, bg = Color.Transparent, size = 40.dp)
            }
            IconBtn(Icons.Rounded.Close, stringResource(R.string.close), nav::close, bg = Color.Transparent, size = 40.dp)
        }

        // The amount, big, with its unit; the calories right under it.
        Column(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(v.replace('.', ',').ifEmpty { "0" }, style = T.Hero)
                Text(" " + if (measure >= 0) label(measures[measure]) else unitG, style = T.Heading.copy(color = C.muted), modifier = Modifier.padding(bottom = 6.dp))
            }
            Text(
                buildString {
                    append(kcal(nut.kcal)); append(" "); append(stringResource(R.string.kcal))
                    if (measure >= 0 || plateG > 0) { append("  ·  "); append(grams(g)); append(" "); append(unitG) }
                },
                style = T.Strong.copy(color = C.ink),
            )
            Text(stringResource(R.string.pfc_short, one(nut.p), one(nut.f), one(nut.c)), style = T.Small)
        }

        // Units: grams, the food's own measures, a plate to take off.
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Chip(unitG, measure < 0, { if (measure >= 0) { v = grams(raw); measure = -1; fresh = true } })
            measures.forEachIndexed { i, p ->
                Chip("${label(p)} ${grams(p.grams)} $unitG", measure == i, { v = "1"; measure = i; fresh = true; plate = 0 })
            }
            // Without its own measures, a food gets the eye ones.
            if (measures.isEmpty()) for ((l, gr) in listOf(R.string.eye_palm to 100, R.string.eye_fist to 150, R.string.eye_handful to 30)) {
                Chip("${stringResource(l)} ≈ $gr $unitG", false, { v = gr.toString(); measure = -1; fresh = true; plate = 0 }, color = C.inkSoft)
            }
            if (measure < 0) for (p in h.plates) Chip(stringResource(R.string.minus_plate, p.name, grams(p.grams)), plate == p.id, { plate = if (plate == p.id) 0 else p.id }, color = C.skyD)
        }

        DayStrip(before, nut.kcal, norm.lo.toDouble(), norm.hi.toDouble(), Modifier.padding(top = 16.dp))
        Text(
            if (after >= 0) stringResource(R.string.after_left, kcal(after)) else stringResource(R.string.after_over, kcal(-after)),
            style = T.Small.copy(color = if (before + nut.kcal > norm.hi) C.redD else C.inkSoft), modifier = Modifier.padding(top = 6.dp),
        )

        Keypad({ k ->
            v = Typed.press(if (fresh && k != '<') "" else v, k, 4, if (measure >= 0) 2 else 1)
            fresh = false
        }, Modifier.padding(top = 14.dp))

        Row(Modifier.fillMaxWidth().padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            MealPicker(meal) { meal = it }
            if (editing) {
                IconBtn(Icons.Rounded.DeleteOutline, stringResource(R.string.delete), {
                    c.write { entries.get(o.entry)?.let { entries.delete(it) } }
                    nav.close()
                }, tint = C.red, bg = C.redL, size = 52.dp)
                Btn(stringResource(R.string.save), {
                    c.write { entries.get(o.entry)?.let { regram(it, g, meal, how) } }
                    nav.close()
                }, Modifier.weight(1f), enabled = g > 0)
            } else Btn(stringResource(R.string.add_n_kcal, kcal(nut.kcal)), { add(close = o.back == null) }, Modifier.weight(1f), enabled = g > 0)
        }
    }
}

/** The meal as one small button with a menu, instead of a row of four. */
@Composable
private fun MealPicker(meal: Meal, onPick: (Meal) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        Row(
            Modifier.height(52.dp).clip(Pill).background(C.card).clickable { open = true }.padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(stringResource(mealName(meal)), style = T.Button.copy(color = C.ink))
            Spacer(Modifier.width(2.dp))
            Icon(Icons.Rounded.KeyboardArrowDown, null, tint = C.muted, modifier = Modifier.size(20.dp))
        }
        DropdownMenu(open, { open = false }) {
            for (m in Meal.entries) DropdownMenuItem({ Text(stringResource(mealName(m)), style = T.Body) }, { onPick(m); open = false })
        }
    }
}

/** The day so far, what this adds, and where the meadow lies. */
@Composable
fun DayStrip(before: Double, adding: Double, lo: Double, hi: Double, modifier: Modifier = Modifier) {
    val span = maxOf(hi * 1.12, before + adding)
    val b by animateFloatAsState((before / span).toFloat(), tween(300), label = "before")
    val a by animateFloatAsState(((before + adding) / span).toFloat(), tween(300), label = "after")
    val over = before + adding > hi
    Canvas(modifier.fillMaxWidth().height(14.dp)) {
        val r = CornerRadius(size.height / 2)
        drawRoundRect(C.line, size = size, cornerRadius = r)
        // The meadow.
        val x0 = size.width * (lo / span).toFloat()
        val x1 = size.width * (hi / span).toFloat()
        drawRect(C.meadow.copy(alpha = .45f), Offset(x0, 0f), Size(x1 - x0, size.height))
        drawRect(C.meadow.copy(alpha = .5f), Offset(x0, 0f), Size(2f, size.height))
        drawRect(C.meadow.copy(alpha = .5f), Offset(x1 - 2f, 0f), Size(2f, size.height))
        // Eaten, then this.
        drawRoundRect(C.inkSoft.copy(alpha = .55f), size = Size(size.width * b, size.height), cornerRadius = r)
        drawRoundRect(if (over) C.red else C.gold, Offset(size.width * b, 0f), Size(size.width * (a - b), size.height), r)
    }
}
