package com.aura.launcher.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.aura.launcher.data.LauncherSettings

class Palette(val name: String, val c1: Color, val c2: Color, val c3: Color)

val Palettes = listOf(
    Palette("Bubblegum", Color(0xFFFF7EB6), Color(0xFFB388FF), Color(0xFF7FDBFF)),
    Palette("Sorbet", Color(0xFFFF9A76), Color(0xFFFF6F91), Color(0xFFFFD56B)),
    Palette("Mint Cream", Color(0xFF5EEAD4), Color(0xFF93C5FD), Color(0xFFFDE68A)),
    Palette("Dusk", Color(0xFF7C5CFF), Color(0xFFFF6FA5), Color(0xFFFFB27A)),
    Palette("Lagoon", Color(0xFF14B8A6), Color(0xFF3B82F6), Color(0xFFA7F3D0)),
    Palette("Ember", Color(0xFFFF7A45), Color(0xFFE64980), Color(0xFFFFD166)),
    Palette("Orchid", Color(0xFFC77DFF), Color(0xFFFF9CCB), Color(0xFF7AA2FF)),
    Palette("Sage", Color(0xFF5FBF93), Color(0xFFE9C46A), Color(0xFF8FB8FF)),
)

@Immutable
class AuraColors(
    val dark: Boolean,
    val text: Color,
    val textDim: Color,
    val base: Color,
    val surface: Color,
    val chip: Color,
    val glass: Brush,
    val glassBorder: Color,
    val palette: Palette,
) {
    val accent: Brush get() = Brush.linearGradient(listOf(palette.c1, palette.c2))
}

val LocalAura = staticCompositionLocalOf<AuraColors> { error("AuraTheme missing") }

@Composable
fun AuraTheme(s: LauncherSettings, content: @Composable () -> Unit) {
    val sysDark = isSystemInDarkTheme()
    val dark = s.systemWallpaper || s.themeMode == 1 || (s.themeMode == 0 && sysDark)
    val pal = Palettes[s.palette.coerceIn(Palettes.indices)]
    val colors = remember(dark, s.palette) {
        if (dark) AuraColors(
            dark = true,
            text = Color.White.copy(alpha = 0.95f),
            textDim = Color.White.copy(alpha = 0.62f),
            base = Color(0xFF0B0C16),
            surface = Color(0xFF181A2B),
            chip = Color.White.copy(alpha = 0.10f),
            glass = Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.17f), Color.White.copy(alpha = 0.07f))),
            glassBorder = Color.White.copy(alpha = 0.20f),
            palette = pal,
        ) else AuraColors(
            dark = false,
            text = Color(0xFF1D1B26),
            textDim = Color(0xFF6B6778),
            base = Color(0xFFF7F2EC),
            surface = Color(0xFFFFFBF7),
            chip = Color.Black.copy(alpha = 0.06f),
            glass = Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.78f), Color.White.copy(alpha = 0.48f))),
            glassBorder = Color.White.copy(alpha = 0.9f),
            palette = pal,
        )
    }
    CompositionLocalProvider(LocalAura provides colors) {
        MaterialTheme(
            colorScheme = if (dark) darkColorScheme(primary = pal.c1) else lightColorScheme(primary = pal.c1),
            content = content,
        )
    }
}
