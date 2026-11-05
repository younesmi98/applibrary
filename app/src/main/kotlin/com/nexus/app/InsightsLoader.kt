package com.nexus.app

import com.nexus.core.database.GameStatus
import com.nexus.core.database.NexusDatabase
import com.nexus.insights.*
import com.nexus.playtime.PlaytimeCalc
import com.nexus.playtime.SessionMethod

data class InsightsSnapshot(val games: List<GameFacts>, val sessions: List<SessionFact>, val titles: Map<String, String>, val covers: Map<String, String?> = emptyMap())

fun GameStatus.toInsights(): Status = when (this) {
    GameStatus.WANT_TO_PLAY -> Status.WANT
    GameStatus.BACKLOG -> Status.BACKLOG
    GameStatus.PLAYING -> Status.PLAYING
    GameStatus.ON_HOLD -> Status.ON_HOLD
    GameStatus.COMPLETED -> Status.COMPLETED
    GameStatus.FULL_COMPLETION -> Status.PERFECT
    GameStatus.ABANDONED -> Status.ABANDONED
    GameStatus.REPLAYING -> Status.REPLAYING
    GameStatus.HIDDEN -> Status.HIDDEN
}

/**
 * Reads Room and builds the read-only models the insights code expects.
 * Not stored yet, so left null/empty on purpose: genres, hours to beat, achievement percent, completion date.
 */
object InsightsLoader {
    suspend fun load(db: NexusDatabase): InsightsSnapshot {
        val dao = db.insights()
        val games = dao.games()
        val sources = dao.sourceRows().groupBy { it.gameId }
        val steam = dao.steamRows().associate { it.gameId to it.minutes }
        val pauses = dao.allPauses().groupBy { it.sessionId }
        val adjustments = dao.adjustments().groupBy { it.gameId }

        // Pauses and idle time are removed so session minutes match the tracked time.
        val facts = mutableListOf<SessionFact>()
        dao.finishedSessions().forEach { s ->
            val end = s.endedAt ?: return@forEach
            val active = PlaytimeCalc.activeMillis(s, pauses[s.id].orEmpty(), end)
            val src = if (s.method == SessionMethod.WINDOWS) SessionSource.PC else SessionSource.MANUAL
            facts += SessionFact(s.gameId, s.startedAt, s.startedAt + active, src)
        }
        dao.pcSessions().forEach { p ->
            val active = (p.endedAt - p.startedAt - p.idleSeconds * 1000L).coerceAtLeast(0)
            facts += SessionFact(p.gameId, p.startedAt, p.startedAt + active, SessionSource.PC)
        }
        val byGame = facts.groupBy { it.gameId }

        val gameFacts = games.map { g ->
            val mine = byGame[g.id].orEmpty()
            GameFacts(
                id = g.id, title = g.title,
                releaseYear = g.releaseYear,
                sources = sources[g.id].orEmpty().map { it.type.label() }.toSet(),
                status = g.status.toInsights(),
                favorite = g.favorite,
                rating = g.rating?.toFloat(),
                storeMinutes = steam[g.id],
                pcMinutes = mine.filter { it.source == SessionSource.PC }.sumOf { it.minutes },
                manualMinutes = mine.filter { it.source == SessionSource.MANUAL }.sumOf { it.minutes },
                adjustmentMinutes = (adjustments[g.id].orEmpty().sumOf { it.deltaMillis } / 60_000L).toInt(),
                lastPlayedAt = mine.maxOfOrNull { it.endedAt },
                addedAt = g.createdAt,
            )
        }
        return InsightsSnapshot(gameFacts, facts, games.associate { it.id to it.title }, games.associate { it.id to it.coverUri })
    }
}
