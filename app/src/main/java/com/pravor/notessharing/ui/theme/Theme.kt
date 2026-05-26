package com.pravor.notessharing.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = ElectricBlue,
    onPrimary = Ink,
    primaryContainer = Color(0xFF173A5F),
    onPrimaryContainer = Color(0xFFE4F1FF),
    secondary = Mint,
    onSecondary = Ink,
    secondaryContainer = Color(0xFF16392F),
    onSecondaryContainer = Color(0xFFDEFFF0),
    tertiary = Coral,
    onTertiary = Ink,
    tertiaryContainer = Color(0xFF51241F),
    onTertiaryContainer = Color(0xFFFFE7E3),
    background = Ink,
    onBackground = Cloud,
    surface = Ink,
    onSurface = Cloud,
    surfaceVariant = PanelHigh,
    onSurfaceVariant = Muted,
    surfaceContainerLowest = Ink,
    surfaceContainerLow = Panel,
    surfaceContainer = Panel,
    surfaceContainerHigh = PanelHigh,
    surfaceContainerHighest = PanelHighest,
    outline = Line,
    outlineVariant = Color(0xFF222936),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF1A67B3),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD4E8FF),
    onPrimaryContainer = Color(0xFF001D35),
    secondary = Color(0xFF167356),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFC7F0DE),
    onSecondaryContainer = Color(0xFF002116),
    tertiary = Color(0xFFB04B3E),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFDAD4),
    onTertiaryContainer = Color(0xFF3F0300),
    background = Color(0xFFF7F9FC),
    onBackground = Color(0xFF11151D),
    surface = Color(0xFFF7F9FC),
    onSurface = Color(0xFF11151D),
    surfaceVariant = Color(0xFFE4EAF2),
    onSurfaceVariant = Color(0xFF536070),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF2F5F9),
    surfaceContainer = Color.White,
    surfaceContainerHigh = Color(0xFFEAF0F7),
    surfaceContainerHighest = Color(0xFFE0E7F0),
    outline = Color(0xFF748091),
    outlineVariant = Color(0xFFC4CCD7)
)

@Composable
fun NotesSharingTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                window.isNavigationBarContrastEnforced = false
            }
        }
    }

    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = Typography,
        content = content
    )
}
