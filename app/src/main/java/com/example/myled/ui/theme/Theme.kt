package com.example.myled.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.myled.R

/** Палитра неоморфного дизайна (UI kit в Figma). */
@Immutable
data class NeuColors(
    val background: Color,
    val lightShadow: Color,
    val darkShadow: Color,
    val text: Color,
    val textMuted: Color,
    val accent: Color,
    val track: Color,
    val isDark: Boolean,
)

val LightNeuColors = NeuColors(
    background = Color(0xFFCDD2D4),
    lightShadow = Color(0xCCF3F6F7),
    darkShadow = Color(0x66868D91),
    text = Color(0xFF3F4145),
    textMuted = Color(0xFF6C7075),
    accent = Color(0xFFE78D59),
    track = Color(0xFFBFC4C7),
    isDark = false,
)

val DarkNeuColors = NeuColors(
    background = Color(0xFF3F4145),
    lightShadow = Color(0x40696D72),
    darkShadow = Color(0x99141516),
    text = Color(0xFFCDD2D4),
    textMuted = Color(0xFF9A9FA3),
    accent = Color(0xFFE78D59),
    track = Color(0xFF34363A),
    isDark = true,
)

val LocalNeuColors = staticCompositionLocalOf { LightNeuColors }

val Philosopher = FontFamily(
    Font(R.font.philosopher_regular, FontWeight.Normal),
    Font(R.font.philosopher_bold, FontWeight.Bold),
)

private fun typography(c: NeuColors): Typography {
    val base = TextStyle(fontFamily = Philosopher, color = c.text)
    return Typography(
        // H1 20
        titleLarge = base.copy(fontSize = 20.sp, fontWeight = FontWeight.Bold, lineHeight = 24.sp),
        // H2 18
        titleMedium = base.copy(fontSize = 18.sp, lineHeight = 22.sp),
        // H3 16
        bodyLarge = base.copy(fontSize = 16.sp, lineHeight = 20.sp),
        bodyMedium = base.copy(fontSize = 15.sp, lineHeight = 19.sp),
        labelLarge = base.copy(fontSize = 15.sp, lineHeight = 18.sp),
        labelMedium = base.copy(fontSize = 14.sp, lineHeight = 16.sp),
    )
}

object NeuTheme {
    val colors: NeuColors
        @Composable get() = LocalNeuColors.current
}

@Composable
fun MyLEDTheme(dark: Boolean = false, content: @Composable () -> Unit) {
    val c = if (dark) DarkNeuColors else LightNeuColors
    val scheme = if (dark) {
        darkColorScheme(primary = c.accent, background = c.background, surface = c.background, onSurface = c.text, onBackground = c.text)
    } else {
        lightColorScheme(primary = c.accent, background = c.background, surface = c.background, onSurface = c.text, onBackground = c.text)
    }
    CompositionLocalProvider(LocalNeuColors provides c) {
        MaterialTheme(colorScheme = scheme, typography = typography(c), content = content)
    }
}
