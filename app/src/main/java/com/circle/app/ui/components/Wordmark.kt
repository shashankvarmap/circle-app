package com.circle.app.ui.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.circle.app.R
import androidx.compose.ui.res.stringResource
import com.circle.app.ui.theme.CircleTheme
import com.circle.app.ui.theme.Display

/**
 * The "Circle." wordmark, the only way it is drawn: the name in the text colour and the full stop
 * in the accent colour, in night and day. Home header, Welcome, onboarding and About use it.
 */
@Composable
fun Wordmark(
    fontSize: TextUnit,
    modifier: Modifier = Modifier,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    lineHeight: TextUnit = TextUnit.Unspecified,
) {
    val colors = CircleTheme.colors
    Text(
        buildAnnotatedString {
            append(stringResource(R.string.wordmark_name))
            withStyle(SpanStyle(color = colors.accent)) { append(stringResource(R.string.wordmark_stop)) }
        },
        fontFamily = Display, fontWeight = FontWeight.SemiBold, fontSize = fontSize,
        letterSpacing = letterSpacing, lineHeight = lineHeight, color = colors.text, modifier = modifier
    )
}
