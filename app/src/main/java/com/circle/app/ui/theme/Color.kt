package com.circle.app.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The two palettes from the design (design-handoff/design-tokens.json).
 * Night = "Orbit", Day = "Edition". Read the active one with `CircleTheme.colors`.
 * `muted` is the lowest allowed color for readable text — there is no dimmer text token.
 */
@Immutable
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
    /** Non-text fills: placeholders, progress tracks, disabled buttons, "+N" chips. */
    val track: Color,
    val navLine: Color,
    val navIdle: Color,
    val navAvatar: Color,
    val navAvatarText: Color,
    val alertDot: Color,
    val cardShadow: Color,
    val cardShadowOffsetY: Dp,
    /** CSS blur radius from the tokens; see [softShadow] for the conversion. */
    val cardShadowBlur: Dp,
)

val NightPalette = CirclePalette(
    isDay = false,
    background = Color(0xFF12151A), surface = Color(0xFF171B22), border = Color(0xFF21252E),
    text = Color(0xFFF1F0EC), body = Color(0xFFE7E5DF), muted = Color(0xFF7C828E),
    accent = Color(0xFFE8A87C), onAccent = Color(0xFF181310), track = Color(0xFF262B35),
    navLine = Color(0xFF21252E), navIdle = Color(0xFF7C828E),
    navAvatar = Color(0xFF3B4351), navAvatarText = Color(0xFFF1F0EC), alertDot = Color(0xFFE5484D),
    cardShadow = Color.Black.copy(alpha = 0.28f), cardShadowOffsetY = 8.dp, cardShadowBlur = 20.dp,
)

val DayPalette = CirclePalette(
    isDay = true,
    background = Color(0xFFF3EDE2), surface = Color(0xFFFAF6EE), border = Color(0xFFD9CFBD),
    text = Color(0xFF1F1B16), body = Color(0xFF2B2620), muted = Color(0xFF6B6256),
    accent = Color(0xFFB5482A), onAccent = Color(0xFFFFF8F0), track = Color(0xFFE2D8C5),
    navLine = Color(0x241F1B16), navIdle = Color(0xFF8A7F6E),
    navAvatar = Color(0xFF1F1B16), navAvatarText = Color(0xFFF3EDE2), alertDot = Color(0xFFB5482A),
    cardShadow = Color(60, 40, 20).copy(alpha = 0.10f), cardShadowOffsetY = 6.dp, cardShadowBlur = 16.dp,
)

val LocalCirclePalette = staticCompositionLocalOf { NightPalette }

// Avatar and group tile colors. Theme-independent: the same in night and day.
val AvatarOrange = Color(0xFFD98C4A)
val AvatarSage = Color(0xFF7FA88C)
val AvatarBlue = Color(0xFF8DA6D9)
val AvatarSlate = Color(0xFF3B4351)
val AvatarRose = Color(0xFFC77B8F)
val AvatarTeal = Color(0xFF6FA8B5)
val AvatarAmber = Color(0xFFE9C46A)

private val AvatarTextDark = Color(0xFF181310)
private val AvatarTextLight = Color(0xFFF1F0EC)

/** Initials color on an avatar fill: light on dark fills (slate), dark otherwise. */
fun avatarTextColor(background: Color): Color {
    // Same rule as the prototype's fgOf(): 0.299 R + 0.587 G + 0.114 B < 110 (0–255 scale).
    val lum = (0.299f * background.red + 0.587f * background.green + 0.114f * background.blue) * 255f
    return if (lum < 110f) AvatarTextLight else AvatarTextDark
}

/** True for colors where light content reads better; used for system bar icons. */
internal fun Color.isDark(): Boolean = luminance() < 0.5f
