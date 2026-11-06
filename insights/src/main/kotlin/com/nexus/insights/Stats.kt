package com.nexus.insights

import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId

data class LibraryStats(
    val totalGames: Int,
    val gamesPerSource: Map<String, Int>,
    val completed: Int,
    val unplayed: Int,
    val currentlyPlaying: Int,
    val favorites: Int,
    val storeMinutesTotal: Long,          // only games where the store reported time
    val trackedMinutesTotal: Long,        // PC + manual sessions recorded by NEXUS
    val genreCounts: Map<String, Int>,
    val releaseYearCounts: Map<Int, Int>,
    val mostTracked: List<Pair<String, Int>>,   // title to tracked minutes, only > 0
)

data class MonthlyActivity(val minutesPerMonth: Map<YearMonth, Int>, val coverageStart: Instant?)

object Stats {
    fun library(games: List<GameFacts>): LibraryStats {
        val visible = games.filter { it.status != Status.HIDDEN }
        return LibraryStats(
            totalGames = visible.size,
            gamesPerSource = visible.flatMap { g -> g.sources.map { it } }.groupingBy { it }.eachCount(),
            completed = visible.count { it.status == Status.COMPLETED || it.status == Status.PERFECT },
            unplayed = visible.count { !it.hasAnyPlaytime },
            currentlyPlaying = visible.count { it.status == Status.PLAYING },
            favorites = visible.count { it.favorite },
            storeMinutesTotal = visible.sumOf { (it.storeMinutes ?: 0).toLong() },
            trackedMinutesTotal = visible.sumOf { it.trackedMinutes.toLong() },
            genreCounts = visible.flatMap { it.genres }.groupingBy { it }.eachCount(),
            releaseYearCounts = visible.mapNotNull { it.releaseYear }.groupingBy { it }.eachCount(),
            mostTracked = visible.filter { it.trackedMinutes > 0 }.sortedByDescending { it.trackedMinutes }.take(10).map { it.title to it.trackedMinutes },
        )
    }

    /** Built only from recorded sessions. coverageStart tells the UI where real history begins. */
    fun monthly(sessions: List<SessionFact>, zone: ZoneId): MonthlyActivity {
        val valid = sessions.filter { it.endedAt > it.startedAt }
        val perMonth = valid.groupBy { YearMonth.from(Instant.ofEpochMilli(it.startedAt).atZone(zone)) }
            .mapValues { (_, v) -> v.sumOf { it.minutes } }.toSortedMap()
        return MonthlyActivity(perMonth, valid.minOfOrNull { it.startedAt }?.let(Instant::ofEpochMilli))
    }
}
