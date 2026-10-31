package app.softlanding.ui.art

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import app.softlanding.domain.Landing
import app.softlanding.domain.Season
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

object Art {
    /** Renders and screenshots want a still picture. */
    @Volatile var still = false
}

/**
 * The day as a flight.
 * [alt] is what's left of the norm as a share of it: 1 at dawn, 0 on the meadow, below 0 the balloon is down early.
 * [progress] is the clock across the waking day. [landing] set means the day is over and shows how it ended.
 */
data class SceneState(
    val alt: Float,
    val progress: Float,
    val landing: Landing? = null,
    val pattern: Int = 0,
    val season: Season = Season.SUMMER,
    val hour: Int = 12,
    /** The lower edge of the meadow as a share of the norm (how much may be left and still count). */
    val meadow: Float = .1f,
)

private data class Sky(val top: Color, val bottom: Color, val sun: Color?, val night: Boolean)

private fun sky(hour: Int): Sky = when (hour) {
    in 5..7 -> Sky(Color(0xFF9CC4E4), Color(0xFFF6D8B4), Color(0xFFFFE2A6), false)
    in 8..16 -> Sky(Color(0xFF7FB8E2), Color(0xFFDDEEF7), Color(0xFFFFF4C9), false)
    in 17..19 -> Sky(Color(0xFF6E86BC), Color(0xFFF4B486), Color(0xFFFFC27A), false)
    in 20..21 -> Sky(Color(0xFF3B4C80), Color(0xFFC7869A), null, true)
    else -> Sky(Color(0xFF1B2547), Color(0xFF3E4C7E), null, true)
}

private data class Land(val far: Color, val near: Color, val meadow: Color, val fieldA: Color, val fieldB: Color, val roof: Color, val snow: Boolean)

private fun land(s: Season) = when (s) {
    Season.SUMMER -> Land(Color(0xFF9DBF8A), Color(0xFF7FA868), Color(0xFF8DC06A), Color(0xFFD9C27A), Color(0xFFA7C774), Color(0xFFC0533E), false)
    Season.AUTUMN -> Land(Color(0xFFC8A46E), Color(0xFFB5874E), Color(0xFF9CB461), Color(0xFFD9A85A), Color(0xFFC77A44), Color(0xFF8E3F30), false)
    Season.WINTER -> Land(Color(0xFFD9E2EC), Color(0xFFC6D3E0), Color(0xFFEFF4F8), Color(0xFFE4EBF2), Color(0xFFD5DFEA), Color(0xFF8A4A3C), true)
    Season.SPRING -> Land(Color(0xFFB4D29A), Color(0xFF94BE7A), Color(0xFFA6D27C), Color(0xFFCFE0A0), Color(0xFFB9D88E), Color(0xFFD06A52), false)
}

@Composable
fun FlightScene(s: SceneState, modifier: Modifier = Modifier) {
    val still = Art.still
    val t = rememberInfiniteTransition(label = "scene")
    val sway by if (still) androidx.compose.runtime.remember { androidx.compose.runtime.mutableFloatStateOf(0f) } else
        t.animateFloat(0f, 1f, infiniteRepeatable(tween(5200, easing = LinearEasing), RepeatMode.Restart), label = "sway")
    val drift by if (still) androidx.compose.runtime.remember { androidx.compose.runtime.mutableFloatStateOf(.3f) } else
        t.animateFloat(0f, 1f, infiniteRepeatable(tween(90_000, easing = LinearEasing), RepeatMode.Restart), label = "drift")
    // A logged meal drops the balloon with a little bounce.
    val alt by animateFloatAsState(s.alt, if (still) tween(0) else spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = 40f), label = "alt")
    val prog by animateFloatAsState(s.progress, tween(if (still) 0 else 900), label = "prog")
    Canvas(modifier) { scene(s, alt, prog, sway, drift) }
}

private fun DrawScope.scene(s: SceneState, alt: Float, prog: Float, sway: Float, drift: Float) {
    val w = size.width
    val h = size.height
    val ground = h * .8f
    val k = sky(s.hour)
    val l = land(s.season)

    drawRect(Brush.verticalGradient(listOf(k.top, k.bottom), 0f, ground))
    if (k.night) {
        val r = Random(7)
        repeat(40) { drawCircle(Color(0xCCFFFFFF), w * .0025f * (1 + r.nextFloat()), Offset(r.nextFloat() * w, r.nextFloat() * ground * .7f)) }
        drawCircle(Color(0xFFF6EBC8), w * .045f, Offset(w * .82f, h * .16f))
        drawCircle(k.top, w * .04f, Offset(w * .845f, h * .145f))
    } else k.sun?.let { sun ->
        val sx = w * (.12f + .76f * (s.hour.coerceIn(6, 20) - 6) / 14f)
        val sy = h * (.32f - .2f * sin(PI * (s.hour.coerceIn(6, 20) - 6) / 14).toFloat())
        drawCircle(Brush.radialGradient(listOf(sun.copy(alpha = .55f), sun.copy(alpha = 0f)), Offset(sx, sy), w * .16f), w * .16f, Offset(sx, sy))
        drawCircle(sun, w * .05f, Offset(sx, sy))
    }

    // Clouds drifting slowly to the right.
    val cr = Random(3)
    repeat(5) { i ->
        val base = cr.nextFloat()
        val cx = ((base + drift * (0.4f + i * .12f)) % 1.2f - .1f) * w
        val cy = h * (.38f + cr.nextFloat() * .24f)
        cloud(cx, cy, w * (.028f + cr.nextFloat() * .028f), if (k.night) Color(0x553E4C7E) else Color(0xD9FFFFFF))
    }

    // Hills behind, then the land.
    hills(ground - h * .07f, h * .07f, l.far, 2.1f, .2f)
    hills(ground - h * .03f, h * .05f, l.near, 3.3f, 1.3f)
    drawRect(l.near, Offset(0f, ground), Size(w, h - ground))
    // Fields in strips, the meadow on the right, and a lake past it.
    val meadowX0 = w * .76f
    val meadowX1 = w * .93f
    val strips = 7
    for (i in 0 until strips) {
        val x0 = w * i / strips * .76f
        drawRect(if (i % 2 == 0) l.fieldA else l.fieldB, Offset(x0, ground), Size(w * .76f / strips + 1, h - ground))
        for (j in 1..3) drawLine(Color(0x22000000), Offset(x0, ground + (h - ground) * j / 4), Offset(x0 + w * .76f / strips, ground + (h - ground) * j / 4), strokeWidth = 1.5f)
    }
    drawRect(l.meadow, Offset(meadowX0, ground), Size(meadowX1 - meadowX0, h - ground))
    drawRect(if (l.snow) Color(0xFFBFD3E6) else Color(0xFF6FA6CF), Offset(meadowX1, ground + (h - ground) * .25f), Size(w - meadowX1, (h - ground) * .75f))
    if (!l.snow) for (i in 0..14) {
        val fx = meadowX0 + (meadowX1 - meadowX0) * (i * .37f % 1f)
        val fy = ground + (h - ground) * (.2f + (i * .53f % 1f) * .7f)
        drawCircle(if (i % 3 == 0) Color(0xFFFFF6D6) else Color(0xFFF2D35B), w * .004f, Offset(fx, fy))
    }
    village(meadowX0 + (meadowX1 - meadowX0) * .55f, ground, w * .028f, l)
    if (s.landing == Landing.FAIR) bunting(meadowX0, meadowX1, ground - h * .1f)

    // The glide path from the balloon to the meadow.
    val bx0 = w * .08f
    val bxMax = w * .74f
    val r = w * .065f
    val top = h * .34f
    val lowest = ground - r * 2.1f
    fun yAt(a: Float) = lowest - (lowest - top) * a.coerceIn(0f, 1.05f)
    val swayY = sin(sway * 2 * PI).toFloat() * h * .008f
    val swayT = sin(sway * 2 * PI + 1).toFloat()

    when (s.landing) {
        null -> {
            val bx = bx0 + (bxMax - bx0) * prog
            if (alt > 0f) {
                // Still flying: a dotted line to the meadow.
                val target = Offset((meadowX0 + meadowX1) / 2, ground - r * .2f)
                val start = Offset(bx, yAt(alt) + r * 2.1f)
                val p = Path().apply { moveTo(start.x, start.y); quadraticTo((start.x + target.x) / 2, start.y + (target.y - start.y) * .15f, target.x, target.y) }
                drawPath(p, Color(0x99FFFFFF), style = Stroke(w * .005f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(w * .012f, w * .014f))))
                drawCircle(Color(0xCCFFFFFF), w * .01f, target)
                balloon(bx, yAt(alt) + swayY, r, s.pattern, tilt = swayT * .3f, flame = alt > .5f)
            } else {
                // Below zero: down early, in the fields.
                balloon(minOf(bx, meadowX0 - r * 1.3f), ground - r * 2.1f, r, s.pattern, deflate = ((-alt) * 3).coerceIn(.15f, .8f))
            }
        }
        Landing.SOFT, Landing.FAIR -> balloon((meadowX0 + meadowX1) / 2 - r * .6f, ground - r * 2.08f, r, s.pattern, deflate = .2f)
        Landing.EARLY -> balloon(w * .48f, ground - r * 2.1f + (h - ground) * .25f, r, s.pattern, deflate = .6f)
        Landing.DRIFT -> balloon(w * .97f, yAt(.32f) + swayY, r * .8f, s.pattern, tilt = .6f)
        Landing.FOG -> Unit
    }

    if (s.landing == Landing.FOG) drawRect(Brush.verticalGradient(listOf(Color(0xCCE9ECF0), Color(0xF2E9ECF0)), 0f, h))
    // The day's light on everything at dusk.
    if (k.night) drawRect(Color(0x33101830))
}

private fun DrawScope.cloud(x: Float, y: Float, r: Float, c: Color) {
    drawCircle(c, r, Offset(x, y))
    drawCircle(c, r * .75f, Offset(x - r * .9f, y + r * .2f))
    drawCircle(c, r * .8f, Offset(x + r * .95f, y + r * .15f))
    drawRoundRect(c, Offset(x - r * 1.6f, y + r * .1f), Size(r * 3.3f, r * .75f), androidx.compose.ui.geometry.CornerRadius(r * .4f))
}

private fun DrawScope.hills(base: Float, amp: Float, c: Color, freq: Float, phase: Float) {
    val w = size.width
    val p = Path().apply {
        moveTo(0f, size.height)
        lineTo(0f, base)
        val n = 48
        for (i in 0..n) {
            val x = w * i / n
            lineTo(x, base - amp * (.5f + .5f * sin(i.toFloat() / n * freq * 2 * PI.toFloat() + phase)))
        }
        lineTo(w, size.height)
        close()
    }
    drawPath(p, c)
}

private fun DrawScope.village(cx: Float, ground: Float, u: Float, l: Land) {
    val walls = listOf(Color(0xFFF4E8CF), Color(0xFFEAD7B1), Color(0xFFF7EEDD))
    // A little church, three houses and a tree.
    drawRect(walls[1], Offset(cx + u * 1.2f, ground - u * 2.6f), Size(u * 1.2f, u * 2.6f))
    drawPath(Path().apply { moveTo(cx + u * 1.1f, ground - u * 2.6f); lineTo(cx + u * 1.8f, ground - u * 4.3f); lineTo(cx + u * 2.5f, ground - u * 2.6f); close() }, l.roof)
    for ((i, dx) in listOf(-2.4f, -.9f, 2.7f).withIndex()) {
        val x = cx + dx * u
        val hh = u * (1.2f + (i % 2) * .3f)
        drawRect(walls[i % 3], Offset(x, ground - hh), Size(u * 1.3f, hh))
        drawPath(Path().apply { moveTo(x - u * .15f, ground - hh); lineTo(x + u * .65f, ground - hh - u * .8f); lineTo(x + u * 1.45f, ground - hh); close() }, if (l.snow) Color(0xFFF4F7FA) else l.roof)
        drawRect(Color(0xFFF2C55C), Offset(x + u * .45f, ground - hh * .65f), Size(u * .35f, u * .35f))
    }
    drawCircle(if (l.snow) Color(0xFFE6EEF5) else lerp(l.near, Color.Black, .25f), u * .9f, Offset(cx - 3.6f * u, ground - u * 1.5f))
    drawRect(Color(0xFF6B4A33), Offset(cx - 3.7f * u, ground - u * .8f), Size(u * .22f, u * .8f))
}

private fun DrawScope.bunting(x0: Float, x1: Float, y: Float) {
    val n = 9
    val cs = listOf(Color(0xFFD2553F), Color(0xFFE2AE48), Color(0xFF5B95C9), Color(0xFF7FAE5A))
    drawLine(Color(0xFF6B4A33), Offset(x0, y), Offset(x1, y), strokeWidth = 2f)
    for (i in 0 until n) {
        val x = x0 + (x1 - x0) * (i + .5f) / n
        val d = (x1 - x0) / n * .4f
        drawPath(Path().apply { moveTo(x - d, y); lineTo(x + d, y); lineTo(x, y + d * 1.8f); close() }, cs[i % cs.size])
    }
}
