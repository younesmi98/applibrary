package com.nexus.steam

import androidx.room.*
import com.nexus.core.database.*
import java.util.UUID

/** Raw fields from IPlayerService/GetOwnedGames. Missing playtime stays null, it is not turned into 0. */
data class SteamGame(val appId: Long, val name: String, val playtimeForeverMinutes: Int?, val lastPlayedEpochSec: Long?)

sealed interface SteamFetch {
    data class Ok(val games: List<SteamGame>) : SteamFetch
    data object PrivateOrEmpty : SteamFetch      // profile/game details not public
    data class Failed(val retryable: Boolean) : SteamFetch
}

/** Store-reported playtime only. Windows and manual time live in other tables and are never added here. */
@Entity(tableName = "steam_playtime_snapshots", indices = [Index("gameId")])
data class SteamPlaytimeSnapshotEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val gameId: String, val minutes: Int, val capturedAt: Long,
)

@Dao
interface SteamDao {
    @Query("SELECT gameId FROM game_sources WHERE type = 'STEAM' AND externalId = :appId LIMIT 1") suspend fun gameIdForApp(appId: String): String?
    @Query("SELECT id FROM games WHERE title = :title COLLATE NOCASE") suspend fun idsWithTitle(title: String): List<String>
    @Query("SELECT minutes FROM steam_playtime_snapshots WHERE gameId = :gameId ORDER BY capturedAt DESC LIMIT 1") suspend fun lastMinutes(gameId: String): Int?
    @Insert suspend fun insertSnapshot(s: SteamPlaytimeSnapshotEntity)
}

data class SyncReport(val added: Int, val updated: Int, val needsReview: List<String>, val privateProfile: Boolean = false)

class SteamSyncer(private val db: NexusDatabase, private val dao: SteamDao, private val now: () -> Long = System::currentTimeMillis) {

    /** Adds and updates only. Missing remote games are kept, and manual games are never touched. */
    suspend fun sync(result: SteamFetch): SyncReport {
        if (result is SteamFetch.PrivateOrEmpty) return SyncReport(0, 0, emptyList(), privateProfile = true)
        if (result !is SteamFetch.Ok) return SyncReport(0, 0, emptyList())
        var added = 0; var updated = 0; val review = mutableListOf<String>()
        for (g in result.games) db.withTransaction {
            val existing = dao.gameIdForApp(g.appId.toString())
            val gameId = existing ?: UUID.randomUUID().toString().also { id ->
                val t = now()
                // Same title already in the library from another source: keep separate, ask the user to merge.
                if (dao.idsWithTitle(g.name).isNotEmpty()) review += g.name
                db.games().insert(GameEntity(id, g.name, coverUri = coverUrl(g.appId), createdAt = t, updatedAt = t))
                db.details().insertSources(listOf(GameSourceEntity(UUID.randomUUID().toString(), id, SourceType.STEAM, g.appId.toString(), officiallyOwned = true)))
                added++
            }
            if (existing != null) updated++
            val minutes = g.playtimeForeverMinutes
            if (minutes != null && dao.lastMinutes(gameId) != minutes)
                dao.insertSnapshot(SteamPlaytimeSnapshotEntity(gameId = gameId, minutes = minutes, capturedAt = now()))
        }
        return SyncReport(added, updated, review)
    }

    // Steam CDN pattern; may 404 for some apps, so the UI must fall back to placeholder art.
    private fun coverUrl(appId: Long) = "https://cdn.cloudflare.steamstatic.com/steam/apps/$appId/library_600x900.jpg"
}
