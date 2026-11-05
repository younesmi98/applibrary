package com.nexus.app

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

object NexusColors {
    val Canvas = Color(0xFF0B0D10)
    val Surface = Color(0xFF14181C)
    val SurfaceHigh = Color(0xFF1C2127)
    val Border = Color(0xFF262D34)
    val TextPrimary = Color(0xFFEDEFF2)
    val TextSecondary = Color(0xFFA3ADB7)
    val Accent = Color(0xFF4FE0D0)
    val OnAccent = Color(0xFF04201D)
    val Danger = Color(0xFFFF8A80)
}

private val scheme = darkColorScheme(
    primary = NexusColors.Accent, onPrimary = NexusColors.OnAccent,
    background = NexusColors.Canvas, onBackground = NexusColors.TextPrimary,
    surface = NexusColors.Surface, onSurface = NexusColors.TextPrimary,
    surfaceVariant = NexusColors.SurfaceHigh, onSurfaceVariant = NexusColors.TextSecondary,
    outline = NexusColors.Border, error = NexusColors.Danger,
    secondaryContainer = NexusColors.SurfaceHigh, onSecondaryContainer = NexusColors.TextPrimary,
)

/** One explicit type scale. Line heights are generous so Persian script does not clip. */
private val type = Typography(
    headlineMedium = TextStyle(fontSize = 28.sp, lineHeight = 36.sp, fontWeight = FontWeight.SemiBold),
    titleLarge = TextStyle(fontSize = 20.sp, lineHeight = 28.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = TextStyle(fontSize = 16.sp, lineHeight = 24.sp, fontWeight = FontWeight.Medium),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 22.sp),
    bodySmall = TextStyle(fontSize = 12.sp, lineHeight = 18.sp),
    labelLarge = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium),
    labelMedium = TextStyle(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium),
)

@Composable
fun NexusTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = scheme, typography = type, content = content)
}
