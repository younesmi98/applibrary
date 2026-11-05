package com.nexus.insights

data class CompareRow(val label: String, val values: List<String>)

object Compare {
    private const val NONE = "—"
    private fun hours(min: Int?) = min?.let { "%d h %02d m".format(it / 60, it % 60) } ?: NONE

    /** Shows each playtime source on its own row. Missing data is a dash, never zero. */
    fun rows(games: List<GameFacts>): List<CompareRow> = listOf(
        CompareRow("Release year", games.map { it.releaseYear?.toString() ?: NONE }),
        CompareRow("Genres", games.map { it.genres.joinToString().ifEmpty { NONE } }),
        CompareRow("Sources", games.map { it.sources.sorted().joinToString().ifEmpty { NONE } }),
        CompareRow("Your rating", games.map { it.rating?.let { r -> "%.1f".format(r) } ?: NONE }),
        CompareRow("Store playtime", games.map { hours(it.storeMinutes) }),
        CompareRow("Tracked in NEXUS", games.map { hours(it.trackedMinutes.takeIf { m -> m > 0 }) }),
        CompareRow("Achievements", games.map { it.achievementPercent?.let { p -> "$p%" } ?: NONE }),
        CompareRow("Status", games.map { it.status.name.lowercase().replace('_', ' ') }),
    )
}
