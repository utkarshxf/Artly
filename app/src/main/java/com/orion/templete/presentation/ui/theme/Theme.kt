package com.orion.templete.presentation.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF1E1E1E),  // Very dark gray, almost black
    onPrimary = Color(0xFFFFFFFF),  // White
    primaryContainer = Color(0xFF333333),  // Darker gray
    onPrimaryContainer = Color(0xFFFFFFFF),  // White
    inversePrimary = Color(0xFFBDBDBD),  // Light gray for inverse primary
    secondary = Color(0xFF4A4A4A),  // Dark gray with a hint of blue
    onSecondary = Color(0xFFFFFFFF),  // White
    secondaryContainer = Color(0xFF616161),  // Slightly lighter dark gray
    onSecondaryContainer = Color(0xFF1E1E1E),  // Very dark gray
    tertiary = Color(0xFF585858),  // Medium dark gray
    onTertiary = Color(0xFFFFFFFF),  // White
    tertiaryContainer = Color(0xFF6D6D6D),  // Slightly lighter gray
    onTertiaryContainer = Color(0xFF1E1E1E),  // Very dark gray
    background = Color(0xFF121212),  // Very dark gray
    onBackground = Color(0xFFFFFFFF),  // White
    surface = Color(0xFF1E1E1E),  // Very dark gray
    onSurface = Color(0xFFFFFFFF),  // White
    surfaceVariant = Color(0xFF2C2C2C),  // Dark gray
    onSurfaceVariant = Color(0xFFE0E0E0),  // Light gray
    surfaceTint = Color(0xFFBDBDBD),  // Light gray for accents
    inverseSurface = Color(0xFFBDBDBD),  // Light gray
    inverseOnSurface = Color(0xFF121212),  // Very dark gray
    error = Color(0xFFCF6679),  // Soft red for errors
    onError = Color(0xFFFFFFFF),  // White
    errorContainer = Color(0xFFCF6679),  // Soft red
    onErrorContainer = Color(0xFFFFFFFF),  // White
    outline = Color(0xFF3C3C3C),  // Darker gray for outlines
    outlineVariant = Color(0xFF4F4F4F),  // Slightly lighter dark gray
    scrim = Color(0xFF121212)  // Very dark gray for scrims
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF212121),  // Very dark gray, almost black
    onPrimary = Color(0xFFFFFFFF),  // White
    primaryContainer = Color(0xFF424242),  // Dark gray
    onPrimaryContainer = Color(0xFFFFFFFF),  // White
    inversePrimary = Color(0xFFFFFFFF),  // White
    secondary = Color(0xFF455A64),  // Blue-gray
    onSecondary = Color(0xFFFFFFFF),  // White
    secondaryContainer = Color(0xFF78909C),  // Lighter blue-gray
    onSecondaryContainer = Color(0xFF212121),  // Very dark gray
    tertiary = Color(0xFF676767),  // Medium gray
    onTertiary = Color(0xFFFFFFFF),  // White
    tertiaryContainer = Color(0xFF676767),  // Medium gray
    onTertiaryContainer = Color(0xFFFFFFFF),  // White
    background = Color(0xFFF5F5F5),  // Very light gray
    onBackground = Color(0xFF212121),  // Very dark gray
    surface = Color(0xFFFFFFFF),  // White
    onSurface = Color(0xFF212121),  // Very dark gray
    surfaceVariant = Color(0xFFE0E0E0),  // Light gray
    onSurfaceVariant = Color(0xFF424242),  // Dark gray
    surfaceTint = Color(0xFF212121),  // Very dark gray
    inverseSurface = Color(0xFF212121),  // Very dark gray
    inverseOnSurface = Color(0xFFFFFFFF),  // White
    error = Color(0xFF90A4AE),  // Blue-gray (softer error indication)
    onError = Color(0xFFFFFFFF),  // White
    errorContainer = Color(0xFF90A4AE),  // Blue-gray
    onErrorContainer = Color(0xFFFFFFFF),  // White
    outline = Color(0xFFEAEAEA),  // Very light gray
    outlineVariant = Color(0xFFBDBDBD),  // Light gray
    scrim = Color(0xFFFFFFFF)  // White
)
@Composable
fun TempleteTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}