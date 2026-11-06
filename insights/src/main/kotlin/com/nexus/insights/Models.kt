package com.nexus.insights

enum class Status { WANT, BACKLOG, PLAYING, ON_HOLD, COMPLETED, PERFECT, ABANDONED, REPLAYING, HIDDEN }
enum class SessionSource { PC, MANUAL }

/**
 * Read-only snapshot of one game for the insights code. Map it from Room later.
 * Playtime sources stay separate on purpose: storeMinutes is the store's own counter (null = the store gave none),
 * pcMinutes / manualMinutes / adjustmentMinutes are what NEXUS recorded. They are never summed into one number here.
 */
data class GameFacts(
    val id: String,
    val title: String,
    val genres: List<String> = emptyList(),
    val releaseYear: Int? = null,
    val sources: Set<String> = emptySet(),
    val status: Status = Status.BACKLOG,
    val favorite: Boolean = false,
    val rating: Float? = null,
    val storeMinutes: Int? = null,
    val pcMinutes: Int = 0,
    val manualMinutes: Int = 0,
    val adjustmentMinutes: Int = 0,
    val lastPlayedAt: Long? = null,
    val addedAt: Long = 0,
    val completedAt: Long? = null,
    val achievementPercent: Int? = null,
    val hoursToBeat: Int? = null,       // user-entered or provider value, null = unknown
) {
    /** Time recorded by NEXUS itself (PC sessions + manual sessions), without adjustments and without store time. */
    val trackedMinutes get() = pcMinutes + manualMinutes
    val hasAnyPlaytime get() = (storeMinutes ?: 0) > 0 || trackedMinutes > 0 || adjustmentMinutes > 0
}

data class SessionFact(val gameId: String, val startedAt: Long, val endedAt: Long, val source: SessionSource) {
    val minutes get() = ((endedAt - startedAt) / 60_000).toInt()
}
