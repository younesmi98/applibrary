package com.nexus.app

import com.nexus.core.database.GameEntity
import com.nexus.core.database.GameSourceEntity
import com.nexus.core.database.PlatformAccountEntity
import com.nexus.core.database.SourceType

/**
 * Figures a platform reported for one game. null means "the platform gave no figure", which is different from 0.
 * When a game exists on two platforms, the source with the larger total is shown; the figures are never added together.
 */
data class GameStats(val forever: Int?, val twoWeeks: Int?, val lastPlayed: Long?, val lastSync: Long?, val platform: SourceType?)

fun List<GameSourceEntity>.toStats(): GameStats {
    val best = filter { it.playtimeForeverMin != null || it.lastPlayedAt != null }.maxByOrNull { it.playtimeForeverMin ?: -1 }
        ?: return GameStats(null, null, null, null, firstOrNull()?.type)
    return GameStats(best.playtimeForeverMin, best.playtime2WeeksMin, best.lastPlayedAt, best.lastSyncAt, best.type)
}

const val PRESENCE_FRESH_MS = 10 * 60 * 1000L

/** Ids of games the platform itself reported as running, and only while that report is fresh. */
fun playingGameIds(account: PlatformAccountEntity?, sources: List<GameSourceEntity>, now: Long): Set<String> {
    val app = account?.currentAppId ?: return emptySet()
    val checked = account.currentCheckedAt ?: return emptySet()
    if (now - checked > PRESENCE_FRESH_MS) return emptySet()
    return sources.filter { it.type == SourceType.STEAM && it.externalId == app }.map { it.gameId }.toSet()
}

data class GameCard(val game: GameEntity, val sources: List<GameSourceEntity>, val stats: GameStats)

/** 12h, 86.5h, 120h. Minutes below one hour stay in minutes. null (no figure) is shown as a dash, a real 0 as 0h. */
fun hoursLabel(minutes: Int?): String = when {
    minutes == null -> "—"
    minutes == 0 -> digits("0h")
    minutes < 60 -> digits("${minutes}m")
    minutes % 60 == 0 || minutes >= 6000 -> digits("${minutes / 60}h")
    else -> digits("%.1fh".format(java.util.Locale.US, minutes / 60.0))
}

/** "2h 15m" style for the detail page when exact minutes are known. */
fun hoursMinutesLabel(minutes: Int?): String =
    if (minutes == null) "—" else formatDuration(minutes * 60_000L)

fun relativeDay(epochMillis: Long?, now: Long = System.currentTimeMillis()): String {
    if (epochMillis == null) return "—"
    val days = ((now - epochMillis) / 86_400_000L).toInt()
    return when {
        days <= 0 -> tr("Today", "امروز")
        days == 1 -> tr("Yesterday", "دیروز")
        days < 30 -> tr("$days days ago", "${num(days)} روز پیش")
        else -> formatDate(epochMillis)
    }
}
