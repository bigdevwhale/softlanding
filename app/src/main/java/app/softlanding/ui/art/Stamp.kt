package app.softlanding.ui.art

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.softlanding.domain.Village
import app.softlanding.ui.theme.T
import kotlin.math.cos
import kotlin.math.sin

/** The colour of each land on stamps and the map. */
fun regionColor(region: Int) = when (region) {
    0 -> Color(0xFF6E9A4E)
    1 -> Color(0xFF4F87B5)
    2 -> Color(0xFF9A6A4E)
    else -> Color(0xFFD2553F)
}

/** A round postmark of a village, slightly turned, as if pressed by hand. */
@Composable
fun Stamp(v: Village, wind: Boolean, modifier: Modifier, ru: Boolean, faded: Boolean = false, label: Boolean = true) {
    val color = if (faded) Color(0xFFBDB6A6) else regionColor(v.region)
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) { rotate(-8f) { postmark(color, wind && !faded, v.index, !label) } }
        if (label) Text(
            v.name(ru), style = T.Tiny.copy(color = color, fontSize = 8.sp, lineHeight = 9.sp, letterSpacing = 0.sp), textAlign = TextAlign.Center,
            maxLines = 2, softWrap = true, modifier = Modifier.padding(horizontal = 9.dp).padding(top = 22.dp),
        )
    }
}

private fun DrawScope.postmark(c: Color, wind: Boolean, seed: Int, centered: Boolean) {
    val r = size.minDimension / 2
    val o = center
    drawCircle(c, r * .94f, o, style = Stroke(r * .07f))
    drawCircle(c, r * .74f, o, style = Stroke(r * .03f))
    // A tiny balloon on top of the name.
    val k = if (centered) 1.7f else 1f
    val by = if (centered) o.y - r * .3f else o.y - r * .42f
    drawCircle(c, r * .12f * k, Offset(o.x, by))
    drawPath(Path().apply { moveTo(o.x - r * .12f * k, by + r * .07f * k); lineTo(o.x, by + r * .27f * k); lineTo(o.x + r * .12f * k, by + r * .07f * k); close() }, c)
    drawRect(c, Offset(o.x - r * .05f * k, by + r * .29f * k), Size(r * .1f * k, r * .08f * k))
    // Little ticks around the ring, like the teeth of a date stamp.
    for (i in 0 until 24) {
        val a = Math.PI * 2 * i / 24 + seed
        val p0 = Offset(o.x + (r * .78f) * cos(a).toFloat(), o.y + (r * .78f) * sin(a).toFloat())
        val p1 = Offset(o.x + (r * .86f) * cos(a).toFloat(), o.y + (r * .86f) * sin(a).toFloat())
        drawLine(c.copy(alpha = .6f), p0, p1, strokeWidth = r * .025f)
    }
    if (wind) {
        val s = Offset(o.x + r * .62f, o.y - r * .62f)
        drawCircle(Color(0xFFD9A441), r * .2f, s)
        drawRoundRect(Color(0xFFFFFCF6), Offset(s.x - r * .1f, s.y - r * .03f), Size(r * .2f, r * .06f), CornerRadius(r * .03f))
        drawRoundRect(Color(0xFFFFFCF6), Offset(s.x - r * .06f, s.y + r * .06f), Size(r * .14f, r * .05f), CornerRadius(r * .03f))
    }
}
