package com.nexus.app

import android.os.Bundle
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
<<<<<<< HEAD
import androidx.compose.animation.AnimatedContent
import androidx.compose.ui.graphics.graphicsLayer
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.fillMaxHeight
=======
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.animation.AnimatedContent
>>>>>>> 7d946ca (NEXUS 0.3.0 redesign and release preparation)
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
<<<<<<< HEAD
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
=======
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
>>>>>>> 7d946ca (NEXUS 0.3.0 redesign and release preparation)
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

sealed interface Screen {
    data object Home : Screen
    data object Library : Screen
    data object Stats : Screen
    data object More : Screen
    data object Add : Screen
    data object AddManual : Screen
    data object Connections : Screen
    data object Profile : Screen
    data object LibraryPrefs : Screen
    data object Appearance : Screen
    data object About : Screen
    data object Changelog : Screen
    data object Licenses : Screen
    data object PlayNext : Screen
    data object Backlog : Screen
    data object Collections : Screen
    data object Wishlist : Screen
    data object Journal : Screen
    data object Import : Screen
    data object Backup : Screen
    data class Detail(val id: String) : Screen
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Android 15/16 edge-to-edge: draw the graphite canvas under transparent system bars,
        // then let Compose keep interactive content inside the safe drawing area.
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            window.isStatusBarContrastEnforced = false
            window.isNavigationBarContrastEnforced = false
        }
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }
        setContent {
            // Persian flips the whole layout to right-to-left.
            CompositionLocalProvider(LocalLayoutDirection provides if (I18n.fa) LayoutDirection.Rtl else LayoutDirection.Ltr) {
                NexusTheme { NexusRoot() }
            }
        }
    }
}

private val tabs = listOf(Screen.Home, Screen.Library, Screen.Stats, Screen.More)

private fun Screen.depth(): Int = if (this in tabs) 0 else 1

/** Soft emerald glow at the top of every page. The gradients end on their own, so there is no visible edge. */
private fun Modifier.pageBackground(): Modifier = this.background(NexusColors.Canvas).drawBehind {
    drawRect(Brush.radialGradient(listOf(NexusColors.Accent.copy(alpha = 0.16f), Color.Transparent), Offset(size.width * 0.1f, 0f), size.width * 0.95f))
    drawRect(Brush.radialGradient(listOf(NexusColors.Accent.copy(alpha = 0.07f), Color.Transparent), Offset(size.width, size.height * 0.08f), size.width * 0.8f))
}

@Composable
private fun FloatingNav(items: List<Triple<Screen, String, ImageVector>>, selected: Screen, onSelect: (Screen) -> Unit, modifier: Modifier) {
    val idx = items.indexOfFirst { it.first == selected }.coerceAtLeast(0)
    val anim = LocalAnimations.current
    GlassSurface(modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 12.dp).height(64.dp), RoundedCornerShape(32.dp), NexusColors.Surface) {
        androidx.compose.foundation.layout.BoxWithConstraints(Modifier.fillMaxSize().padding(5.dp)) {
            val w = maxWidth / items.size
            val x by androidx.compose.animation.core.animateDpAsState(w * idx, if (anim) spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessMedium) else androidx.compose.animation.core.snap(), label = "tab")
            Box(
                Modifier.offset(x = x).width(w).fillMaxHeight().clip(RoundedCornerShape(26.dp))
                    .background(NexusColors.Accent.copy(alpha = 0.18f)).border(1.dp, NexusColors.Accent.copy(alpha = 0.45f), RoundedCornerShape(26.dp)),
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

    // Show the NEXUS eight-spoke splash while Room opens/migrates the on-device library.
    // The short minimum display time prevents a one-frame flash on fast devices.
    var startupReady by remember { mutableStateOf(false) }
    var startupError by remember { mutableStateOf(false) }
    var startupAttempt by remember { mutableIntStateOf(0) }
    LaunchedEffect(startupAttempt) {
        startupError = false
        val startedAt = SystemClock.elapsedRealtime()
        val opened = withContext(Dispatchers.IO) {
            runCatching { app.db.openHelper.writableDatabase.version }.isSuccess
        }
        val minDisplayMs = 850L
        val remaining = minDisplayMs - (SystemClock.elapsedRealtime() - startedAt)
        if (remaining > 0) delay(remaining)
        if (opened) startupReady = true else startupError = true
    }
    if (!startupReady) {
        NexusStartupSplash(databaseError = startupError, onRetry = {
            startupError = false
            startupAttempt += 1
        })
        return
    }

    val factory = remember { NexusVmFactory(app) }

    // A real back stack: Back returns to where the person came from. Tabs reset it.
    val stack = remember { mutableStateListOf<Screen>(Screen.Home) }
    val screen = stack.last()
    fun go(s: Screen) { stack.add(s) }
    fun tab(s: Screen) { stack.clear(); stack.add(s) }
    fun back() { if (stack.size > 1) stack.removeAt(stack.lastIndex) }
    BackHandler(enabled = stack.size > 1) { back() }
    val open: (String) -> Unit = { go(Screen.Detail(it)) }
    val isTab = screen in tabs

    // Optional sync when the app opens: only with Steam connected and a stale last sync.
    LaunchedEffect(Unit) {
        if (!AppPrefs.autoSync) return@LaunchedEffect
        val connected = withContext(Dispatchers.IO) { app.platform.isConnected() }
        val last = app.db.insights().accountOnce("STEAM")?.lastSyncAt
        if (connected && (last == null || System.currentTimeMillis() - last > 6 * 3600_000L)) app.platform.sync()
    }

    val fx = rememberEffects()
    val hazeState = remember { HazeState() }
    val animate = fx.animations
    CompositionLocalProvider(LocalGlass provides GlassEnv(hazeState, fx.blur), LocalAnimations provides animate) {
<<<<<<< HEAD
        Box(Modifier.fillMaxSize()) {
            // Everything inside this box is what the tab bar blurs.
            Box(Modifier.fillMaxSize().haze(state = hazeState)) {
=======
        Box(Modifier.fillMaxSize().background(NexusColors.Canvas)) {
            Box(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
                // Everything inside this box is what the tab bar blurs.
                Box(Modifier.fillMaxSize().haze(state = hazeState)) {
>>>>>>> 7d946ca (NEXUS 0.3.0 redesign and release preparation)
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
<<<<<<< HEAD
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
=======
                            Screen.Home -> {
                                val vm: HomeViewModel = viewModel(factory = factory)
                                HomeScreen(app, vm, open, onSearch = { tab(Screen.Library) }, onConnections = { go(Screen.Connections) }, onAdd = { go(Screen.Add) }, onLibrary = { tab(Screen.Library) })
                            }
                            Screen.Library -> {
                                val vm: LibraryViewModel = viewModel(factory = factory)
                                LibraryScreen(vm, open, onAdd = { go(Screen.Add) }, onImport = { go(Screen.Import) }, onConnections = { go(Screen.Connections) })
                            }
                            Screen.Stats -> StatsScreen(app)
                            Screen.More -> MoreScreen(app, go = { go(it) })
                            Screen.Add -> AddChooserScreen(app, ::back, onConnections = { go(Screen.Connections) }, onImport = { go(Screen.Import) }, onManual = { go(Screen.AddManual) }, onDone = { tab(Screen.Library) })
                            Screen.AddManual -> {
                                val vm: AddGameViewModel = viewModel(factory = factory)
                                AddGameScreen(vm, ::back, onSaved = { id -> back(); back(); go(Screen.Detail(id)) })
                            }
                            Screen.Connections -> ConnectionsScreen(app, ::back, onImport = { go(Screen.Import) })
                            Screen.Profile -> ProfileScreen(::back)
                            Screen.LibraryPrefs -> LibraryPrefsScreen(::back)
                            Screen.Appearance -> AppearanceScreen(::back)
                            Screen.About -> AboutScreen(::back, go = { go(it) })
                            Screen.Changelog -> ChangelogScreen(::back)
                            Screen.Licenses -> LicensesScreen(::back)
                            Screen.PlayNext -> PlayNextScreen(app, ::back, open)
                            Screen.Backlog -> BacklogScreen(app, ::back, open)
                            Screen.Collections -> CollectionsScreen(app, ::back)
                            Screen.Wishlist -> WishlistScreen(app, ::back)
                            Screen.Journal -> JournalScreen(app, ::back, open)
                            Screen.Import -> ImportScreen(app, ::back)
                            Screen.Backup -> BackupScreen(app, ::back)
                            is Screen.Detail -> {
                                val vm: DetailViewModel = viewModel(key = s.id, factory = NexusVmFactory(app, s.id))
                                DetailScreen(vm, onBack = ::back)
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
                ), screen, { tab(it) }, Modifier.align(Alignment.BottomCenter),
            )
>>>>>>> 7d946ca (NEXUS 0.3.0 redesign and release preparation)
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
