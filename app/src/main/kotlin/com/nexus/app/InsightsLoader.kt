package com.nexus.app

import com.nexus.core.database.GameStatus
import com.nexus.core.database.NexusDatabase
import com.nexus.insights.*

/** Read-only models for Play next, Backlog and Stats, built only from what platforms reported. */
data class InsightsSnapshot(val games: List<GameFacts>, val titles: Map<String, String>, val covers: Map<String, String?>, val cards: List<GameCard>)

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

object InsightsLoader {
    suspend fun load(db: NexusDatabase): InsightsSnapshot {
        val dao = db.insights()
        val games = dao.games()
        val bySource = dao.allSources().groupBy { it.gameId }
        val cards = games.map { g -> GameCard(g, bySource[g.id].orEmpty(), bySource[g.id].orEmpty().toStats()) }
        val facts = cards.map { c ->
            GameFacts(
                id = c.game.id, title = c.game.title, releaseYear = c.game.releaseYear,
                sources = c.sources.map { it.type.label() }.toSet(), status = c.game.status.toInsights(),
                favorite = c.game.favorite, rating = c.game.rating?.toFloat(),
                storeMinutes = c.stats.forever, pcMinutes = 0, manualMinutes = 0, adjustmentMinutes = 0,
                lastPlayedAt = c.stats.lastPlayed, addedAt = c.game.createdAt,
            )
        }
        return InsightsSnapshot(facts, games.associate { it.id to it.title }, games.associate { it.id to it.coverUri }, cards)
    }
}
