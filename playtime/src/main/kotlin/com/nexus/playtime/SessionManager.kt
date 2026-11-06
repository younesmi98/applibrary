package com.nexus.playtime

import java.util.UUID

enum class RecoveryChoice { RESUME, COMPLETE, DISCARD }

class SessionManager(private val dao: SessionDao, private val now: () -> Long = System::currentTimeMillis) {

    /** Only one live session at a time. Returns the new session id. */
    suspend fun start(gameId: String): String {
        check(dao.unfinished().isEmpty()) { "A session is already running" }
        val id = UUID.randomUUID().toString()
        dao.insert(GameSessionEntity(id, gameId, SessionMethod.MANUAL, SessionState.ACTIVE, now()))
        return id
    }

    suspend fun pause(id: String) {
        val s = dao.get(id) ?: return
        if (s.state != SessionState.ACTIVE) return
        dao.update(s.copy(state = SessionState.PAUSED))
        dao.insertPause(SessionPauseEntity(sessionId = id, pausedAt = now()))
    }

    suspend fun resume(id: String) {
        val s = dao.get(id) ?: return
        if (s.state != SessionState.PAUSED) return
        dao.pauses(id).firstOrNull { it.resumedAt == null }?.let { dao.updatePause(it.copy(resumedAt = now())) }
        dao.update(s.copy(state = SessionState.ACTIVE))
    }

    /** Ends the session at `at` (defaults to now). Open pauses are closed at the same moment. */
    suspend fun finish(id: String, at: Long = now()) {
        val s = dao.get(id) ?: return
        if (s.state == SessionState.FINISHED) return
        dao.pauses(id).firstOrNull { it.resumedAt == null }?.let { dao.updatePause(it.copy(resumedAt = at)) }
        dao.update(s.copy(state = SessionState.FINISHED, endedAt = at.coerceAtLeast(s.startedAt)))
    }

    /** Add a past session by hand. */
    suspend fun addHistorical(gameId: String, start: Long, end: Long, note: String? = null) {
        require(end > start) { "End must be after start" }
        dao.insert(GameSessionEntity(UUID.randomUUID().toString(), gameId, SessionMethod.MANUAL, SessionState.FINISHED, start, end, note))
    }

    suspend fun addAdjustment(gameId: String, deltaMillis: Long, reason: String? = null): String {
        val id = UUID.randomUUID().toString()
        dao.insertAdjustment(PlaytimeAdjustmentEntity(id, gameId, deltaMillis, reason, now()))
        return id
    }
    suspend fun undoAdjustment(id: String) = dao.undoAdjustment(id)

    /** Call on app start. Show the user a dialog for each result: resume, complete, or discard. */
    suspend fun findInterrupted(): List<GameSessionEntity> = dao.unfinished()

    suspend fun recover(id: String, choice: RecoveryChoice, lastKnownAlive: Long? = null) = when (choice) {
        RecoveryChoice.RESUME -> resume(id)
        RecoveryChoice.COMPLETE -> finish(id, lastKnownAlive ?: now())
        RecoveryChoice.DISCARD -> dao.delete(id)
    }
}
