package com.nexus.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Menu
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

    Scaffold(
        containerColor = NexusColors.Canvas,
        bottomBar = {
            if (isTab) NavigationBar(containerColor = NexusColors.Surface) {
                val items = listOf(
                    Triple(Screen.Home, tr("Home", "خانه"), Icons.Default.Home),
                    Triple(Screen.Library, tr("Library", "کتابخانه"), Icons.AutoMirrored.Filled.List),
                    Triple(Screen.Stats, tr("Stats", "آمار"), Icons.Default.DateRange),
                    Triple(Screen.More, tr("More", "بیشتر"), Icons.Default.Menu),
                )
                items.forEach { (target, label, icon) ->
                    NavigationBarItem(
                        selected = screen == target, onClick = { screen = target },
                        icon = { Icon(icon, null) }, label = { Text(label) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = NexusColors.OnAccent, selectedTextColor = NexusColors.Accent,
                            indicatorColor = NexusColors.Accent, unselectedIconColor = NexusColors.TextSecondary, unselectedTextColor = NexusColors.TextSecondary,
                        ),
                    )
                }
            }
        },
    ) { pad ->
        Box(Modifier.padding(pad).fillMaxSize()) {
            Crossfade(targetState = screen, label = "screen") { s ->
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
}
