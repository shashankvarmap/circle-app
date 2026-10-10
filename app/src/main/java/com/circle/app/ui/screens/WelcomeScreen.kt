package com.circle.app.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.circle.app.R
import com.circle.app.ui.launch.SessionViewModel
import com.circle.app.ui.theme.*

/**
 * Welcome: the wordmark over two faint rings, Create account and Log in. Create account only
 * shows before the story was ever seen on this device (ACCESS_AND_LAUNCH 0.1, case 3).
 */
@Composable
fun WelcomeScreen(
    session: SessionViewModel,
    onCreateAccount: () -> Unit,
    onLogIn: () -> Unit,
    onLegal: () -> Unit,
) {
    val colors = CircleTheme.colors
    val canCreate by session.canCreateAccount.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize().background(colors.background).systemBarsPadding()) {
        Box(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 28.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(300.dp)) {
                val c = Offset(size.width / 2, size.height / 2)
                drawCircle(
                    colors.border, radius = 140.dp.toPx(), center = c,
                    style = Stroke(1.4.dp.toPx(), cap = StrokeCap.Round, pathEffect = PathEffect.dashPathEffect(floatArrayOf(2.dp.toPx(), 8.dp.toPx())))
                )
                drawCircle(colors.border, radius = 92.dp.toPx(), center = c, style = Stroke(1.2.dp.toPx()))
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                com.circle.app.ui.components.Wordmark(
                    fontSize = 58.sp, letterSpacing = (-1.74).sp, lineHeight = 58.sp, modifier = Modifier.semantics { heading() }
                )
                Text(
                    stringResource(R.string.welcome_tagline), fontFamily = Display, fontStyle = FontStyle.Italic,
                    fontSize = 18.sp, lineHeight = 24.sp, color = colors.muted, textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 14.dp).widthIn(max = 270.dp)
                )
                Text(
                    stringResource(R.string.welcome_line), fontSize = 14.sp, color = colors.muted,
                    textAlign = TextAlign.Center, modifier = Modifier.padding(top = 10.dp)
                )
            }
        }
        Column(Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, bottom = 30.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (canCreate) WelcomeButton(stringResource(R.string.welcome_create_account), filled = true, onClick = onCreateAccount)
            WelcomeButton(stringResource(R.string.welcome_log_in), filled = !canCreate, onClick = onLogIn)
            Text(
                buildAnnotatedString {
                    append(stringResource(R.string.welcome_agree_prefix))
                    withStyle(SpanStyle(color = colors.text, textDecoration = TextDecoration.Underline)) {
                        append(stringResource(R.string.welcome_agree_link))
                    }
                    append(stringResource(R.string.welcome_agree_suffix))
                },
                fontSize = 11.5.sp, lineHeight = 17.sp, color = colors.muted, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp).clip(RoundedCornerShape(8.dp))
                    .clickable(role = Role.Button, onClickLabel = stringResource(R.string.welcome_agree_link), onClick = onLegal)
                    .padding(vertical = 8.dp)
            )
        }
    }
}

@Composable
private fun WelcomeButton(text: String, filled: Boolean, onClick: () -> Unit) {
    val colors = CircleTheme.colors
    val shape = RoundedCornerShape(999.dp)
    Box(
        Modifier.fillMaxWidth().heightIn(min = 50.dp).clip(shape)
            .background(if (filled) colors.accent else colors.background)
            .then(if (filled) Modifier else Modifier.border(1.5.dp, colors.border, shape))
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text, color = if (filled) colors.onAccent else colors.text, fontSize = 15.sp,
            fontWeight = if (filled) FontWeight.Bold else FontWeight.SemiBold
        )
    }
}
