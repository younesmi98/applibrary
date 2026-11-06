package com.nexus.insights

import java.time.Instant
import java.time.Year
import java.time.YearMonth
import java.time.ZoneId

data class Wrapped(
    val year: Int,
    val recordedMinutes: Int,
    val topGame: Pair<String, Int>?,
    val mostActiveMonth: Pair<YearMonth, Int>?,
    val topGenre: String?,
    val gamesPlayed: Int,
    val gamesCompleted: Int,
    val longestSessionMinutes: Int,
    val newGames: Int,
)

object WrappedBuilder {
    /**
     * Uses only sessions NEXUS recorded in that year. Store hours (Steam etc.) are not used because they carry no dates.
     * Returns null when the year has no recorded sessions, so the UI shows "not enough data" instead of an empty report.
     */
    fun build(year: Int, games: List<GameFacts>, sessions: List<SessionFact>, zone: ZoneId): Wrapped? {
        fun inYear(ms: Long) = Year.from(Instant.ofEpochMilli(ms).atZone(zone)).value == year
        val s = sessions.filter { it.endedAt > it.startedAt && inYear(it.startedAt) }
        if (s.isEmpty()) return null
        val byId = games.associateBy { it.id }
        val perGame = s.groupBy { it.gameId }.mapValues { (_, v) -> v.sumOf { it.minutes } }
        val perMonth = s.groupBy { YearMonth.from(Instant.ofEpochMilli(it.startedAt).atZone(zone)) }.mapValues { (_, v) -> v.sumOf { it.minutes } }
        val genreMinutes = mutableMapOf<String, Int>()
        perGame.forEach { (id, m) -> byId[id]?.genres?.forEach { g -> genreMinutes.merge(g, m, Int::plus) } }
        return Wrapped(
            year = year,
            recordedMinutes = s.sumOf { it.minutes },
            topGame = perGame.maxByOrNull { it.value }?.let { (id, m) -> (byId[id]?.title ?: return@let null) to m },
            mostActiveMonth = perMonth.maxByOrNull { it.value }?.toPair(),
            topGenre = genreMinutes.maxByOrNull { it.value }?.key,
            gamesPlayed = perGame.size,
            gamesCompleted = games.count { it.completedAt?.let(::inYear) == true },
            longestSessionMinutes = s.maxOf { it.minutes },
            newGames = games.count { it.addedAt > 0 && inYear(it.addedAt) },
        )
    }
}
