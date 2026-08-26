package com.viralclip.app.ui.theme

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
    primary = ViralPurple,
    onPrimary = Color.White,
    primaryContainer = ViralPurpleDark,
    onPrimaryContainer = ViralPurpleLight,
    secondary = ViralPink,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF3D1A35),
    onSecondaryContainer = ViralPink,
    tertiary = ViralCyan,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFF0D3D45),
    onTertiaryContainer = ViralCyan,
    background = DarkBackground,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurfaceElevated,
    onSurfaceVariant = TextSecondary,
    surfaceTint = ViralPurple,
    outline = DarkBorder,
    outlineVariant = Color(0xFF333344),
    error = ErrorColor,
    onError = Color.White,
    errorContainer = Color(0xFF3D1515),
    onErrorContainer = ErrorColor,
    inverseSurface = TextPrimary,
    inverseOnSurface = DarkBackground,
    inversePrimary = ViralPurpleDark
)

private val LightColorScheme = lightColorScheme(
    primary = ViralPurple,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEDE9FE),
    onPrimaryContainer = ViralPurpleDark,
    secondary = ViralPink,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFCE7F3),
    onSecondaryContainer = ViralPinkDark,
    tertiary = ViralCyan,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFCFFAFE),
    onTertiaryContainer = Color(0xFF0E7490),
    background = LightBackground,
    onBackground = TextPrimaryDark,
    surface = LightSurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = LightSurfaceElevated,
    onSurfaceVariant = TextSecondaryDark,
    surfaceTint = ViralPurple,
    outline = LightBorder,
    outlineVariant = Color(0xFFD1D5DB),
    error = ErrorColor,
    onError = Color.White,
    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = Color(0xFF991B1B)
)

@Composable
fun ViralClipTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
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
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = ViralClipTypography,
        content = content
    )
}
