package com.altomedia.beruang.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val colors = lightColorScheme(
    primary = Green,
    onPrimary = Surface,
    secondary = Gold,
    onSecondary = Text,
    tertiary = GreenBright,
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
