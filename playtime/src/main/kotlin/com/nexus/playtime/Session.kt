package com.nexus.playtime

import androidx.room.*
import kotlinx.coroutines.flow.Flow

enum class SessionState { ACTIVE, PAUSED, FINISHED }
enum class SessionMethod { MANUAL, WINDOWS }   // kept apart from store-reported time and from adjustments

@Entity(tableName = "game_sessions", indices = [Index("gameId"), Index("state"), Index("startedAt")])
data class GameSessionEntity(
    @PrimaryKey val id: String,
    val gameId: String,
    val method: SessionMethod,
    val state: SessionState,
    val startedAt: Long,          // UTC epoch millis
    val endedAt: Long? = null,
    val note: String? = null,
)

@Entity(tableName = "game_session_pauses", indices = [Index("sessionId")])
data class SessionPauseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: String,
    val pausedAt: Long,
    val resumedAt: Long? = null,
)

/** One-time historical time. Never mixed into sessions; undo = delete the row. */
@Entity(tableName = "playtime_adjustments", indices = [Index("gameId")])
data class PlaytimeAdjustmentEntity(
    @PrimaryKey val id: String,
    val gameId: String,
    val deltaMillis: Long,
    val reason: String? = null,
    val createdAt: Long,
)

@Dao
interface SessionDao {
    @Insert suspend fun insert(s: GameSessionEntity)
    @Update suspend fun update(s: GameSessionEntity)
    @Query("DELETE FROM game_sessions WHERE id = :id") suspend fun delete(id: String)
    @Insert suspend fun insertPause(p: SessionPauseEntity)
    @Update suspend fun updatePause(p: SessionPauseEntity)
    @Query("SELECT * FROM game_session_pauses WHERE sessionId = :id") suspend fun pauses(id: String): List<SessionPauseEntity>
    @Query("SELECT * FROM game_sessions WHERE id = :id") suspend fun get(id: String): GameSessionEntity?
    @Query("SELECT * FROM game_sessions WHERE state != 'FINISHED'") suspend fun unfinished(): List<GameSessionEntity>
    @Query("SELECT * FROM game_sessions WHERE gameId = :gameId ORDER BY startedAt DESC") fun history(gameId: String): Flow<List<GameSessionEntity>>
    @Insert suspend fun insertAdjustment(a: PlaytimeAdjustmentEntity)
    @Query("DELETE FROM playtime_adjustments WHERE id = :id") suspend fun undoAdjustment(id: String)
    @Query("SELECT * FROM playtime_adjustments WHERE gameId = :gameId ORDER BY createdAt DESC") fun adjustments(gameId: String): Flow<List<PlaytimeAdjustmentEntity>>
}

object PlaytimeCalc {
    /** Active time = (end or now) - start - paused time. Clock jumps backwards never give negative time. */
    fun activeMillis(s: GameSessionEntity, pauses: List<SessionPauseEntity>, now: Long): Long {
        val end = s.endedAt ?: now
        val paused = pauses.sumOf { p ->
            val pe = p.resumedAt ?: if (s.state == SessionState.FINISHED) end else now
            (pe - p.pausedAt).coerceAtLeast(0)
        }
        return (end - s.startedAt - paused).coerceAtLeast(0)
    }
}
