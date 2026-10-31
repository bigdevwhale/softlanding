package app.softlanding.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import app.softlanding.R

/** A travel poster from the age of balloons: cream paper, navy ink, a red-and-cream envelope, meadow green and brass. */
@Immutable
object C {
    val paper = Color(0xFFF7F1E5)
    val card = Color(0xFFFFFCF6)
    val line = Color(0xFFE6DCC8)
    val lineD = Color(0xFFD3C5AB)
    val ink = Color(0xFF1E2638)
    val inkSoft = Color(0xFF3A4358)
    val muted = Color(0xFF767A88)
    val faint = Color(0xFFA9A79F)
    val red = Color(0xFFD2553F)
    val redD = Color(0xFFA83E2C)
    val redL = Color(0xFFF6DDD5)
    val meadow = Color(0xFF6E9A4E)
    val meadowD = Color(0xFF4F7536)
    val meadowL = Color(0xFFE3EDD5)
    val gold = Color(0xFFD9A441)
    val goldD = Color(0xFFA77A22)
    val goldL = Color(0xFFF7E9C8)
    val sky = Color(0xFF8EC3E6)
    val skyD = Color(0xFF4F87B5)
    val skyL = Color(0xFFDCEDF7)
    val protein = Color(0xFF4F7FCB)
    val fat = Color(0xFFE3A13B)
    val carbs = Color(0xFF8AAE5C)
    val scrim = Color(0x99141A26)
}

/** Prata by Cyreal (SIL OFL): titles, village names and the big numbers. */
val Display = FontFamily(Font(R.font.prata, FontWeight.Normal))

/** Manrope by Mikhail Sharanda (SIL OFL): everything that has to be read fast. */
val Sans = FontFamily(
    Font(R.font.manrope_regular, FontWeight.Normal),
    Font(R.font.manrope_medium, FontWeight.Medium),
    Font(R.font.manrope_semibold, FontWeight.SemiBold),
    Font(R.font.manrope_bold, FontWeight.Bold),
)

/** Caveat by Impallari Type (SIL OFL): the aeronaut's handwriting in the logbook. */
val Hand = FontFamily(Font(R.font.caveat, FontWeight.Normal))

object T {
    val Hero = TextStyle(fontFamily = Display, fontSize = 44.sp, lineHeight = 48.sp, color = C.ink)
    val Title = TextStyle(fontFamily = Display, fontSize = 26.sp, lineHeight = 32.sp, color = C.ink)
    val Heading = TextStyle(fontFamily = Display, fontSize = 20.sp, lineHeight = 26.sp, color = C.ink)
    val Strong = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Bold, fontSize = 16.sp, lineHeight = 21.sp, color = C.ink)
    val Body = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Medium, fontSize = 15.sp, lineHeight = 21.sp, color = C.ink)
    val Small = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Medium, fontSize = 13.sp, lineHeight = 17.sp, color = C.muted)
    val Tiny = TextStyle(fontFamily = Sans, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, lineHeight = 14.sp, letterSpacing = .6.sp, color = C.muted)
    val Button = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Bold, fontSize = 15.sp, lineHeight = 18.sp)
    val Number = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Bold, fontSize = 17.sp, lineHeight = 20.sp, color = C.ink)
    val Key = TextStyle(fontFamily = Sans, fontWeight = FontWeight.SemiBold, fontSize = 26.sp, lineHeight = 30.sp, color = C.ink)
    val Log = TextStyle(fontFamily = Hand, fontSize = 21.sp, lineHeight = 23.sp, color = C.inkSoft)
}

@Composable
fun SoftTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = C.red, onPrimary = C.card, background = C.paper, surface = C.card, onSurface = C.ink,
            onBackground = C.ink, surfaceContainer = C.card, outline = C.lineD, error = C.red, secondary = C.meadow,
        ),
        typography = MaterialTheme.typography.copy(bodyLarge = T.Body, bodyMedium = T.Body, bodySmall = T.Small, labelLarge = T.Button, titleLarge = T.Title),
        content = content,
    )
}
