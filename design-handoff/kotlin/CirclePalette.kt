package com.circle.app.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * The two palettes from the design. Night = "Orbit", Day = "Edition".
 * UNVERIFIED STARTER: written without a compiler. Merge with the existing ui/theme/Color.kt
 * (which has the old single dark palette) rather than keeping both.
 */
data class CirclePalette(
    val isDay: Boolean,
    val background: Color,
    val surface: Color,
    val border: Color,
    val text: Color,
    val body: Color,
    val muted: Color,
    val accent: Color,
    val onAccent: Color,
    val track: Color,
    val navLine: Color,
    val navIdle: Color,
    val navAvatar: Color,
    val navAvatarText: Color,
    val alertDot: Color,
)

val NightPalette = CirclePalette(
    isDay = false,
    background = Color(0xFF12151A), surface = Color(0xFF171B22), border = Color(0xFF21252E),
    text = Color(0xFFF1F0EC), body = Color(0xFFE7E5DF), muted = Color(0xFF7C828E),
    accent = Color(0xFFE8A87C), onAccent = Color(0xFF181310), track = Color(0xFF262B35),
    navLine = Color(0xFF21252E), navIdle = Color(0xFF7C828E),
    navAvatar = Color(0xFF3B4351), navAvatarText = Color(0xFFF1F0EC), alertDot = Color(0xFFE5484D),
)

val DayPalette = CirclePalette(
    isDay = true,
    background = Color(0xFFF3EDE2), surface = Color(0xFFFAF6EE), border = Color(0xFFD9CFBD),
    text = Color(0xFF1F1B16), body = Color(0xFF2B2620), muted = Color(0xFF6B6256),
    accent = Color(0xFFB5482A), onAccent = Color(0xFFFFF8F0), track = Color(0xFFE2D8C5),
    navLine = Color(0x241F1B16), navIdle = Color(0xFF8A7F6E),
    navAvatar = Color(0xFF1F1B16), navAvatarText = Color(0xFFF3EDE2), alertDot = Color(0xFFB5482A),
)

val LocalCirclePalette = staticCompositionLocalOf { NightPalette }

/** Use as: val p = CirclePalette.current */
object CircleTheme2 {
    val palette: CirclePalette
        @Composable @ReadOnlyComposable get() = LocalCirclePalette.current
}

/** Avatar text color: light text on dark avatar colors (e.g. slate), dark text otherwise. */
fun avatarTextColor(background: Color): Color {
    val lum = 0.299f * background.red * 255f + 0.587f * background.green * 255f + 0.114f * background.blue * 255f
    return if (lum < 110f) Color(0xFFF1F0EC) else Color(0xFF181310)
}
