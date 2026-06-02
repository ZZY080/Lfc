package com.lfc.consumer.ui.theme

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

private val LightColorScheme = lightColorScheme(
    primary = XhsRed,
    onPrimary = XhsSurface,
    primaryContainer = XhsRedContainer,
    onPrimaryContainer = XhsRedDark,
    secondary = XhsRedLight,
    onSecondary = XhsSurface,
    tertiary = XhsRedDark,
    onTertiary = XhsSurface,
    background = XhsBackground,
    onBackground = XhsTextPrimary,
    surface = XhsSurface,
    onSurface = XhsTextPrimary,
    surfaceVariant = XhsBackground,
    onSurfaceVariant = XhsTextSecondary,
    outline = XhsDivider,
    outlineVariant = XhsDivider,
    error = XhsRed,
    onError = XhsSurface,
)

private val DarkColorScheme = darkColorScheme(
    primary = XhsRedDarkTheme,
    onPrimary = XhsTextPrimaryDark,
    primaryContainer = XhsRedDark,
    onPrimaryContainer = XhsRedLight,
    secondary = XhsRedLight,
    onSecondary = XhsTextPrimaryDark,
    background = XhsBackgroundDark,
    onBackground = XhsTextPrimaryDark,
    surface = XhsSurfaceDark,
    onSurface = XhsTextPrimaryDark,
    surfaceVariant = XhsBackgroundDark,
    onSurfaceVariant = XhsTextSecondaryDark,
    outline = Color(0xFF3A3A3A),
    error = XhsRedDarkTheme,
    onError = XhsTextPrimaryDark,
)

@Composable
fun LfcTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            WindowCompat.setDecorFitsSystemWindows(window, false)
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content,
    )
}
