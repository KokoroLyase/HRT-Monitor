package com.hrt.monitor.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.hrt.monitor.data.Prefs

private val LightColors = lightColorScheme(
    primary = Color(0xFF7B4FBF),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFEDE0FF),
    onPrimaryContainer = Color(0xFF2E1A4F),
    secondary = Color(0xFFC2498F),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFFD9EC),
    onSecondaryContainer = Color(0xFF4A1738),
    tertiary = Color(0xFF2E9E8F),
    onTertiary = Color(0xFFFFFFFF),
    background = Color(0xFFFBF8FE),
    onBackground = Color(0xFF1C1B20),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1C1B20),
    surfaceVariant = Color(0xFFF0EBF7),
    onSurfaceVariant = Color(0xFF49454E),
    outline = Color(0xFF7A757F),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFD3B5FF),
    onPrimary = Color(0xFF42207C),
    primaryContainer = Color(0xFF5A3798),
    onPrimaryContainer = Color(0xFFEDE0FF),
    secondary = Color(0xFFFFB1D6),
    onSecondary = Color(0xFF5C1139),
    secondaryContainer = Color(0xFF7A2950),
    onSecondaryContainer = Color(0xFFFFD9EC),
    tertiary = Color(0xFF8CD8CC),
    onTertiary = Color(0xFF003731),
    background = Color(0xFF151119),
    onBackground = Color(0xFFE6E1E9),
    surface = Color(0xFF1C1821),
    onSurface = Color(0xFFE6E1E9),
    surfaceVariant = Color(0xFF49454E),
    onSurfaceVariant = Color(0xFFCBC4CF),
    outline = Color(0xFF948F99),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6)
)

@Composable
fun HrtTheme(content: @Composable () -> Unit) {
    val dark = when (UiState.themeMode.intValue) {
        Prefs.THEME_DARK -> true
        Prefs.THEME_LIGHT -> false
        else -> isSystemInDarkTheme()
    }
    MaterialTheme(colorScheme = if (dark) DarkColors else LightColors, content = content)
}
