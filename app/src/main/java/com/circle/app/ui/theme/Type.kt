package com.circle.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.circle.app.R

// Both fonts are bundled variable fonts (SIL OFL; licenses in assets/licenses).
// Fraunces' own defaults are wght 900 / opsz 9, so every entry sets both axes explicitly.
// Browsers pick opsz from the font size; Compose can't, so each family pins one optical size.

@OptIn(ExperimentalTextApi::class) // variable-font Font(resId, …, variationSettings)
private fun fraunces(opticalSize: Float) = FontFamily(
    listOf(400, 500, 600, 700).map { w ->
        Font(
            R.font.fraunces,
            weight = FontWeight(w),
            variationSettings = FontVariation.Settings(
                FontVariation.weight(w),
                FontVariation.Setting("opsz", opticalSize)
            )
        )
    }
)

/** Fraunces: wordmark, headings, event/explore titles. */
val Display = fraunces(opticalSize = 24f)

/** Fraunces tuned for reading sizes: day-mode post text. */
val DisplayText = fraunces(opticalSize = 16f)

/** Public Sans: everything else, including night-mode post text. */
@OptIn(ExperimentalTextApi::class)
val Body = FontFamily(
    listOf(400, 500, 600, 700, 800).map { w ->
        Font(
            R.font.public_sans,
            weight = FontWeight(w),
            variationSettings = FontVariation.Settings(FontVariation.weight(w))
        )
    }
)

private val NightPostText = TextStyle(fontFamily = Body, fontSize = 14.sp, lineHeight = 21.sp)
private val DayPostText = TextStyle(fontFamily = DisplayText, fontSize = 16.5.sp, lineHeight = 23.4.sp)

/** Post body text: Public Sans 14/1.5 at night, Fraunces 16.5/1.42 by day. */
fun postTextStyle(palette: CirclePalette): TextStyle = if (palette.isDay) DayPostText else NightPostText

private val Default = Typography()

/** Material type scale with Public Sans everywhere, Fraunces for the larger headings. */
val CircleTypography = Typography(
    displayLarge = Default.displayLarge.copy(fontFamily = Display),
    displayMedium = Default.displayMedium.copy(fontFamily = Display),
    displaySmall = Default.displaySmall.copy(fontFamily = Display),
    headlineLarge = Default.headlineLarge.copy(fontFamily = Display),
    headlineMedium = Default.headlineMedium.copy(fontFamily = Display),
    headlineSmall = TextStyle(fontFamily = Display, fontWeight = FontWeight.SemiBold, fontSize = 22.sp),
    titleLarge = TextStyle(fontFamily = Display, fontWeight = FontWeight.SemiBold, fontSize = 19.sp),
    titleMedium = TextStyle(fontFamily = Body, fontWeight = FontWeight.Bold, fontSize = 15.sp),
    titleSmall = Default.titleSmall.copy(fontFamily = Body),
    bodyLarge = TextStyle(fontFamily = Body, fontWeight = FontWeight.Normal, fontSize = 14.sp),
    bodyMedium = TextStyle(fontFamily = Body, fontWeight = FontWeight.Normal, fontSize = 13.sp),
    bodySmall = Default.bodySmall.copy(fontFamily = Body),
    labelLarge = Default.labelLarge.copy(fontFamily = Body),
    labelMedium = Default.labelMedium.copy(fontFamily = Body),
    labelSmall = TextStyle(fontFamily = Body, fontWeight = FontWeight.Bold, fontSize = 11.sp)
)
