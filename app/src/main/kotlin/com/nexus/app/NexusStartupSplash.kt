package com.nexus.app

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.padding

/**
 * The eight-spoke iOS-style activity indicator used by the younes.bond loader.
 * Each spoke fades in sequence instead of rotating a single Material spinner.
 * It is intentionally drawn on Canvas to avoid adding an animation dependency.
 */
@Composable
fun NexusEightSpokeSpinner(
    modifier: Modifier = Modifier,
    color: Color = NexusColors.Accent,
    reduceMotion: Boolean = FxState.reduceMotion,
) {
    val phase = if (reduceMotion) {
        0f
    } else {
        val transition = rememberInfiniteTransition(label = "nexus-eight-spoke")
        val animatedPhase by transition.animateFloat(
            initialValue = 0f,
            targetValue = 8f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 900, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
            label = "spoke-phase",
        )
        animatedPhase
    }

    Canvas(modifier = modifier.size(42.dp)) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = size.minDimension
        val innerRadius = radius * 0.24f
        val outerRadius = radius * 0.43f
        val strokeWidth = radius * 0.075f

        repeat(8) { index ->
            // Increasing phase moves the brightest spoke clockwise, as on the website loader.
            val trailPosition = ((index - phase + 8f) % 8f)
            val alpha = if (reduceMotion) {
                0.68f
            } else {
                (1f - trailPosition / 8f).coerceIn(0.16f, 1f)
            }
            val angle = Math.toRadians((-90.0 + index * 45.0))
            val directionX = kotlin.math.cos(angle).toFloat()
            val directionY = kotlin.math.sin(angle).toFloat()
            drawLine(
                color = color.copy(alpha = alpha),
                start = Offset(center.x + directionX * innerRadius, center.y + directionY * innerRadius),
                end = Offset(center.x + directionX * outerRadius, center.y + directionY * outerRadius),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round,
            )
        }
    }
}

@Composable
fun NexusStartupSplash(
    databaseError: Boolean,
    onRetry: () -> Unit,
) {
    Box(
        Modifier
            .fillMaxSize()
            .background(NexusColors.Canvas)
            .drawBehind {
                drawRect(
                    Brush.radialGradient(
                        colors = listOf(NexusColors.Accent.copy(alpha = 0.17f), Color.Transparent),
                        center = Offset(size.width * 0.5f, size.height * 0.42f),
                        radius = size.minDimension * 0.72f,
                    ),
                )
                drawRect(
                    Brush.verticalGradient(
                        0f to Color(0xFF101A15),
                        0.55f to Color.Transparent,
                        1f to Color(0xFF090D0B),
                    ),
                )
            }
            .windowInsetsPadding(WindowInsets.safeDrawing),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(82.dp)
                    .background(
                        Brush.linearGradient(listOf(NexusColors.SurfaceHigh, NexusColors.Deep)),
                        RoundedCornerShape(27.dp),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "N",
                    color = NexusColors.Accent,
                    fontSize = 56.sp,
                    lineHeight = 60.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(Modifier.height(18.dp))
            Text(
                text = "NEXUS",
                color = NexusColors.TextPrimary,
                fontSize = 25.sp,
                lineHeight = 30.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 6.sp,
            )
            Spacer(Modifier.height(5.dp))
            Text(
                text = tr("GAME LIBRARY", "کتابخانه‌ی بازی‌ها"),
                color = NexusColors.TextSecondary,
                fontSize = 11.sp,
                letterSpacing = 2.8.sp,
            )
            Spacer(Modifier.height(42.dp))

            if (databaseError) {
                Text(
                    text = tr(
                        "NEXUS couldn't open its local library. Your saved data has not been deleted.",
                        "NEXUS نتونست کتابخانه‌ی محلی رو باز کنه. اطلاعات ذخیره‌شده‌ات حذف نشده.",
                    ),
                    color = NexusColors.TextSecondary,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp,
                )
                Spacer(Modifier.height(16.dp))
                AccentButton(
                    text = tr("Try again", "تلاش دوباره"),
                    onClick = onRetry,
                    modifier = Modifier.size(width = 180.dp, height = 46.dp),
                )
            } else {
                NexusEightSpokeSpinner()
                Spacer(Modifier.height(18.dp))
                Text(
                    text = tr("Preparing your library…", "در حال آماده‌سازی کتابخانه…"),
                    color = NexusColors.TextSecondary,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
