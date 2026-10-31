package app.softlanding.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.softlanding.R
import app.softlanding.ui.theme.C
import app.softlanding.ui.theme.T

/** Typing a number into [value] by key: no system keyboard jumping up, and the screen stays put. */
object Typed {
    fun press(value: String, key: Char, max: Int = 5, decimals: Int = 1): String = when (key) {
        '<' -> value.dropLast(1)
        '.' -> if (value.contains('.') || decimals == 0) value else (value.ifEmpty { "0" }) + "."
        else -> {
            val next = if (value == "0") key.toString() else value + key
            val (int, frac) = next.split('.').let { it[0] to it.getOrNull(1) }
            if (int.length > max || (frac?.length ?: 0) > decimals) value else next
        }
    }
}

@Composable
fun Keypad(onKey: (Char) -> Unit, modifier: Modifier = Modifier, dot: Boolean = true) {
    val haptic = LocalHapticFeedback.current
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        for (row in listOf("123", "456", "789", (if (dot) "." else " ") + "0<")) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            for (k in row) {
                Box(
                    Modifier.weight(1f).height(54.dp).clip(RoundS).background(if (k == ' ') C.paper else C.card)
                        .then(if (k == ' ') Modifier else Modifier.clickable { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onKey(k) }),
                    contentAlignment = Alignment.Center,
                ) {
                    when (k) {
                        '<' -> Icon(Icons.AutoMirrored.Rounded.Backspace, stringResource(R.string.erase), tint = C.inkSoft)
                        ' ' -> Unit
                        else -> Text(if (k == '.') "," else k.toString(), style = T.Key)
                    }
                }
            }
        }
    }
}

