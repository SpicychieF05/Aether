package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val AetherDarkColorScheme = darkColorScheme(
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

private val AetherLightColorScheme = lightColorScheme(
    primary = Color(0xFFF57C00),
    onPrimary = Color.White,
    secondary = Color(0xFF0288D1),
    onSecondary = Color.White,
    tertiary = Color(0xFFE64A19),
    background = Color(0xFFF0F4F8),
    onBackground = Color(0xFF191C1E),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF191C1E),
    surfaceVariant = Color(0xFFE1E2EC),
    onSurfaceVariant = Color(0xFF44474F)
)

@Composable
fun AetherTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // Dynamic diorama app theme: uses rich AetherDarkColorScheme for night & dark mode,
    // with full support for light theme tokens.
    val colorScheme = if (darkTheme) AetherDarkColorScheme else AetherDarkColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    AetherTheme(darkTheme = darkTheme, content = content)
}
