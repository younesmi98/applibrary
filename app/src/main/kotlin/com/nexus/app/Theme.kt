package com.nexus.app

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
<<<<<<< HEAD
import androidx.compose.ui.text.font.FontFamily
=======
>>>>>>> 7d946ca (NEXUS 0.3.0 redesign and release preparation)
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

<<<<<<< HEAD
/** Palette taken from the NEXUS design sheet: graphite base, one teal accent, glass only on floating layers. */
object NexusColors {
    val Canvas = Color(0xFF141415)
    val Deep = Color(0xFF0F0F10)
    val Surface = Color(0xFF19191B)
    val SurfaceHigh = Color(0xFF232325)
    val Border = Color(0x14EDEDED)
    val Glass = Color(0x0FFFFFFF)
    val GlassBorder = Color(0x1AFFFFFF)
    val TextPrimary = Color(0xFFEDEDED)
    val TextSecondary = Color(0xFFA1A1A6)
    val Muted = Color(0xFF85858B)
    val Accent = Color(0xFF3CC8A8)
    val OnAccent = Color(0xFF0F0F10)
    val Danger = Color(0xFFFF8A80)
}

private val scheme = darkColorScheme(
=======
/**
 * Graphite and deep black with one emerald accent. Backgrounds and the accent read app preferences, so the
 * Appearance screen changes them live. Button fills are a darker emerald so white text stays readable.
 */
object NexusColors {
    val Canvas: Color get() = if (AppPrefs.deepBlack) Color(0xFF070A09) else Color(0xFF0D1110)
    val Deep: Color get() = Color(0xFF0A0E0C)
    val Surface: Color get() = Color(0xFF141A17)
    val SurfaceHigh: Color get() = Color(0xFF1B2420)
    val Card: Color get() = Color(0xFF202A25)
    val Border: Color get() = Color(0xFF29332D)
    val Glass: Color get() = Color(0x14FFFFFF)
    val GlassBorder: Color get() = Color(0x24FFFFFF)
    val TextPrimary: Color get() = Color(0xFFF2F5F3)
    val TextSecondary: Color get() = Color(0xFFA3ADA8)
    val Muted: Color get() = Color(0xFF8A9690)
    val Accent: Color get() = AppPrefs.accents[AppPrefs.accent]
    val AccentDark: Color get() = androidx.compose.ui.graphics.lerp(AppPrefs.accents[AppPrefs.accent], Color.Black, 0.55f)
    val AccentMid: Color get() = androidx.compose.ui.graphics.lerp(AppPrefs.accents[AppPrefs.accent], Color.Black, 0.38f)
    val OnAccent: Color get() = Color.White
    val Danger: Color get() = Color(0xFFFF626D)
    val Warning: Color get() = Color(0xFFFFC15A)
}

private fun scheme() = darkColorScheme(
>>>>>>> 7d946ca (NEXUS 0.3.0 redesign and release preparation)
    primary = NexusColors.TextPrimary, onPrimary = NexusColors.Deep,
    background = NexusColors.Canvas, onBackground = NexusColors.TextPrimary,
    surface = NexusColors.Surface, onSurface = NexusColors.TextPrimary,
    surfaceVariant = NexusColors.SurfaceHigh, onSurfaceVariant = NexusColors.TextSecondary,
<<<<<<< HEAD
    surfaceContainerHigh = NexusColors.SurfaceHigh, surfaceContainer = NexusColors.Surface,
=======
>>>>>>> 7d946ca (NEXUS 0.3.0 redesign and release preparation)
    outline = NexusColors.GlassBorder, outlineVariant = NexusColors.Border, error = NexusColors.Danger,
    secondaryContainer = NexusColors.SurfaceHigh, onSecondaryContainer = NexusColors.TextPrimary,
)

private val shapes = Shapes(
    extraSmall = RoundedCornerShape(12.dp), small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(14.dp), large = RoundedCornerShape(16.dp), extraLarge = RoundedCornerShape(20.dp),
)

/**
 * Large-title scale. English uses Inter, Persian uses Vazirmatn (both bundled). Persian lines are a little taller
 * so dots and tails are never clipped.
 */
private fun typography(f: FontFamily, fa: Boolean): Typography {
    val x = if (fa) 3 else 0
    fun st(size: Int, line: Int, w: FontWeight) = TextStyle(fontFamily = f, fontSize = size.sp, lineHeight = (line + x).sp, fontWeight = w)
    return Typography(
        headlineMedium = st(30, 38, FontWeight.Bold), titleLarge = st(20, 28, FontWeight.SemiBold), titleMedium = st(16, 24, FontWeight.Medium),
        bodyLarge = st(16, 24, FontWeight.Normal), bodyMedium = st(14, 21, FontWeight.Normal), bodySmall = st(12, 18, FontWeight.Normal),
        labelLarge = st(14, 20, FontWeight.Medium), labelMedium = st(12, 16, FontWeight.Medium),
    )
}

/** Tabular, fixed-width style for timers and small figures. */
<<<<<<< HEAD
val MonoStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp, lineHeight = 16.sp, color = NexusColors.Muted)
=======
val MonoStyle: TextStyle get() = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp, lineHeight = 16.sp, color = NexusColors.Muted)
>>>>>>> 7d946ca (NEXUS 0.3.0 redesign and release preparation)

@Composable
fun NexusTheme(content: @Composable () -> Unit) {
    val fa = I18n.fa
    val typo = remember(fa) { typography(if (fa) Fonts.vazirmatn else Fonts.inter, fa) }
<<<<<<< HEAD
    MaterialTheme(colorScheme = scheme, typography = typo, shapes = shapes, content = content)
=======
    // Text outside a Surface used to fall back to black on the dark pages; this sets the default content colour.
    MaterialTheme(colorScheme = scheme(), typography = typo, shapes = shapes) {
        androidx.compose.runtime.CompositionLocalProvider(LocalContentColor provides NexusColors.TextPrimary) {
            ProvideTextStyle(typo.bodyLarge, content)
        }
    }
>>>>>>> 7d946ca (NEXUS 0.3.0 redesign and release preparation)
}
