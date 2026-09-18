package com.circle.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val CircleColorScheme = darkColorScheme(
    primary = AccentDefault,
    onPrimary = OnAccent,
    background = Background,
    onBackground = TextPrimary,
    surface = Surface,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceAlt,
    outline = Border,
    error = Danger
)

@Composable
fun CircleTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = CircleColorScheme,
        typography = CircleTypography,
        content = content
    )
}
