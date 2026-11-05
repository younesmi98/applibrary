package com.nexus.insights

import java.time.Duration

enum class SuggestionType { MOVE_TO_ON_HOLD, MOVE_TO_ABANDONED_REVIEW, START_OR_REMOVE }
data class BacklogSuggestion(val gameId: String, val type: SuggestionType, val reason: String)

/** Suggests only. Nothing here changes a game's status: the user confirms each change. */
object BacklogAssistant {
    fun suggest(games: List<GameFacts>, now: Long, staleDays: Int = 60, untouchedDays: Int = 180): List<BacklogSuggestion> {
        fun daysSince(t: Long) = Duration.ofMillis(now - t).toDays()
        return games.mapNotNull { g ->
            when {
                g.status == Status.PLAYING && g.lastPlayedAt != null && daysSince(g.lastPlayedAt) >= staleDays ->
                    BacklogSuggestion(g.id, SuggestionType.MOVE_TO_ON_HOLD, "Marked playing, last played ${daysSince(g.lastPlayedAt)} days ago")
                g.status == Status.ON_HOLD && g.lastPlayedAt != null && daysSince(g.lastPlayedAt) >= untouchedDays ->
                    BacklogSuggestion(g.id, SuggestionType.MOVE_TO_ABANDONED_REVIEW, "On hold for ${daysSince(g.lastPlayedAt)} days")
                g.status == Status.BACKLOG && !g.hasAnyPlaytime && g.addedAt > 0 && daysSince(g.addedAt) >= untouchedDays ->
                    BacklogSuggestion(g.id, SuggestionType.START_OR_REMOVE, "Never played, added ${daysSince(g.addedAt)} days ago")
                else -> null
            }
        }
    }
}
