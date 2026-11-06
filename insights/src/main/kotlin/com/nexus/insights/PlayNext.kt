package com.nexus.insights

import kotlin.random.Random

enum class PlayNextMode { RANDOM, SHORT, LEAST_PLAYED, UNPLAYED, CLOSE_TO_COMPLETION, RECENTLY_ADDED, HIGHEST_RATED }

object PlayNext {
    private val finished = setOf(Status.COMPLETED, Status.PERFECT, Status.ABANDONED, Status.HIDDEN)

    /** Games with unknown length are left out of SHORT and unrated games out of HIGHEST_RATED: no guessing. */
    fun suggest(games: List<GameFacts>, mode: PlayNextMode, limit: Int = 5, random: Random = Random.Default, shortHours: Int = 8): List<GameFacts> {
        val pool = games.filter { it.status !in finished }
        val picked = when (mode) {
            PlayNextMode.RANDOM -> pool.shuffled(random)
            PlayNextMode.SHORT -> pool.filter { (it.hoursToBeat ?: Int.MAX_VALUE) <= shortHours }.sortedBy { it.hoursToBeat }
            PlayNextMode.LEAST_PLAYED -> pool.filter { it.hasAnyPlaytime }.sortedBy { (it.storeMinutes ?: 0) + it.trackedMinutes }
            PlayNextMode.UNPLAYED -> pool.filter { !it.hasAnyPlaytime }.shuffled(random)
            PlayNextMode.CLOSE_TO_COMPLETION -> pool.filter { (it.achievementPercent ?: 0) in 70..99 }.sortedByDescending { it.achievementPercent }
            PlayNextMode.RECENTLY_ADDED -> pool.sortedByDescending { it.addedAt }
            PlayNextMode.HIGHEST_RATED -> pool.filter { it.rating != null }.sortedByDescending { it.rating }
        }
        return picked.take(limit)
    }
}
