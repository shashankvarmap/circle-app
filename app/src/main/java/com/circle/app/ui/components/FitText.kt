package com.circle.app.ui.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.text.TextStyle

/**
 * A one-line title that shrinks (down to [minScale] of its size) instead of breaking a word in two,
 * e.g. "Explore" next to the city switch at 200% font on a 320dp phone.
 */
@Composable
fun FitText(text: String, style: TextStyle, modifier: Modifier = Modifier, minScale: Float = 0.5f) {
    var factor by remember(text, style) { mutableFloatStateOf(1f) }
    var ready by remember(text, style) { mutableFloatStateOf(0f) }
    Text(
        text,
        style = style.copy(fontSize = style.fontSize * factor, letterSpacing = style.letterSpacing * factor),
        maxLines = 1,
        softWrap = false,
        modifier = modifier.drawWithContent { if (ready > 0f) drawContent() },
        onTextLayout = { result ->
            if (result.didOverflowWidth && factor > minScale) factor = maxOf(minScale, factor * 0.9f) else ready = 1f
        }
    )
}
