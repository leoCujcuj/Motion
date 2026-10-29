package com.example.motion.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val AppleDarkScheme = darkColorScheme(
    primary = AppleBlueDark,
    onPrimary = Color.White,
    secondary = AppleGreenDark,
    onSecondary = Color.White,
    background = AppleBackgroundDark,
    onBackground = AppleTextPrimaryDark,
    surface = AppleSurfaceDark,
    onSurface = AppleTextPrimaryDark,
    surfaceVariant = AppleGroupedCardDark,
    onSurfaceVariant = AppleTextSecondaryDark,
    outline = AppleSeparatorDark
)

private val AppleLightScheme = lightColorScheme(
    primary = AppleBlueLight,
    onPrimary = Color.White,
    secondary = AppleGreenLight,
    onSecondary = Color.White,
    background = AppleBackgroundLight,
    onBackground = AppleTextPrimaryLight,
    surface = AppleSurfaceLight,
    onSurface = AppleTextPrimaryLight,
    surfaceVariant = AppleGroupedCardLight,
    onSurfaceVariant = AppleTextSecondaryLight,
    outline = AppleSeparatorLight
)

@Composable
fun MotionTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) AppleDarkScheme else AppleLightScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
