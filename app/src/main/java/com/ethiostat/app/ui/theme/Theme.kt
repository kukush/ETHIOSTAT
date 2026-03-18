package com.ethiostat.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val NavyDarkColorScheme = darkColorScheme(
    primary = BrandBlue,
    secondary = VoiceGreen,
    tertiary = GoldAccent,
    background = NavyBackground,
    surface = NavySurface,
    surfaceVariant = NavySurfaceVariant,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.Black,
    onBackground = Color(0xFFE8EEF4),
    onSurface = Color(0xFFE8EEF4),
    onSurfaceVariant = Color(0xFF8CA5BE),
    error = ErrorRed
)

private val StyledLightColorScheme = lightColorScheme(
    primary = BrandBlue,
    secondary = VoiceGreen,
    tertiary = GoldAccent,
    background = LightBackground,
    surface = LightSurface,
    surfaceVariant = LightSurfaceVariant,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.Black,
    onBackground = Color(0xFF1A2B3C),
    onSurface = Color(0xFF1A2B3C),
    onSurfaceVariant = Color(0xFF4A6278),
    error = ErrorRed
)

@Composable
fun EthioStatTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) NavyDarkColorScheme else StyledLightColorScheme
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }
    
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
