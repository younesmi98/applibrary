package com.nexus.app

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nexus.insights.GameFacts
import com.nexus.insights.Status
import com.nexus.playtime.PlaytimeCalc
import com.nexus.playtime.SessionState
import kotlinx.coroutines.delay

@Composable
fun HomeScreen(app: NexusApp, onOpen: (String) -> Unit, onAdd: () -> Unit, onImport: () -> Unit) {
    val snap by produceState<InsightsSnapshot?>(null) { value = InsightsLoader.load(app.db) }
    val live by produceState<ActiveSession?>(null) {
        while (true) {
            val s = app.db.sessions().unfinished().firstOrNull()
            value = s?.let { ActiveSession(it, app.db.sessions().pauses(it.id)) }
            delay(2000)
        }
    }
    val tick by produceState(System.currentTimeMillis()) { while (true) { value = System.currentTimeMillis(); delay(1000) } }

    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp).verticalScroll(androidx.compose.foundation.rememberScrollState()), verticalArrangement = Arrangement.spacedBy(28.dp)) {
        PageTitle("NEXUS")
        val s = snap
        if (s == null) { Hint(tr("Loading…", "در حال بارگذاری…")); return@Column }

        live?.let { l ->
            Panel(Modifier.clickable { onOpen(l.session.gameId) }) {
                Text(tr("Session in progress", "جلسه در حال اجراست"), style = MaterialTheme.typography.bodySmall, color = NexusColors.TextSecondary)
                Text(s.titles[l.session.gameId] ?: tr("Game", "بازی"), style = MaterialTheme.typography.titleLarge)
                val paused = l.session.state == SessionState.PAUSED
                Text(
                    formatClock(PlaytimeCalc.activeMillis(l.session, l.pauses, tick)),
                    style = MaterialTheme.typography.headlineMedium,
                    color = if (paused) NexusColors.TextSecondary else NexusColors.Accent,
                )
            }
        }

        if (s.games.isEmpty()) {
            EmptyState(
                tr("Your library is empty", "کتابخانه‌ات خالیه"),
                tr("Add a game by hand, or import a list from a file.", "یه بازی رو دستی اضافه کن یا فهرست بازی‌ها رو از فایل وارد کن."),
            ) {
                Button(onClick = onAdd) { Text(tr("Add a game", "افزودن بازی")) }
                OutlinedButton(onClick = onImport) { Text(tr("Import from file", "ورود از فایل")) }
            }
            return@Column
        }

        val played = s.games.filter { it.lastPlayedAt != null }.sortedByDescending { it.lastPlayedAt }
        val playing = s.games.filter { it.status == Status.PLAYING }
        PosterRow(tr("Continue playing", "ادامه‌ی بازی"), (played + playing).distinctBy { it.id }.take(10), s, onOpen)
        PosterRow(tr("Your backlog", "صف بازی‌های تو"), s.games.filter { it.status == Status.BACKLOG || it.status == Status.WANT }.sortedByDescending { it.addedAt }.take(10), s, onOpen)
        PosterRow(tr("Favorites", "موردعلاقه‌ها"), s.games.filter { it.favorite }.take(10), s, onOpen)
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun PosterRow(title: String, games: List<GameFacts>, s: InsightsSnapshot, onOpen: (String) -> Unit) {
    if (games.isEmpty()) return
    Section(title) {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(games, key = { it.id }) { g ->
                Column(Modifier.width(112.dp).clickable { onOpen(g.id) }, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Cover(s.covers[g.id], g.title, Modifier.size(width = 112.dp, height = 156.dp))
                    Text(g.title, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}
