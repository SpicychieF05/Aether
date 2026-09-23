package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val AetherColorScheme = darkColorScheme(
    primary = AetherAmber,
    onPrimary = Color(0xFF1E1A00),
    secondary = AetherSky,
    onSecondary = Color(0xFF003544),
    tertiary = AetherWarmOrange,
    background = AetherMidnight,
    onBackground = AetherTextLight,
    surface = AetherDeepNavy,
    onSurface = AetherTextLight,
    surfaceVariant = AetherSurface,
    onSurfaceVariant = AetherTextDim
)

@Composable
fun AetherTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = AetherColorScheme,
        typography = Typography,
        content = content
    )
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    AetherTheme(content = content)
}
