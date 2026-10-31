package app.softlanding.ui.add

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DocumentScanner
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.OutdoorGrill
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.softlanding.R
import app.softlanding.container
import app.softlanding.data.Codes
import app.softlanding.data.Food
import app.softlanding.data.Meal
import app.softlanding.data.Off
import app.softlanding.data.Portion
import app.softlanding.data.SharedDish
import app.softlanding.domain.Amount
import app.softlanding.domain.Label
import app.softlanding.domain.QuickLine
import app.softlanding.domain.Search
import app.softlanding.ui.Home
import app.softlanding.ui.Nav
import app.softlanding.ui.Overlay
import app.softlanding.ui.components.Btn
import app.softlanding.ui.components.Card
import app.softlanding.ui.components.Chip
import app.softlanding.ui.components.Field
import app.softlanding.ui.components.IconBtn
import app.softlanding.ui.components.LocalBus
import app.softlanding.ui.components.RoundS
import app.softlanding.ui.components.Section
import app.softlanding.ui.components.grams
import app.softlanding.ui.components.kcal
import app.softlanding.ui.flight.mealName
import app.softlanding.ui.theme.C
import app.softlanding.ui.theme.T
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** The amount a food goes in with when added in one tap, and how to say it. */
fun defaultAmount(f: Food, ru: Boolean): Pair<Double, String> {
    if (f.lastGrams > 0) return f.lastGrams to ""
    val p = Portion.parse(f.portions).firstOrNull()
    if (p != null) return p.grams to "1 ${p.label(ru)}"
    if (f.isDish && f.servings > 0 && f.cooked > 0) return f.cooked / f.servings to ""
    return 100.0 to ""
}

@Composable
fun AddPage(h: Home, o: Overlay.Add, nav: Nav) {
    val ctx = LocalContext.current
    val c = ctx.container
    val bus = LocalBus.current
    val scope = rememberCoroutineScope()
    val keyboard = LocalSoftwareKeyboardController.current
    var meal by remember { mutableStateOf(o.meal) }
    var q by remember { mutableStateOf("") }
    var online by remember { mutableStateOf<List<Food>?>(null) }
    var loading by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf<String?>(null) }
    val focus = remember { FocusRequester() }
    val day = o.day

    fun amount(f: Food) = nav.go(Overlay.Amount(f, meal, day, back = Overlay.Add(meal, day)))

    fun quickAdd(f: Food) {
        val (g, how) = defaultAmount(f, h.ru)
        c.write { eat(keep(f), g, meal, day, how) }
    }

    /** What a scanned code turned out to be. */
    fun code(raw: String) {
        if (Codes.isDish(raw)) {
            val d = SharedDish.decode(raw) ?: return bus.say(ctx.getString(R.string.scan_bad))
            scope.launch {
                val id = c.foods.insert(d)
                bus.say(ctx.getString(R.string.dish_imported, d.name))
                c.foods.get(id)?.let { amount(it) }
            }
            return
        }
        val gtin = Codes.gtin(raw) ?: return bus.say(ctx.getString(R.string.scan_bad))
        busy = ctx.getString(R.string.scan_looking)
        scope.launch {
            val f = c.byBarcode(gtin, h.ru)
            busy = null
            if (f != null) amount(f)
            else {
                bus.say(ctx.getString(if (h.s.online) R.string.scan_unknown else R.string.scan_unknown_offline))
                nav.go(Overlay.FoodEdit(null, barcode = gtin, then = meal to day))
            }
        }
    }

    fun scan() = Scanner.scan(ctx, ::code) { bus.say(ctx.getString(R.string.scan_fail)) }

    // A label photographed: the numbers are read on the phone and a new food opens with them filled in.
    val shot = remember { Scanner.shotUri(ctx) }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        if (ok) {
            busy = ctx.getString(R.string.label_reading)
            scope.launch {
                val text = runCatching { Scanner.text(ctx, shot) }.getOrDefault("")
                busy = null
                val g = Label.read(text)
                if (!g.any) bus.say(ctx.getString(R.string.label_none))
                nav.go(Overlay.FoodEdit(null, guess = g, name = q, then = meal to day))
            }
        }
    }

    // The scanner needs the page on screen first; started during the first frame it quietly does nothing.
    LaunchedEffect(o.scan) { if (o.scan) { delay(350); scan() } else { delay(250); runCatching { focus.requestFocus() } } }
    LaunchedEffect(q) { online = null }

    BackHandler(onBack = nav::close)
    Column(Modifier.fillMaxSize().background(C.paper).clickable(remember { MutableInteractionSource() }, null) {}.imePadding()) {
        // Search on top, the meal under it.
        Row(Modifier.fillMaxWidth().statusBarsPadding().padding(start = 4.dp, end = 12.dp, top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            IconBtn(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.back), nav::close, bg = Color.Transparent)
            Field(q, { q = it }, stringResource(R.string.search_hint), Modifier.weight(1f), lead = Icons.Rounded.Search, focus = focus,
                onDone = { keyboard?.hide() }, trail = { if (q.isNotEmpty()) IconBtn(Icons.Rounded.Close, stringResource(R.string.clear), { q = "" }, bg = Color.Transparent, size = 36.dp) })
        }
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            for (m in Meal.entries) Chip(stringResource(mealName(m)), m == meal, { meal = m })
        }

        LazyColumn(Modifier.weight(1f).fillMaxWidth()) {
            if (q.isBlank()) {
                item {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Tool(Icons.Rounded.QrCodeScanner, stringResource(R.string.tool_scan), Modifier.weight(1f), ::scan)
                        Tool(Icons.Rounded.DocumentScanner, stringResource(R.string.tool_label), Modifier.weight(1f)) { runCatching { camera.launch(shot) }.onFailure { bus.say(ctx.getString(R.string.no_camera)) } }
                        Tool(Icons.Rounded.Bolt, stringResource(R.string.tool_kcal), Modifier.weight(1f)) { nav.go(Overlay.Quick(meal, day)) }
                        Tool(Icons.Rounded.OutdoorGrill, stringResource(R.string.tool_dish), Modifier.weight(1f)) { nav.go(Overlay.Dish(0, then = meal to day)) }
                    }
                }
                val y = h.meal(day - 1, meal)
                if (y.isNotEmpty() && h.meal(day, meal).isEmpty()) item {
                    Card(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 14.dp), onClick = { c.write { copy(y, meal, day) }; nav.close() }, color = C.skyL, border = C.skyL) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.ContentCopy, null, tint = C.skyD)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(stringResource(R.string.same_as_yesterday), style = T.Strong)
                                Text(y.joinToString(", ") { it.name }, style = T.Small, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            }
                            Text(kcal(y.sumOf { it.kcal }), style = T.Number)
                        }
                    }
                }
                val usual = Search.usual(h.byId, h.entries, meal, h.today)
                item { Section(stringResource(if (usual.isEmpty()) R.string.popular else R.string.usual_for, stringResource(mealName(meal)).lowercase()), Modifier.padding(horizontal = 16.dp)) }
                val list = usual.ifEmpty { starters(h, meal) }
                items(list, key = { "u" + it.id }) { f -> FoodRow(f, h.ru, { amount(f) }, { quickAdd(f) }) }
                item {
                    Text(stringResource(R.string.line_tip), style = T.Small, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(24.dp))
                }
            } else {
                // A line of several things: «гречка 200, курица 150, 2 яйца».
                val bits = if (QuickLine.looksLikeList(q)) QuickLine.parse(q) else emptyList()
                val matched = bits.map { b -> b to (if (b.unit == Amount.KCAL && b.text.isBlank()) null else Search.find(h.foods, b.text, h.now, 1).firstOrNull()) }
                if (bits.size > 1 || bits.any { it.amount != null }) item {
                    LineCard(h, matched) { xs ->
                        c.write {
                            val kept = xs.map { (f, g, how) -> Triple(keep(f), g, how) }
                            eatAll(kept, meal, day)
                            matched.filter { it.second == null && it.first.unit == Amount.KCAL }.forEach { (b, _) -> quick(b.amount ?: 0.0, ctx.getString(R.string.quick_entry), meal, day) }
                        }
                        q = ""
                        keyboard?.hide()
                    }
                }
                val results = Search.find(h.foods, bits.singleOrNull()?.text?.takeIf { it.isNotBlank() && bits.size == 1 } ?: q, h.now)
                if (results.isNotEmpty()) item { Section(stringResource(R.string.found), Modifier.padding(horizontal = 16.dp)) }
                items(results, key = { "r" + it.id }) { f -> FoodRow(f, h.ru, { amount(f) }, { quickAdd(f) }) }
                online?.let { xs ->
                    item { Section(stringResource(R.string.found_online), Modifier.padding(horizontal = 16.dp)) }
                    if (xs.isEmpty()) item { Text(stringResource(R.string.online_none), style = T.Small, modifier = Modifier.padding(horizontal = 16.dp)) }
                    items(xs, key = { "o" + (it.barcode ?: it.name) + it.kcal }) { f -> FoodRow(f, h.ru, { scope.launch { amount(c.keep(f)) } }, { quickAdd(f) }) }
                }
                item {
                    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (online == null && h.s.online) Btn(if (loading) stringResource(R.string.searching) else stringResource(R.string.search_online), {
                            loading = true
                            scope.launch {
                                online = withContext(Dispatchers.IO) { runCatching { Off.search(q, h.ru) }.getOrNull() } ?: emptyList<Food>().also { bus.say(ctx.getString(R.string.online_fail)) }
                                loading = false
                            }
                        }, Modifier.fillMaxWidth(), kind = app.softlanding.ui.components.Kind.SECONDARY, icon = Icons.Rounded.Language, enabled = !loading)
                        if (bits.size <= 1) Btn(stringResource(R.string.create_named, q.trim()), { nav.go(Overlay.FoodEdit(null, name = q.trim(), then = meal to day)) },
                            Modifier.fillMaxWidth(), kind = app.softlanding.ui.components.Kind.GHOST, icon = Icons.Rounded.Add)
                    }
                }
            }
            item { Spacer(Modifier.height(90.dp)) }
        }

        // What this meal has so far, and the way out.
        val so = h.meal(day, meal)
        if (so.isNotEmpty()) Row(
            Modifier.fillMaxWidth().background(C.card).navigationBarsPadding().padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(mealName(meal)) + " · " + stringResource(R.string.kcal_n, kcal(so.sumOf { it.kcal })), style = T.Strong)
                Text(so.joinToString(", ") { it.name }, style = T.Small, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Btn(stringResource(R.string.done), nav::close, small = true, kind = app.softlanding.ui.components.Kind.DARK)
        } else Spacer(Modifier.navigationBarsPadding())
    }

    busy?.let { msg ->
        Box(Modifier.fillMaxSize().background(C.scrim).clickable(remember { MutableInteractionSource() }, null) {}, contentAlignment = Alignment.Center) {
            Column(Modifier.background(C.card, RoundS).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = C.red)
                Text(msg, style = T.Body, modifier = Modifier.padding(top = 12.dp))
            }
        }
    }
}

/** First foods to offer before there are habits: by the meal. */
private fun starters(h: Home, m: Meal): List<Food> {
    val keys = when (m) {
        Meal.BREAKFAST -> listOf("oatmeal_m", "egg", "tvorog_5", "yogurt_greek", "coffee_black", "cappuccino", "banana", "bread_wg", "syrniki", "omelette")
        Meal.LUNCH -> listOf("borscht", "chicken_soup", "buckwheat_c", "chicken_breast", "rice_c", "veg_salad", "pelmeni", "potato_mash", "cutlet", "bread_rye")
        Meal.DINNER -> listOf("salmon", "chicken_breast_g", "buckwheat_c", "veg_salad", "pasta_c", "tvorog_5", "omelette", "cod", "potato_baked", "greek_salad")
        Meal.SNACK -> listOf("apple", "banana", "kefir_1", "almonds", "yogurt_plain", "chocolate_dark", "cookie", "tangerine", "tea", "protein_bar")
    }
    val byKey = h.foods.associateBy { it.key }
    return keys.mapNotNull { byKey[it] }
}

@Composable
private fun Tool(icon: ImageVector, label: String, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier.clip(RoundS).background(C.card).border(1.dp, C.line, RoundS).clickable(onClick = onClick).padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, null, tint = C.red, modifier = Modifier.size(24.dp))
        Text(label, style = T.Small.copy(color = C.ink), modifier = Modifier.padding(top = 6.dp), maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun FoodRow(f: Food, ru: Boolean, onClick: () -> Unit, onQuick: (() -> Unit)?) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(start = 16.dp, end = 10.dp, top = 9.dp, bottom = 9.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (f.fav) { Icon(Icons.Rounded.Star, null, tint = C.gold, modifier = Modifier.size(14.dp)); Spacer(Modifier.width(4.dp)) }
                Text(f.title(ru), style = T.Body, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Text(subtitle(f, ru), style = T.Small, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (onQuick != null) {
            val (g, _) = defaultAmount(f, ru)
            Column(horizontalAlignment = Alignment.End, modifier = Modifier.padding(end = 8.dp)) {
                Text(kcal(f.per(g).kcal), style = T.Number)
                Text(grams(g) + " " + stringResource(if (f.ml) R.string.ml else R.string.g), style = T.Small)
            }
            Box(Modifier.size(38.dp).clip(CircleShape).background(C.redL).clickable(onClick = onQuick), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Add, stringResource(R.string.add_quick, f.title(ru)), tint = C.red, modifier = Modifier.size(22.dp))
            }
        }
    }
}

@Composable
private fun subtitle(f: Food, ru: Boolean): String {
    val per = stringResource(if (f.ml) R.string.per_100ml else R.string.per_100g, kcal(f.kcal))
    val tag = when {
        f.isDish -> stringResource(R.string.tag_dish)
        f.brand.isNotBlank() -> f.brand
        else -> null
    }
    return listOfNotNull(tag, per).joinToString(" · ")
}

/** The quick line, piece by piece, with one button for all of it. */
@Composable
private fun LineCard(h: Home, matched: List<Pair<app.softlanding.domain.Bit, Food?>>, onAll: (List<Triple<Food, Double, String>>) -> Unit) {
    val xs = matched.mapNotNull { (b, f) -> f?.let { food ->
        val g = QuickLine.grams(b, food)
        val how = when (b.unit) {
            Amount.PIECES -> Portion.parse(food.portions).firstOrNull()?.let { p -> "${grams(b.amount ?: 1.0)} ${p.label(h.ru)}" } ?: ""
            else -> ""
        }
        Triple(food, g, how)
    } }
    val kcalOnly = matched.filter { it.second == null && it.first.unit == Amount.KCAL }.sumOf { it.first.amount ?: 0.0 }
    val total = xs.sumOf { it.first.per(it.second).kcal } + kcalOnly
    Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp), color = C.goldL, border = C.goldL) {
        Text(stringResource(R.string.line_title), style = T.Tiny.copy(color = C.goldD))
        for ((b, f) in matched) Row(Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(f?.title(h.ru) ?: if (b.unit == Amount.KCAL) stringResource(R.string.quick_entry) else stringResource(R.string.line_unknown, b.text), style = T.Body.copy(color = if (f == null && b.unit != Amount.KCAL) C.muted else C.ink), maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (f != null) Text(grams(QuickLine.grams(b, f)) + " " + stringResource(if (f.ml) R.string.ml else R.string.g), style = T.Small)
            }
            Text(f?.let { kcal(it.per(QuickLine.grams(b, it)).kcal) } ?: if (b.unit == Amount.KCAL) kcal(b.amount ?: 0.0) else "—", style = T.Number)
        }
        Btn(stringResource(R.string.line_add, kcal(total)), { onAll(xs) }, Modifier.fillMaxWidth().padding(top = 12.dp), enabled = xs.isNotEmpty() || kcalOnly > 0)
    }
}
