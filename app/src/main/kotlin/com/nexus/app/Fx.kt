package com.nexus.app

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.PowerManager
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeChild
import com.nexus.polish.DeviceInfo
import com.nexus.polish.EffectLevel
import com.nexus.polish.Effects
import com.nexus.polish.EffectsPolicy

/** The person's visual-effects choice. Changes apply immediately because the values are Compose state. */
object FxState {
    var choice by mutableStateOf<EffectLevel?>(null)
    var reduceMotion by mutableStateOf(false)

    fun load(ctx: Context) {
        val p = ctx.getSharedPreferences("nexus_prefs", Context.MODE_PRIVATE)
        choice = p.getString("effects", null)?.let { runCatching { EffectLevel.valueOf(it) }.getOrNull() }
        reduceMotion = p.getBoolean("reduced_motion", false)
    }

    fun setChoice(ctx: Context, v: EffectLevel?) {
        choice = v; ctx.getSharedPreferences("nexus_prefs", Context.MODE_PRIVATE).edit().putString("effects", v?.name).apply()
    }

    fun setReduceMotion(ctx: Context, v: Boolean) {
        reduceMotion = v; ctx.getSharedPreferences("nexus_prefs", Context.MODE_PRIVATE).edit().putBoolean("reduced_motion", v).apply()
    }
}

@Composable
fun rememberEffects(): Effects {
    val ctx = LocalContext.current
    val choice = FxState.choice
    val reduce = FxState.reduceMotion
    return remember(choice, reduce) {
        val am = ctx.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val mem = ActivityManager.MemoryInfo().also { am.getMemoryInfo(it) }
        val pm = ctx.getSystemService(Context.POWER_SERVICE) as PowerManager
        val noAnim = android.provider.Settings.Global.getFloat(ctx.contentResolver, android.provider.Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
        EffectsPolicy.resolve(DeviceInfo((mem.totalMem / 1_048_576L).toInt(), pm.isPowerSaveMode, noAnim, choice, reduce))
    }
}

/** What glass surfaces need: the blur source (if any) and whether real blur is allowed on this device right now. */
class GlassEnv(val state: HazeState?, val blur: Boolean)

val LocalGlass = compositionLocalOf { GlassEnv(null, false) }
val LocalAnimations = compositionLocalOf { true }

/** Real blur needs Android 12 (API 31) and an effects level of High. Otherwise the surface is a denser frosted tint. */
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier, shape: Shape, tint: Color = Color(0xFF1E1E21), content: @Composable BoxScope.() -> Unit,
) {
    val env = LocalGlass.current
    val real = env.blur && env.state != null && Build.VERSION.SDK_INT >= 31
    val fill = if (real) tint.copy(alpha = 0.32f) else tint.copy(alpha = 0.92f)
    val edge = Brush.verticalGradient(listOf(Color(0x70FFFFFF), Color(0x14FFFFFF)))   // specular highlight on the top edge
    Box(
        modifier.then(if (real) Modifier.hazeChild(state = env.state!!, shape = shape) else Modifier.clip(shape))
            .background(fill, shape).border(1.dp, edge, shape),
        content = content,
    )
}

/** Press feedback: a quick spring shrink. Skipped when motion is reduced. */
fun Modifier.pressClick(enabled: Boolean = true, onClick: () -> Unit): Modifier = composed {
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val anim = LocalAnimations.current
    val scale by animateFloatAsState(if (pressed && anim) 0.95f else 1f, spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMedium), label = "press")
    this.graphicsLayer { scaleX = scale; scaleY = scale }.clickable(interactionSource = src, indication = null, enabled = enabled, onClick = onClick)
}
