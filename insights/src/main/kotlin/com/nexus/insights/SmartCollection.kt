package com.nexus.insights

enum class PlaytimeBasis { STORE, TRACKED }

sealed interface Rule {
    fun matches(g: GameFacts): Boolean
    data class StatusIn(val statuses: Set<Status>) : Rule { override fun matches(g: GameFacts) = g.status in statuses }
    data class PlaytimeUnder(val minutes: Int, val basis: PlaytimeBasis) : Rule {
        // Unknown store time never counts as "under": the store gave us nothing to compare.
        override fun matches(g: GameFacts) = when (basis) {
            PlaytimeBasis.STORE -> g.storeMinutes?.let { it < minutes } ?: false
            PlaytimeBasis.TRACKED -> g.trackedMinutes < minutes
        }
    }
    data class AchievementAtLeast(val percent: Int) : Rule { override fun matches(g: GameFacts) = (g.achievementPercent ?: -1) >= percent }
    data class HasGenre(val genre: String) : Rule { override fun matches(g: GameFacts) = g.genres.any { it.equals(genre, true) } }
    data class FromSource(val source: String) : Rule { override fun matches(g: GameFacts) = source in g.sources }
    data object FavoriteOnly : Rule { override fun matches(g: GameFacts) = g.favorite }
}

/** All rules must match (AND). An empty rule list matches nothing, so a half-built collection never shows the whole library. */
data class SmartCollection(val name: String, val rules: List<Rule>) {
    fun apply(games: List<GameFacts>) = if (rules.isEmpty()) emptyList() else games.filter { g -> rules.all { it.matches(g) } }
}
