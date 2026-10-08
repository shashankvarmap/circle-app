package com.circle.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.core.view.WindowCompat

/** Accessors for the active theme, e.g. `CircleTheme.colors.accent`. */
object CircleTheme {
    val colors: CirclePalette
        @Composable @ReadOnlyComposable get() = LocalCirclePalette.current

    val postText: TextStyle
        @Composable @ReadOnlyComposable get() = postTextStyle(LocalCirclePalette.current)
}

/** Follows the system day/night setting. Night = Orbit, Day = Edition. */
@Composable
fun CircleTheme(night: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val palette = if (night) NightPalette else DayPalette

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            window.statusBarColor = palette.background.toArgb()
            window.navigationBarColor = palette.background.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !palette.background.isDark()
                isAppearanceLightNavigationBars = !palette.background.isDark()
            }
        }
    }

    CompositionLocalProvider(LocalCirclePalette provides palette) {
        MaterialTheme(
            colorScheme = palette.toColorScheme(),
            typography = CircleTypography,
            content = content
        )
    }
}

/** Material components (menus, text fields, progress) pick their colors from here. */
private fun CirclePalette.toColorScheme(): ColorScheme {
    val base = if (isDay) lightColorScheme() else darkColorScheme()
    return base.copy(
        primary = accent, onPrimary = onAccent,
        secondary = accent, onSecondary = onAccent,
        background = background, onBackground = text,
        surface = surface, onSurface = text,
        surfaceVariant = track, onSurfaceVariant = muted,
        surfaceTint = surface,
        surfaceContainerLowest = surface, surfaceContainerLow = surface, surfaceContainer = surface,
        surfaceContainerHigh = surface, surfaceContainerHighest = surface,
        outline = border, outlineVariant = border,
        error = alertDot
    )
}
