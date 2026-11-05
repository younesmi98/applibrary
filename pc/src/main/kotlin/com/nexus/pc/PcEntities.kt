package com.nexus.pc

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/** A Windows PC paired with this phone. Only the SHA-256 hash of its token is stored. */
@Entity(tableName = "paired_devices")
data class PairedDeviceEntity(
    @PrimaryKey val id: String,
    val name: String,
    val os: String,
    val companionVersion: String,
    val tokenHash: String,
    val pairedAt: Long,
    val lastSeenAt: Long?,
    val currentGameId: String?,
    val revokedAt: Long?,
)

/** Which running process counts as a game. Never stores the install path of the user's PC on a server. */
@Entity(tableName = "process_rules", indices = [Index("gameId"), Index("deviceId")])
data class ProcessRuleEntity(
    @PrimaryKey val id: String,
    val gameId: String,
    val deviceId: String?,          // null = any paired PC
    val executableName: String,     // e.g. eldenring.exe, compared case-insensitively
    val trackingMode: TrackingMode,
    val idleMinutes: Int?,          // null = idle detection off
    val enabled: Boolean,
)

enum class TrackingMode { WHILE_PROCESS_RUNS, WHILE_FOREGROUND }

/** Windows-tracked time. Separate table, never merged with Steam or manual time. */
@Entity(
    tableName = "pc_sessions",
    indices = [Index("gameId"), Index(value = ["deviceId", "remoteSessionId"], unique = true)],
)
data class PcSessionEntity(
    @PrimaryKey val id: String,
    val gameId: String,
    val deviceId: String,
    val remoteSessionId: String,    // id made by the companion, makes re-sync idempotent
    val startedAt: Long,            // UTC millis
    val endedAt: Long,
    val idleSeconds: Int,
)

@Dao
interface PcDao {
    @Query("SELECT * FROM paired_devices ORDER BY pairedAt DESC") fun devices(): Flow<List<PairedDeviceEntity>>
    @Query("SELECT * FROM paired_devices WHERE id = :id") suspend fun device(id: String): PairedDeviceEntity?
    @Upsert suspend fun upsertDevice(d: PairedDeviceEntity)
    @Query("UPDATE paired_devices SET revokedAt = :t WHERE id = :id") suspend fun revoke(id: String, t: Long)
    @Query("UPDATE paired_devices SET lastSeenAt = :t, currentGameId = :gameId WHERE id = :id")
    suspend fun heartbeat(id: String, t: Long, gameId: String?)

    @Query("SELECT * FROM process_rules WHERE gameId = :gameId") fun rulesFor(gameId: String): Flow<List<ProcessRuleEntity>>
    @Query("SELECT * FROM process_rules WHERE enabled = 1") suspend fun activeRules(): List<ProcessRuleEntity>
    @Upsert suspend fun upsertRule(r: ProcessRuleEntity)
    @Query("DELETE FROM process_rules WHERE id = :id") suspend fun deleteRule(id: String)

    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertSession(s: PcSessionEntity): Long
    @Query("SELECT * FROM pc_sessions WHERE gameId = :gameId ORDER BY startedAt DESC") fun sessionsFor(gameId: String): Flow<List<PcSessionEntity>>
    @Query("SELECT COALESCE(SUM(endedAt - startedAt - idleSeconds * 1000), 0) FROM pc_sessions WHERE gameId = :gameId")
    fun trackedMillis(gameId: String): Flow<Long>
}
