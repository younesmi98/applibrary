package com.nexus.pc

import java.util.UUID

/** What the companion sends per finished session. No paths, no window titles, nothing else. */
data class IncomingPcSession(val remoteSessionId: String, val ruleId: String, val startedAt: Long, val endedAt: Long, val idleSeconds: Int)

data class PcSyncReport(val stored: Int, val duplicates: Int, val rejected: Int, val unknownRule: Int)

class PcSessionSync(private val dao: PcDao, private val auth: PairingManager, private val now: () -> Long = System::currentTimeMillis) {

    /** Safe to call again with the same payload: duplicates are ignored by the unique index. */
    suspend fun ingest(deviceId: String, token: String, sessions: List<IncomingPcSession>, runningRuleId: String? = null): PcSyncReport? {
        if (!auth.authenticate(deviceId, token)) return null
        val rules = dao.activeRules().associateBy { it.id }
        var stored = 0; var dup = 0; var rej = 0; var unknown = 0
        for (s in sessions) {
            val rule = rules[s.ruleId]
            if (rule == null) { unknown++; continue }
            val length = s.endedAt - s.startedAt
            // Reject impossible data: end before start, in the future, idle longer than the session, over 48 h.
            if (length <= 0 || s.endedAt > now() + CLOCK_SKEW_MS || s.idleSeconds < 0 || s.idleSeconds * 1000L > length || length > MAX_SESSION_MS) { rej++; continue }
            val row = PcSessionEntity(UUID.randomUUID().toString(), rule.gameId, deviceId, s.remoteSessionId, s.startedAt, s.endedAt, s.idleSeconds)
            if (dao.insertSession(row) == -1L) dup++ else stored++
        }
        dao.heartbeat(deviceId, now(), runningRuleId?.let { rules[it]?.gameId })
        return PcSyncReport(stored, dup, rej, unknown)
    }

    companion object { const val CLOCK_SKEW_MS = 5 * 60_000L; const val MAX_SESSION_MS = 48 * 3_600_000L }
}
