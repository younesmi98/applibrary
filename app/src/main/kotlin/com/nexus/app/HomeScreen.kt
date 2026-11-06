package com.nexus.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.nexus.insights.GameFacts
import com.nexus.insights.Status
import com.nexus.playtime.PlaytimeCalc
import com.nexus.playtime.SessionState
import kotlinx.coroutines.delay
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
    }
    val tick by produceState(System.currentTimeMillis()) { while (true) { value = System.currentTimeMillis(); delay(1000) } }

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
            }
        }

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
                }
            }
            repeat(3 - games.size) { Spacer(Modifier.weight(1f)) }
        }
    }
}
