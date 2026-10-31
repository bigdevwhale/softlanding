package app.softlanding.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.softlanding.R
import app.softlanding.ui.theme.C
import app.softlanding.ui.theme.T
import java.util.Locale
import kotlin.math.roundToInt

val Round = RoundedCornerShape(20.dp)
val RoundS = RoundedCornerShape(14.dp)
val Pill = RoundedCornerShape(50)

/** «1 240»: thin spaces between thousands, the way both languages read numbers. */
fun kcal(d: Double): String = String.format(Locale.ROOT, "%,d", d.roundToInt()).replace(',', ' ')

fun grams(d: Double): String = if (d >= 100 || d == Math.floor(d)) d.roundToInt().toString() else String.format(Locale.ROOT, "%.1f", d).trimEnd('0').trimEnd('.').let { if (Fmt.comma) it.replace('.', ',') else it }

/** The decimal mark of the screen's language, set by the root: «72,1» in Russian, «72.1» in English. */
object Fmt { @Volatile var comma = false }

fun one(d: Double): String = String.format(Locale.ROOT, "%.1f", d).let { if (it.endsWith(".0")) it.dropLast(2) else it }.let { if (Fmt.comma) it.replace('.', ',') else it }

/** Notes at the bottom of the screen; one may carry an action, like undo. */
class Bus {
    data class Note(val text: String, val action: String? = null, val run: (() -> Unit)? = null, val id: Long = System.nanoTime())
    val notes = mutableStateListOf<Note>()
    fun say(text: String, action: String? = null, run: (() -> Unit)? = null) {
        notes.clear(); notes += Note(text, action, run)
    }
}

val LocalBus = staticCompositionLocalOf { Bus() }

@Composable
fun Card(modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, color: Color = C.card, border: Color = C.line, pad: Dp = 16.dp,
         content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier.clip(Round).background(color).border(1.dp, border, Round)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier).padding(pad),
        content = content,
    )
}

enum class Kind { PRIMARY, SECONDARY, GHOST, GREEN, DARK }

@Composable
fun Btn(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, kind: Kind = Kind.PRIMARY, icon: ImageVector? = null, small: Boolean = false, enabled: Boolean = true) {
    val (bg, fg, br) = when (kind) {
        Kind.PRIMARY -> Triple(C.red, C.card, C.red)
        Kind.GREEN -> Triple(C.meadow, C.card, C.meadow)
        Kind.DARK -> Triple(C.ink, C.card, C.ink)
        Kind.SECONDARY -> Triple(C.card, C.ink, C.lineD)
        Kind.GHOST -> Triple(Color.Transparent, C.inkSoft, Color.Transparent)
    }
    val a = if (enabled) 1f else .45f
    Row(
        modifier.heightIn(min = if (small) 40.dp else 52.dp).clip(Pill).background(bg.copy(alpha = bg.alpha * a)).border(1.dp, br.copy(alpha = br.alpha * a), Pill)
            .clickable(enabled = enabled, onClick = onClick).padding(horizontal = if (small) 14.dp else 20.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center,
    ) {
        if (icon != null) { Icon(icon, null, tint = fg.copy(alpha = a), modifier = Modifier.size(if (small) 18.dp else 20.dp)); Spacer(Modifier.width(8.dp)) }
        Text(text, style = T.Button.copy(color = fg.copy(alpha = a)), maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** A round icon button with a spoken label. */
@Composable
fun IconBtn(icon: ImageVector, label: String, onClick: () -> Unit, modifier: Modifier = Modifier, tint: Color = C.ink, bg: Color = C.card, size: Dp = 44.dp) {
    Box(modifier.size(size).clip(CircleShape).background(bg).clickable(onClick = onClick).semantics { contentDescription = label }, contentAlignment = Alignment.Center) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(size * .5f))
    }
}

@Composable
fun Chip(text: String, on: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, icon: ImageVector? = null, color: Color = C.ink) {
    Row(
        modifier.height(38.dp).clip(Pill).background(if (on) color else C.card).border(1.dp, if (on) color else C.line, Pill).clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) { Icon(icon, null, tint = if (on) C.card else C.inkSoft, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(6.dp)) }
        Text(text, style = T.Body.copy(color = if (on) C.card else C.ink), maxLines = 1)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun Flow(modifier: Modifier = Modifier, gap: Dp = 8.dp, content: @Composable () -> Unit) {
    FlowRow(modifier, horizontalArrangement = Arrangement.spacedBy(gap), verticalArrangement = Arrangement.spacedBy(gap)) { content() }
}

@Composable
fun Field(
    value: String, onChange: (String) -> Unit, hint: String, modifier: Modifier = Modifier, number: Boolean = false, onDone: (() -> Unit)? = null,
    lead: ImageVector? = null, focus: FocusRequester? = null, style: TextStyle = T.Body, trail: (@Composable RowScope.() -> Unit)? = null, singleLine: Boolean = true,
) {
    Row(
        modifier.heightIn(min = 50.dp).clip(RoundS).background(C.card).border(1.dp, C.lineD, RoundS).padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (lead != null) { Icon(lead, null, tint = C.muted, modifier = Modifier.size(20.dp)); Spacer(Modifier.width(10.dp)) }
        Box(Modifier.weight(1f).padding(vertical = 12.dp)) {
            if (value.isEmpty()) Text(hint, style = style.copy(color = C.faint), maxLines = 1, overflow = TextOverflow.Ellipsis)
            BasicTextField(
                value, onChange, Modifier.fillMaxWidth().then(if (focus != null) Modifier.focusRequester(focus) else Modifier),
                textStyle = style, singleLine = singleLine, cursorBrush = SolidColor(C.red),
                keyboardOptions = KeyboardOptions(keyboardType = if (number) KeyboardType.Decimal else KeyboardType.Text, imeAction = if (onDone != null) ImeAction.Done else ImeAction.Default),
                keyboardActions = KeyboardActions(onDone = { onDone?.invoke() }),
            )
        }
        trail?.invoke(this)
    }
}

@Composable
fun Section(text: String, modifier: Modifier = Modifier, trail: (@Composable () -> Unit)? = null) {
    Row(modifier.fillMaxWidth().padding(top = 18.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(text.uppercase(), style = T.Tiny, modifier = Modifier.weight(1f))
        trail?.invoke()
    }
}

/** A full screen page over the tabs: a back arrow, a title, scrolling content. */
@Composable
fun Page(title: String, onBack: () -> Unit, modifier: Modifier = Modifier, scroll: Boolean = true, actions: (@Composable RowScope.() -> Unit)? = null,
         bottom: (@Composable ColumnScope.() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    BackHandler(onBack = onBack)
    Column(modifier.fillMaxSize().background(C.paper).clickable(remember { MutableInteractionSource() }, null) {}) {
        Row(Modifier.fillMaxWidth().statusBarsPadding().padding(start = 6.dp, end = 10.dp, top = 6.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconBtn(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.back), onBack, bg = Color.Transparent)
            Text(title, style = T.Title, modifier = Modifier.weight(1f).padding(start = 4.dp), maxLines = 1, overflow = TextOverflow.Ellipsis)
            actions?.invoke(this)
        }
        Column(
            Modifier.weight(1f).fillMaxWidth().then(if (scroll) Modifier.verticalScroll(rememberScrollState()) else Modifier).padding(horizontal = 16.dp),
            content = { content(); if (scroll) Spacer(Modifier.height(24.dp)) },
        )
        if (bottom != null) Column(Modifier.fillMaxWidth().background(C.paper).navigationBarsPadding().imePadding().padding(16.dp), content = bottom)
        else Spacer(Modifier.navigationBarsPadding())
    }
}

/** A sheet from the bottom over a dimmed screen; a tap on the dim or back closes it. */
@Composable
fun Sheet(onClose: () -> Unit, title: String? = null, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    BackHandler(onBack = onClose)
    val shown by animateFloatAsState(1f, tween(220), label = "sheet")
    Box(Modifier.fillMaxSize().background(C.scrim.copy(alpha = C.scrim.alpha * shown)).clickable(remember { MutableInteractionSource() }, null, onClick = onClose)) {
        Column(
            modifier.align(Alignment.BottomCenter).fillMaxWidth().clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)).background(C.paper)
                .clickable(remember { MutableInteractionSource() }, null) {}.navigationBarsPadding().imePadding().padding(horizontal = 18.dp).padding(bottom = 14.dp),
        ) {
            Box(Modifier.padding(top = 10.dp).align(Alignment.CenterHorizontally).size(40.dp, 4.dp).clip(Pill).background(C.lineD))
            if (title != null) Row(Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(title, style = T.Heading, modifier = Modifier.weight(1f), maxLines = 2, overflow = TextOverflow.Ellipsis)
                IconBtn(Icons.Rounded.Close, stringResource(R.string.close), onClose, bg = Color.Transparent, size = 40.dp)
            } else Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

/** A thin bar for one macro: how much of the day's target is in. */
@Composable
fun MacroBar(label: String, value: Double, target: Int, color: Color, modifier: Modifier = Modifier) {
    val k = if (target > 0) (value / target).toFloat() else 0f
    val anim by animateFloatAsState(k.coerceIn(0f, 1f), tween(600), label = "macro")
    Column(modifier) {
        Text(label, style = T.Small, maxLines = 1)
        Box(Modifier.padding(vertical = 5.dp).fillMaxWidth().height(6.dp).clip(Pill).background(color.copy(alpha = .18f))) {
            Box(Modifier.fillMaxWidth(anim).height(6.dp).clip(Pill).background(color))
        }
        Row(verticalAlignment = Alignment.Bottom) {
            Text("${value.roundToInt()}", style = T.Strong.copy(color = if (k > 1.25f) C.red else C.ink))
            Text(" / $target", style = T.Small)
        }
    }
}

/** A line with a label on the left and a value on the right. */
@Composable
fun Row2(left: String, right: String, modifier: Modifier = Modifier, strong: Boolean = false) {
    Row(modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Text(left, style = if (strong) T.Strong else T.Body, modifier = Modifier.weight(1f))
        Text(right, style = if (strong) T.Strong else T.Body.copy(color = C.inkSoft), textAlign = TextAlign.End)
    }
}

@Composable
fun Empty(text: String, modifier: Modifier = Modifier) {
    Text(text, style = T.Body.copy(color = C.muted), textAlign = TextAlign.Center, modifier = modifier.fillMaxWidth().padding(24.dp))
}

@Composable
fun Dot(color: Color, size: Dp = 8.dp) = Box(Modifier.size(size).clip(CircleShape).background(color))

@Composable
fun BoxScope.Center(content: @Composable () -> Unit) = Box(Modifier.align(Alignment.Center)) { content() }
