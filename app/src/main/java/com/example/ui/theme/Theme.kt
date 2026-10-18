package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val StonicDarkColorScheme = darkColorScheme(
    primary = StonicNeonPink,
    onPrimary = Color.White,
    primaryContainer = StonicDarkPink,
    onPrimaryContainer = StonicSoftPink,
    secondary = StonicCyan,
    onSecondary = Color.Black,
    secondaryContainer = StonicCyanDark,
    onSecondaryContainer = StonicCyan,
    tertiary = StonicPurple,
    onTertiary = Color.White,
    background = StonicBackground,
    onBackground = StonicTextPrimary,
    surface = StonicSurface,
    onSurface = StonicTextPrimary,
    surfaceVariant = StonicSurfaceVariant,
    onSurfaceVariant = StonicTextSecondary,
    outline = StonicBorderPink,
    outlineVariant = StonicBorderSubtle
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    // Stonic AI is permanently styled in a signature deep futuristic dark aesthetic
    MaterialTheme(
        colorScheme = StonicDarkColorScheme,
        typography = Typography,
        content = content
    )
}
