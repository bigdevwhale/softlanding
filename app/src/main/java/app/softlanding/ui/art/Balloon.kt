package app.softlanding.ui.art

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import app.softlanding.ui.theme.C
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/** The envelope patterns of the hangar, opened one by one by soft landings. */
object Patterns {
    private val cream = Color(0xFFFBF3E2)
    private val navy = Color(0xFF26355A)
    private val red = Color(0xFFD2553F)
    private val gold = Color(0xFFE2AE48)
    private val green = Color(0xFF6E9A4E)
    private val sky = Color(0xFF7FB6DE)
    private val plum = Color(0xFF8C4F7E)
    private val rainbow = listOf(Color(0xFFD2553F), Color(0xFFE88B3D), Color(0xFFE2C048), Color(0xFF7FAE5A), Color(0xFF5B95C9), Color(0xFF7D68B5))
    private val sunset = listOf(Color(0xFFF2C46B), Color(0xFFEE9C5C), Color(0xFFE07258), Color(0xFFC2546A), Color(0xFF8D4B78), Color(0xFF55427A))

    const val COUNT = 12

    /** The colour of gore [g] (of [n]) at height [y] (0 top .. 1 mouth) for pattern [p]. */
    fun gore(p: Int, g: Int, n: Int, y: Float): Color = when (p) {
        1 -> if (g % 2 == 0) navy else cream
        2 -> if (g % 2 == 0) green else cream
        3 -> if (g % 2 == 0) gold else red
        4 -> if (y in .42f..0.54f) red else if (g % 2 == 0) sky else cream
        5 -> rainbow[g % rainbow.size]
        6 -> listOf(red, gold, green, sky, cream, plum, navy)[Random(g * 7 + 3).nextInt(7)]
        7 -> if (y in .45f..0.6f) (if ((g + (y * 20).toInt()) % 2 == 0) gold else red) else red
        8 -> navy
        9 -> if ((g + (y * 7).toInt()) % 2 == 0) red else cream
        10 -> sunset[(g + (y * 4).toInt()).coerceAtLeast(0) % sunset.size]
        11 -> if (y < .22f) red else if (g % 2 == 0) gold else cream
        else -> if (g % 2 == 0) red else cream
    }

    /** Stars on the night envelope. */
    fun stars(p: Int) = p == 8
}

/**
 * A hot-air balloon drawn from its middle-top: [cx] the centre, [top] the crown, [r] the envelope's half width.
 * [deflate] 0..1 slumps it onto the basket after landing.
 */
fun DrawScope.balloon(cx: Float, top: Float, r: Float, pattern: Int, deflate: Float = 0f, tilt: Float = 0f, flame: Boolean = false) {
    val cy = top + r
    val belly = cy + .5f * r
    val mouth = cy + 1.45f * r
    val mouthR = .22f * r
    val basketTop = cy + 1.78f * r
    val bw = .44f * r
    val bh = .32f * r
    val squash = 1f - deflate * .55f

    fun radius(y: Float): Float = when {
        y <= belly -> { val k = (y - cy) / r; r * sqrt((1 - k * k).coerceAtLeast(0f)) }
        else -> { val k = (y - belly) / (mouth - belly); val rb = r * .866f; rb + (mouthR - rb) * (k * (2 - k) * .55f + k * .45f) }
    }

    fun ys(n: Int) = (0..n).map { top + (mouth - top) * it / n }
    val yy = ys(40)
    fun y(v: Float) = mouth - (mouth - v) * squash
    fun x(v: Float, at: Float) = cx + v + tilt * (mouth - at) * .08f

    // Ropes and basket.
    val rope = Color(0xFF5A4636)
    for (s in listOf(-1f, -.35f, .35f, 1f)) {
        drawLine(rope, Offset(cx + s * mouthR, y(mouth)), Offset(cx + s * bw * .45f, basketTop + deflate * r * .2f), strokeWidth = r * .025f)
    }
    val bt = basketTop + deflate * r * .2f
    drawRoundRect(Color(0xFF9A6A3E), Offset(cx - bw / 2, bt), Size(bw, bh), androidx.compose.ui.geometry.CornerRadius(r * .05f))
    drawRect(Color(0xFF7A5030), Offset(cx - bw / 2, bt), Size(bw, bh * .22f))
    for (i in 1..3) drawLine(Color(0x33000000), Offset(cx - bw / 2 + bw * i / 4, bt + bh * .25f), Offset(cx - bw / 2 + bw * i / 4, bt + bh), strokeWidth = r * .015f)
    if (flame) {
        val fy = y(mouth) + r * .05f
        drawOval(Color(0xCCFFC24A), Offset(cx - r * .07f, fy - r * .04f), Size(r * .14f, r * .2f))
        drawOval(Color(0xFFFFF1B8), Offset(cx - r * .035f, fy), Size(r * .07f, r * .12f))
    }

    // The outline of the envelope.
    val outline = Path().apply {
        moveTo(x(0f, y(top)), y(top))
        for (v in yy) lineTo(x(radius(v), y(v)), y(v))
        for (v in yy.reversed()) lineTo(x(-radius(v), y(v)), y(v))
        close()
    }
    clipPath(outline) {
        // Gores: meridians at angles across the front half, each a band between two of them.
        val n = 8
        for (g in 0 until n) {
            val a0 = -PI / 2 + PI * g / n
            val a1 = -PI / 2 + PI * (g + 1) / n
            val bands = 10
            for (b in 0 until bands) {
                val v0 = top + (mouth - top) * b / bands
                val v1 = top + (mouth - top) * (b + 1) / bands
                val seg = Path().apply {
                    moveTo(x(radius(v0) * sin(a0).toFloat(), y(v0)), y(v0))
                    lineTo(x(radius(v0) * sin(a1).toFloat(), y(v0)), y(v0))
                    lineTo(x(radius(v1) * sin(a1).toFloat(), y(v1)), y(v1) + 1f)
                    lineTo(x(radius(v1) * sin(a0).toFloat(), y(v1)), y(v1) + 1f)
                    close()
                }
                drawPath(seg, Patterns.gore(pattern, g, n, (b + .5f) / bands))
            }
        }
        if (Patterns.stars(pattern)) {
            val rnd = Random(11)
            repeat(26) { drawCircle(Color(0xFFF7E3A1), r * (.02f + rnd.nextFloat() * .025f), Offset(cx + (rnd.nextFloat() * 2 - 1) * r * .85f, y(top + rnd.nextFloat() * r * 1.4f))) }
        }
        // Light from the upper left, shade on the right.
        drawRect(Brush.radialGradient(listOf(Color(0x55FFFFFF), Color(0x00FFFFFF)), Offset(cx - r * .45f, y(cy - r * .35f)), r * 1.1f), Offset(cx - r * 2, top - r), Size(r * 4, r * 4))
        drawRect(Brush.horizontalGradient(listOf(Color(0x00000000), Color(0x00000000), Color(0x40000000)), cx - r, cx + r), Offset(cx - r * 2, top - r), Size(r * 4, r * 4))
        // The seams.
        for (g in 1 until 8) {
            val a = -PI / 2 + PI * g / 8
            val seam = Path().apply { yy.forEachIndexed { i, v -> val px = x(radius(v) * sin(a).toFloat(), y(v)); if (i == 0) moveTo(px, y(v)) else lineTo(px, y(v)) } }
            drawPath(seam, Color(0x22000000), style = Stroke(r * .012f))
        }
    }
    drawPath(outline, Color(0x33000000), style = Stroke(r * .02f))
    // The skirt at the mouth.
    drawRect(Color(0xFF3A3A3A).copy(alpha = .55f), Offset(cx - mouthR, y(mouth) - r * .05f), Size(mouthR * 2, r * .06f))
    if (abs(deflate) > .01f) drawOval(Color(0x22000000), Offset(cx - r * .8f, bt + bh - r * .04f), Size(r * 1.6f, r * .12f))
}
