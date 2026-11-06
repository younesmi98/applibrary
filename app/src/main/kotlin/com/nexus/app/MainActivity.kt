package com.nexus.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.ui.graphics.graphicsLayer
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nexus.playtime.RecoveryChoice

sealed interface Screen {
    data object Home : Screen
    data object Library : Screen
    data object Stats : Screen
    data object More : Screen
    data object Add : Screen
    data object Settings : Screen
    data object Wrapped : Screen
    data object PlayNext : Screen
    data object Backlog : Screen
    data object Collections : Screen
    data object Wishlist : Screen
    data object Journal : Screen
    data object Import : Screen
    data object Backup : Screen
    data object Devices : Screen
    data class Detail(val id: String) : Screen
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // Persian flips the whole layout to right-to-left.
            androidx.compose.runtime.CompositionLocalProvider(
                LocalLayoutDirection provides if (I18n.fa) LayoutDirection.Rtl else LayoutDirection.Ltr,
            ) { NexusTheme { NexusRoot() } }
        }
    }
}

private fun Screen.depth(): Int = when (this) {
    Screen.Home, Screen.Library, Screen.Stats, Screen.More -> 0
    else -> 1
}

/** Soft teal and blue glow at the top of every page, as in the design sheet. */
private fun Modifier.pageBackground(): Modifier = this.background(NexusColors.Canvas).drawBehind {
    val h = 300.dp.toPx()
    drawRect(Brush.radialGradient(listOf(Color(0x423CC8A8), Color.Transparent), Offset(size.width * 0.15f, 0f), size.width * 1.1f), size = Size(size.width, h))
    drawRect(Brush.radialGradient(listOf(Color(0x2E5A82C8), Color.Transparent), Offset(size.width, h * 0.1f), size.width * 0.9f), size = Size(size.width, h))
}

/** Floating tab bar: real blur when allowed, a sliding teal indicator on a spring, icon scale on the chosen tab. */
@Composable
private fun FloatingNav(items: List<Triple<Screen, String, ImageVector>>, selected: Screen, onSelect: (Screen) -> Unit, modifier: Modifier) {
    val idx = items.indexOfFirst { it.first == selected }.coerceAtLeast(0)
    val anim = LocalAnimations.current
    GlassSurface(modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 12.dp).height(64.dp), RoundedCornerShape(32.dp), Color(0xFF1E1E21)) {
        androidx.compose.foundation.layout.BoxWithConstraints(Modifier.fillMaxSize().padding(5.dp)) {
            val w = maxWidth / items.size
            val x by androidx.compose.animation.core.animateDpAsState(w * idx, if (anim) spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessMedium) else androidx.compose.animation.core.snap(), label = "tab")
            Box(
                Modifier.offset(x = x).width(w).fillMaxHeight().clip(RoundedCornerShape(26.dp))
                    .background(Color(0x2E3CC8A8)).border(1.dp, Color(0x593CC8A8), RoundedCornerShape(26.dp)),
            )
            Row(Modifier.fillMaxSize()) {
                items.forEachIndexed { i, (target, label, icon) ->
                    val on = i == idx
                    val scale by androidx.compose.animation.core.animateFloatAsState(if (on && anim) 1.12f else 1f, spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMedium), label = "scale")
                    val tint by androidx.compose.animation.animateColorAsState(if (on) NexusColors.Accent else NexusColors.Muted, label = "tint")
                    Column(
                        Modifier.weight(1f).fillMaxHeight().selectable(selected = on, role = Role.Tab, onClick = { onSelect(target) }),
                        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
                    ) {
                        Icon(icon, null, Modifier.size(22.dp).graphicsLayer { scaleX = scale; scaleY = scale }, tint = tint)
                        Text(label, fontSize = 11.sp, lineHeight = 14.sp, color = if (on) NexusColors.TextPrimary else NexusColors.Muted, maxLines = 1)
                    }
                }
            }
        }
    }
}

@Composable
fun NexusRoot() {
    val ctx = LocalContext.current
    val app = ctx.nexus
    var screen by remember { mutableStateOf<Screen>(Screen.Home) }
    val factory = remember { NexusVmFactory(app) }
    val tabs = listOf(Screen.Home, Screen.Library, Screen.Stats, Screen.More)
    val isTab = screen in tabs

    BackHandler(enabled = screen != Screen.Home) {
        screen = when (screen) {
            Screen.Library, Screen.Stats, Screen.More -> Screen.Home
            is Screen.Detail, Screen.Add -> Screen.Library
            else -> Screen.More
        }
    }
    val toMore = { screen = Screen.More }
    val open: (String) -> Unit = { screen = Screen.Detail(it) }

    // Unfinished sessions found on cold start: keep running, finish now, or discard.
    val rvm: RecoveryViewModel = viewModel(factory = factory)
    LaunchedEffect(Unit) { rvm.check() }
    val interrupted by rvm.interrupted.collectAsStateWithLifecycle()
    interrupted.firstOrNull()?.let { (s, title) ->
        AlertDialog(
            onDismissRequest = {},
            title = { Text(tr("Unfinished session", "جلسه‌ی نیمه‌تمام")) },
            text = { Text(tr("$title has a session started ${formatDate(s.startedAt)} that was never finished. What should happen to it?", "برای «$title» جلسه‌ای هست که از ${formatDate(s.startedAt)} شروع شده و تموم نشده. باهاش چی کنیم؟")) },
            confirmButton = { TextButton(onClick = { rvm.choose(s.id, RecoveryChoice.RESUME, ctx) }) { Text(tr("Keep running", "ادامه بده")) } },
            dismissButton = {
                Row {
                    TextButton(onClick = { rvm.choose(s.id, RecoveryChoice.COMPLETE, ctx) }) { Text(tr("Finish now", "همین حالا تموم کن")) }
                    TextButton(onClick = { rvm.choose(s.id, RecoveryChoice.DISCARD, ctx) }) { Text(tr("Discard", "دور بینداز")) }
                }
            },
        )
    }

    val fx = rememberEffects()
    val hazeState = remember { HazeState() }
    val animate = fx.animations
    CompositionLocalProvider(LocalGlass provides GlassEnv(hazeState, fx.blur), LocalAnimations provides animate) {
        Box(Modifier.fillMaxSize()) {
            // Everything inside this box is what the tab bar blurs.
            Box(Modifier.fillMaxSize().haze(state = hazeState)) {
                AnimatedContent(
                    targetState = screen, label = "screen",
                    transitionSpec = {
                        val d = targetState.depth() - initialState.depth()
                        val sign = if (I18n.fa) -1 else 1
                        val sp = spring<androidx.compose.ui.unit.IntOffset>(dampingRatio = 0.9f, stiffness = Spring.StiffnessMediumLow)
                        when {
                            !animate -> fadeIn(tween(0)) togetherWith fadeOut(tween(0))
                            d > 0 -> (slideInHorizontally(sp) { sign * it / 3 } + fadeIn(tween(220))) togetherWith (slideOutHorizontally(sp) { -sign * it / 6 } + fadeOut(tween(180)))
                            d < 0 -> (slideInHorizontally(sp) { -sign * it / 6 } + fadeIn(tween(220))) togetherWith (slideOutHorizontally(sp) { sign * it / 3 } + fadeOut(tween(180)))
                            else -> fadeIn(tween(180)) togetherWith fadeOut(tween(120))
                        }
                    },
                ) { s ->
                    Box(Modifier.fillMaxSize().pageBackground()) {
                        when (s) {
                            Screen.Home -> HomeScreen(app, open, onAdd = { screen = Screen.Add }, onImport = { screen = Screen.Import })
                            Screen.Library -> {
                                val vm: LibraryViewModel = viewModel(factory = factory)
                                LibraryScreen(vm, open, onAdd = { screen = Screen.Add }, onImport = { screen = Screen.Import })
                            }
                            Screen.Stats -> StatsScreen(app)
                            Screen.More -> MoreScreen(go = { screen = it })
                            Screen.Add -> {
                                val vm: AddGameViewModel = viewModel(factory = factory)
                                AddGameScreen(vm, onBack = { screen = Screen.Library }, onSaved = { screen = Screen.Detail(it) })
                            }
                            Screen.Settings -> SettingsScreen(onBack = toMore)
                            Screen.Wrapped -> WrappedScreen(app, toMore)
                            Screen.PlayNext -> PlayNextScreen(app, toMore, open)
                            Screen.Backlog -> BacklogScreen(app, toMore, open)
                            Screen.Collections -> CollectionsScreen(app, toMore)
                            Screen.Wishlist -> WishlistScreen(app, toMore)
                            Screen.Journal -> JournalScreen(app, toMore, open)
                            Screen.Import -> ImportScreen(app, toMore)
                            Screen.Backup -> BackupScreen(app, toMore)
                            Screen.Devices -> DevicesScreen(app, toMore)
                            is Screen.Detail -> {
                                val vm: DetailViewModel = viewModel(key = s.id, factory = NexusVmFactory(app, s.id))
                                DetailScreen(vm, onBack = { screen = Screen.Library })
                            }
                        }
                    }
                }
            }
            if (isTab) FloatingNav(
                listOf(
                    Triple(Screen.Home, tr("Home", "خانه"), NexusIcons.Home),
                    Triple(Screen.Library, tr("Library", "کتابخانه"), NexusIcons.Library),
                    Triple(Screen.Stats, tr("Stats", "آمار"), NexusIcons.Stats),
                    Triple(Screen.More, tr("More", "بیشتر"), NexusIcons.More),
                ), screen, { screen = it }, Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}
