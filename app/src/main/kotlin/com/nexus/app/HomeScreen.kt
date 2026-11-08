package com.nexus.app

import androidx.compose.foundation.background
<<<<<<< HEAD
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
=======
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
>>>>>>> 7d946ca (NEXUS 0.3.0 redesign and release preparation)
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
<<<<<<< HEAD
import androidx.compose.ui.platform.LocalContext
=======
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
>>>>>>> 7d946ca (NEXUS 0.3.0 redesign and release preparation)
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nexus.core.database.SourceType
import kotlinx.coroutines.delay
<<<<<<< HEAD
import kotlinx.coroutines.launch
import java.time.LocalDate

@Composable
fun HomeScreen(app: NexusApp, onOpen: (String) -> Unit, onAdd: () -> Unit, onImport: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val snap by produceState<InsightsSnapshot?>(null) { value = InsightsLoader.load(app.db) }
    val live by produceState<ActiveSession?>(null) {
        while (true) {
            val s = app.db.sessions().unfinished().firstOrNull()
            value = s?.let { ActiveSession(it, app.db.sessions().pauses(it.id)) }
            delay(2000)
        }
=======
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.time.LocalDate
import java.util.Date

@Composable
fun HomeScreen(
    app: NexusApp, vm: HomeViewModel, onOpen: (String) -> Unit, onSearch: () -> Unit,
    onConnections: () -> Unit, onAdd: () -> Unit, onLibrary: () -> Unit,
) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val syncing by app.platform.syncing.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val connected = (ui as? HomeUi.Ready)?.s?.account?.accountId != null

    // While Home is open and Steam is connected, ask Steam once a minute whether a game is running.
    LaunchedEffect(connected) {
        if (connected) while (isActive) { app.platform.refreshPresence(); delay(60_000) }
>>>>>>> 7d946ca (NEXUS 0.3.0 redesign and release preparation)
    }

<<<<<<< HEAD
    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp).verticalScroll(androidx.compose.foundation.rememberScrollState()), verticalArrangement = Arrangement.spacedBy(24.dp)) {
        PageTitle(tr("Home", "خانه"), formatDayLong(LocalDate.now()))
        val s = snap
        if (s == null) { HomeSkeleton(); return@Column }

        live?.let { l ->
            val paused = l.session.state == SessionState.PAUSED
            Section(tr("Playing now", "الان در حال بازی")) {
                Panel {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.clickable { onOpen(l.session.gameId) }) {
                        Box(Modifier.size(8.dp).clip(CircleShape).background(if (paused) NexusColors.Muted else NexusColors.Accent))
                        Column(Modifier.weight(1f)) {
                            Text(s.titles[l.session.gameId] ?: tr("Game", "بازی"), style = MaterialTheme.typography.titleMedium)
                            Text(tr("Manual session", "جلسه‌ی دستی"), style = MonoStyle)
                        }
                        Text(formatClock(PlaytimeCalc.activeMillis(l.session, l.pauses, tick)), style = MaterialTheme.typography.titleMedium)
                    }
                    Row(Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        GlassButton(if (paused) tr("Resume", "ادامه") else tr("Pause", "توقف"), {
                            scope.launch {
                                if (paused) app.sessions.resume(l.session.id) else app.sessions.pause(l.session.id)
                                SessionNotifier.push(ctx, l.session.id)
                            }
                        }, Modifier.weight(1f))
                        GlassButton(tr("Finish", "پایان"), {
                            scope.launch { app.sessions.finish(l.session.id); SessionNotifier.stop(ctx) }
                        }, Modifier.weight(1f))
                    }
                }
=======
    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(24.dp)) {
        val ready = ui as? HomeUi.Ready
        val acc = ready?.s?.account
        // ---- header: local profile, search, sync
        Row(Modifier.padding(top = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Avatar(AppPrefs.avatarUri ?: acc?.avatarUrl, AppPrefs.profileName.ifBlank { acc?.displayName ?: "N" }, 48)
            Column(Modifier.weight(1f)) {
                Text(AppPrefs.profileName.ifBlank { acc?.displayName ?: "NEXUS" }, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(formatDayLong(LocalDate.now()), style = MaterialTheme.typography.bodySmall, color = NexusColors.TextSecondary)
>>>>>>> 7d946ca (NEXUS 0.3.0 redesign and release preparation)
            }
            GlassIconButton(Icons.Default.Search, tr("Search", "جستجو"), onSearch)
            if (syncing) Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp, color = NexusColors.Accent) }
            else GlassIconButton(Icons.Default.Refresh, tr("Sync", "همگام‌سازی")) { if (connected) scope.launch { app.platform.sync() } else onConnections() }
        }

<<<<<<< HEAD
        if (s.games.isEmpty()) {
            EmptyState(
                tr("Your library is empty", "کتابخانه‌ات خالیه"),
                tr("Add a game by hand, or import a list from a file.", "یه بازی رو دستی اضافه کن یا فهرست بازی‌ها رو از فایل وارد کن."),
            ) {
                Button(onClick = onAdd, Modifier.height(44.dp)) { Text(tr("Add a game", "افزودن بازی")) }
                OutlinedButton(onClick = onImport, Modifier.height(44.dp)) { Text(tr("Import from file", "ورود از فایل")) }
            }
            return@Column
        }

        val played = s.games.filter { it.lastPlayedAt != null }.sortedByDescending { it.lastPlayedAt }
        val playing = s.games.filter { it.status == Status.PLAYING }
        PosterRow(tr("Recently played", "اخیراً بازی‌شده"), (played + playing).distinctBy { it.id }.take(3), s, onOpen)
        PosterRow(tr("Your backlog", "صف بازی‌های تو"), s.games.filter { it.status == Status.BACKLOG || it.status == Status.WANT }.sortedByDescending { it.addedAt }.take(3), s, onOpen)
        PosterRow(tr("Favorites", "موردعلاقه‌ها"), s.games.filter { it.favorite }.take(3), s, onOpen)
        Spacer(Modifier.height(120.dp))
    }
}

/** Three posters per row with a small figure underneath, like the design sheet. */
@Composable
private fun PosterRow(title: String, games: List<GameFacts>, s: InsightsSnapshot, onOpen: (String) -> Unit) {
    if (games.isEmpty()) return
    Section(title) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            games.forEach { g ->
                Column(Modifier.weight(1f).clickable { onOpen(g.id) }, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Cover(s.covers[g.id], g.title, Modifier.fillMaxWidth().aspectRatio(2f / 3f))
                    val tracked = g.pcMinutes + g.manualMinutes
                    Text(if (tracked > 0) formatDuration(tracked * 60_000L) else g.status.label(), style = MonoStyle)
=======
        when (val u = ui) {
            HomeUi.Loading -> HomeSkeleton()
            is HomeUi.Ready -> {
                val s = u.s
                if (s.overview.total == 0) {
                    EmptyState(
                        tr("Your library is empty", "کتابخانه‌ات خالیه"),
                        if (connected) tr("Sync again, or add a game by hand.", "دوباره همگام‌سازی کن یا یه بازی رو دستی اضافه کن.")
                        else tr("Connect Steam to bring your games in, or add one by hand.", "برای آوردن بازی‌هات استیم رو وصل کن یا یه بازی رو دستی اضافه کن."),
                    ) {
                        AccentButton(tr("Connect platforms", "اتصال پلتفرم‌ها"), onConnections)
                        GlassButton(tr("Add a game", "افزودن بازی"), onAdd)
                    }
                } else {
                    SyncStrip(s.account, syncing, onConnections)
                    s.nowPlaying?.let { NowPlayingCard(it, onOpen) }
                    CardRow(tr("Recently played", "اخیراً بازی‌شده"), s.recent, onOpen, tr("Open Library", "کتابخانه"), onLibrary)
                    Overview(s.overview)
                    CardRow(tr("Favorites", "موردعلاقه‌ها"), s.favorites, onOpen)
                    CardRow(tr("Continue exploring", "ادامه‌ی گشتن"), s.explore, onOpen)
>>>>>>> 7d946ca (NEXUS 0.3.0 redesign and release preparation)
                }
            }
            repeat(3 - games.size) { Spacer(Modifier.weight(1f)) }
        }
        Spacer(Modifier.height(120.dp))
    }
}

@Composable
fun Avatar(model: String?, name: String, size: Int) {
    Box(Modifier.size(size.dp).clip(CircleShape).background(NexusColors.Card).border(1.5.dp, NexusColors.Accent.copy(alpha = 0.7f), CircleShape), contentAlignment = Alignment.Center) {
        Text(name.trim().take(1).uppercase(), style = MaterialTheme.typography.titleMedium, color = NexusColors.TextSecondary)
        if (model != null) coil.compose.AsyncImage(model = model, contentDescription = null, modifier = Modifier.matchParentSize(), contentScale = androidx.compose.ui.layout.ContentScale.Crop)
    }
}

/** One quiet line about the platform connection: when it last synced, or what went wrong. */
@Composable
private fun SyncStrip(acc: com.nexus.core.database.PlatformAccountEntity?, syncing: Boolean, onConnections: () -> Unit) {
    if (acc?.accountId == null) {
        Panel(Modifier.clickable(onClick = onConnections)) {
            Text(tr("No platform connected", "هیچ پلتفرمی وصل نیست"), style = MaterialTheme.typography.titleMedium)
            Hint(tr("Connect Steam to see real playtime and what you are playing.", "استیم رو وصل کن تا ساعت بازی واقعی و بازی در حال اجرا رو ببینی."))
        }
        return
    }
    val err = acc.lastError
    Hint(
        when {
            syncing -> tr("Syncing Steam…", "در حال همگام‌سازی استیم…")
            err != null -> errorText(err) + " · " + tr("Showing saved data", "داده‌ی ذخیره‌شده نمایش داده می‌شه")
            else -> tr("Steam · last sync ", "استیم · آخرین همگام‌سازی ") + relativeDay(acc.lastSyncAt)
        },
        Modifier.clickable(enabled = err != null, onClick = onConnections),
    )
}

fun errorText(code: String): String = when (code) {
    "AUTH" -> tr("Steam needs access: check the key", "استیم دسترسی می‌خواد: کلید رو بررسی کن")
    "PRIVATE" -> tr("Steam profile or game details are private", "پروفایل یا بازی‌های استیم خصوصیه")
    "OFFLINE" -> tr("No connection", "اتصال نیست")
    "RATE" -> tr("Steam asked to slow down", "استیم خواست کندتر درخواست بدیم")
    else -> tr("Sync failed", "همگام‌سازی ناموفق بود")
}

/** Shown only when Steam itself reported a running game a moment ago. */
@Composable
private fun NowPlayingCard(n: NowPlaying, onOpen: (String) -> Unit) {
    val shape = RoundedCornerShape(22.dp)
    Box(
        Modifier.fillMaxWidth().height(170.dp).clip(shape).border(1.dp, NexusColors.Accent.copy(alpha = 0.5f), shape)
            .then(if (n.card != null) Modifier.clickable { onOpen(n.card.game.id) } else Modifier),
    ) {
        Cover(n.card?.game?.backgroundUri ?: n.card?.game?.coverUri, n.title, Modifier.fillMaxSize(), radius = 0, showTitle = false)
        Box(Modifier.fillMaxSize().background(ScrimBrush))
        Column(Modifier.align(Alignment.BottomStart).padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF22C55E)))
                Text(tr("NOW PLAYING", "الان در حال بازی"), style = MaterialTheme.typography.labelMedium, color = NexusColors.Accent)
            }
            Text(n.title, style = MaterialTheme.typography.titleLarge, color = Color.White, maxLines = 2)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                PlatformBadge(SourceType.STEAM)
                Text("Steam · " + tr("reported at ", "گزارش در ") + digits(DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(n.checkedAt))), style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.85f))
            }
        }
    }
}

@Composable
private fun Overview(o: HomeOverview) {
    Section(tr("Library overview", "نمای کلی کتابخانه")) {
        LabeledValue(tr("Games", "بازی‌ها"), num(o.total))
        o.perPlatform.entries.sortedByDescending { it.value }.forEach { LabeledValue(it.key.label(), num(it.value)) }
        LabeledValue(
            tr("Total playtime", "مجموع ساعت بازی"), o.storeMinutes?.let { hoursLabel(it.toInt()) } ?: "—",
            if (o.storeMinutes != null) tr("As reported by connected platforms", "طبق گزارش پلتفرم‌های متصل") else tr("No platform has reported playtime yet", "هنوز هیچ پلتفرمی ساعت بازی گزارش نکرده"),
        )
        LabeledValue(tr("Favorites", "موردعلاقه‌ها"), num(o.favorites))
    }
}

@Composable
private fun CardRow(title: String, cards: List<GameCard>, onOpen: (String) -> Unit, action: String? = null, onAction: () -> Unit = {}) {
    if (cards.isEmpty()) return
    Section(title) {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(cards, key = { it.game.id }) { c ->
                Column(Modifier.width(112.dp).pressClick(true) { onOpen(c.game.id) }, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box {
                        Cover(c.game.coverUri, c.game.title, Modifier.size(width = 112.dp, height = 156.dp), radius = 14)
                        c.sources.firstOrNull()?.let { PlatformBadge(it.type, Modifier.padding(6.dp)) }
                    }
                    Text(c.game.title, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(hoursLabel(c.stats.forever) + if (c.stats.lastPlayed != null) " · " + relativeDay(c.stats.lastPlayed) else "", style = MonoStyle, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
        if (action != null) TextButton(onClick = onAction) { Text(action) }
    }
}
