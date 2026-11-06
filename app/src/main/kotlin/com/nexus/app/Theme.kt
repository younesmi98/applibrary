package com.nexus.app

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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
    primary = NexusColors.TextPrimary, onPrimary = NexusColors.Deep,
    background = NexusColors.Canvas, onBackground = NexusColors.TextPrimary,
    surface = NexusColors.Surface, onSurface = NexusColors.TextPrimary,
    surfaceVariant = NexusColors.SurfaceHigh, onSurfaceVariant = NexusColors.TextSecondary,
    surfaceContainerHigh = NexusColors.SurfaceHigh, surfaceContainer = NexusColors.Surface,
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
val MonoStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp, lineHeight = 16.sp, color = NexusColors.Muted)

@Composable
fun NexusTheme(content: @Composable () -> Unit) {
    val fa = I18n.fa
    val typo = remember(fa) { typography(if (fa) Fonts.vazirmatn else Fonts.inter, fa) }
    MaterialTheme(colorScheme = scheme, typography = typo, shapes = shapes, content = content)
}
