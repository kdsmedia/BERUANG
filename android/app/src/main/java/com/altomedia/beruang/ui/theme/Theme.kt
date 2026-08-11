package com.altomedia.beruang.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val colors = darkColorScheme(
    primary = GreenBright,
    onPrimary = Bg,
    secondary = Gold,
    onSecondary = Bg,
    tertiary = Green,
    background = Bg,
    onBackground = Text,
    surface = Surface,
    onSurface = Text,
    surfaceVariant = Surface2,
    onSurfaceVariant = Muted,
    outline = Line,
    error = Danger
)

@Composable
fun BERUANGTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = colors,
        typography = BERUANGTypography,
        content = content
    )
}
