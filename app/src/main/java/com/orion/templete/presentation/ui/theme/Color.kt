package com.orion.templete.presentation.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color



val Black = Color(0xFF121212)
val White = Color(0xFFF9F9F9)
val LiteGray = Color(0xFFE6E6E6)
val Blue = Color(0xFF3897F0)
val Gray = Color(0xFF2F343F)
val Dark = Color(0xFF262B33)

// Additional colors
val ErrorRed = Color(0xFFED4956)
val LighterGray = Color(0xFFDBDBDB)

internal val LightColors = lightColorScheme(
    primary = Black,
    onPrimary = White,
    primaryContainer = Blue,
    onPrimaryContainer = White,
    inversePrimary = Black,
    secondary = Gray,
    background = White,
    onBackground = Black,
    surface = White,
    surfaceVariant = LiteGray,
    onSurface = Black,
    error = ErrorRed,
    onError = White,
    errorContainer = ErrorRed,
    onErrorContainer = White,
    outline = Gray,
    outlineVariant = LighterGray,
    scrim = Black.copy(alpha = 0.5f)
)

internal val DarkColors = darkColorScheme(
    primary = White,
    onPrimary = Black,
    primaryContainer = White,
    onPrimaryContainer = Blue,
    inversePrimary = Black,
    secondary = Gray,
    tertiary = LighterGray,
    onTertiary = Black,
    tertiaryContainer = Gray,
    background = Black,
    onBackground = White,
    surface = Dark,
    surfaceVariant = Gray,
    onSurface = White,
    onSurfaceVariant = White,
    error = ErrorRed,
    onError = White,
    outline = Gray,
    inverseSurface = White,

    outlineVariant = Dark,
    scrim = Black.copy(alpha = 0.5f)
)