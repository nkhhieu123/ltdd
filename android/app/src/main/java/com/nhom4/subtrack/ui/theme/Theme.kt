package com.nhom4.subtrack.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Violet = Color(0xFF6C4CF1)
val Coral = Color(0xFFFF7A59)
val Mint = Color(0xFF1FA99C)
val Amber = Color(0xFFF2A900)
val SoonRed = Color(0xFFE5484D)
val MonthOrange = Color(0xFFF76B15)

private val LightColors = lightColorScheme(
    primary = Violet,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE6E0FF),
    onPrimaryContainer = Color(0xFF22135C),
    secondary = Mint,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFCDF2EC),
    onSecondaryContainer = Color(0xFF00382F),
    tertiary = Coral,
    onTertiary = Color.White,
    background = Color(0xFFF6F5FB),
    onBackground = Color(0xFF1F1A3D),
    surface = Color(0xFFF6F5FB),
    onSurface = Color(0xFF1F1A3D),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color.White,
    surfaceContainer = Color(0xFFEFEDF8),
    surfaceVariant = Color(0xFFE7E4F2),
    onSurfaceVariant = Color(0xFF5B5675),
    error = SoonRed,
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFB7A6FF),
    onPrimary = Color(0xFF2B1B5A),
    primaryContainer = Color(0xFF4A35B0),
    onPrimaryContainer = Color(0xFFE6E0FF),
    secondary = Color(0xFF6FD8C9),
    onSecondary = Color(0xFF00382F),
    tertiary = Color(0xFFFFB59F),
    background = Color(0xFF14111F),
    onBackground = Color(0xFFE7E3F5),
    surface = Color(0xFF14111F),
    onSurface = Color(0xFFE7E3F5),
    surfaceContainerLowest = Color(0xFF1D1930),
    surfaceContainerLow = Color(0xFF1D1930),
    surfaceContainer = Color(0xFF252038),
    surfaceVariant = Color(0xFF2E2944),
    onSurfaceVariant = Color(0xFFC6C0DA),
    error = Color(0xFFFF8A8E),
)

@Composable
fun SubTrackTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        content = content,
    )
}
