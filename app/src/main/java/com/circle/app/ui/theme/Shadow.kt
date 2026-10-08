package com.circle.app.ui.theme

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp

/**
 * A soft, offset drop shadow like CSS `box-shadow: 0 <offsetY> <blur> <color>`.
 * Compose's elevation shadows can't be offset or tinted on older APIs, so this draws one
 * with a framework shadow layer. Apply it before `clip`. Hardware-accelerated shadow
 * layers need API 28+; on 26–27 the card simply draws without a shadow.
 */
fun Modifier.softShadow(color: Color, offsetY: Dp, blur: Dp, cornerRadius: Dp): Modifier = drawBehind {
    // CSS blur is ~2 sigma; Android's shadow radius maps to sigma ≈ 0.577 r + 0.5.
    val radius = ((blur.toPx() / 2f) - 0.5f).coerceAtLeast(0f) / 0.57735f
    drawIntoCanvas { canvas ->
        val paint = Paint()
        paint.asFrameworkPaint().apply {
            this.color = Color.Transparent.toArgb()
            setShadowLayer(radius, 0f, offsetY.toPx(), color.toArgb())
        }
        val r = cornerRadius.toPx()
        canvas.drawRoundRect(0f, 0f, size.width, size.height, r, r, paint)
    }
}

/** The card shadow from the tokens: night 0 8 20 black@28%, day 0 6 16 brown@10%. */
fun Modifier.cardShadow(palette: CirclePalette, cornerRadius: Dp): Modifier =
    softShadow(palette.cardShadow, palette.cardShadowOffsetY, palette.cardShadowBlur, cornerRadius)
