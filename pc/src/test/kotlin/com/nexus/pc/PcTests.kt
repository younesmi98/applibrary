package com.nexus.pc

import kotlinx.coroutines.flow.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class FakePcDao : PcDao {
    val devs = mutableMapOf<String, PairedDeviceEntity>()
    val rules = mutableMapOf<String, ProcessRuleEntity>()
    val sessions = mutableListOf<PcSessionEntity>()
    override fun devices() = flowOf(devs.values.toList())
    override suspend fun device(id: String) = devs[id]
    override suspend fun upsertDevice(d: PairedDeviceEntity) { devs[d.id] = d }
    override suspend fun revoke(id: String, t: Long) { devs[id]?.let { devs[id] = it.copy(revokedAt = t) } }
    override suspend fun heartbeat(id: String, t: Long, gameId: String?) { devs[id]?.let { devs[id] = it.copy(lastSeenAt = t, currentGameId = gameId) } }
    override fun rulesFor(gameId: String) = flowOf(rules.values.filter { it.gameId == gameId })
    override suspend fun activeRules() = rules.values.filter { it.enabled }
    override suspend fun upsertRule(r: ProcessRuleEntity) { rules[r.id] = r }
    override suspend fun deleteRule(id: String) { rules.remove(id) }
    override suspend fun insertSession(s: PcSessionEntity): Long {
        if (sessions.any { it.deviceId == s.deviceId && it.remoteSessionId == s.remoteSessionId }) return -1
        sessions += s; return sessions.size.toLong()
    }
    override fun sessionsFor(gameId: String) = flowOf(sessions.filter { it.gameId == gameId })
    override fun trackedMillis(gameId: String) = flowOf(sessions.filter { it.gameId == gameId }.sumOf { it.endedAt - it.startedAt - it.idleSeconds * 1000L })
}

class PcTests {
    private var t = 1_000_000_000L
    private val dao = FakePcDao()
    private val pairing = PairingManager(dao, { t })
    private val sync = PcSessionSync(dao, pairing, { t })

    private fun paired(): PairedResult = runBlocking {
        val code = pairing.createOffer().code
        (pairing.claim(code, "Desk PC", "Windows 11", "0.1") as PairOutcome.Paired).result
    }
    private fun rule() = ProcessRuleEntity("r1", "game1", null, "eldenring.exe", TrackingMode.WHILE_PROCESS_RUNS, null, true).also { dao.rules[it.id] = it }

    @Test fun wrongCodeFailsAndCodeIsSingleUse() = runBlocking {
        val offer = pairing.createOffer()
        assertEquals(PairOutcome.WrongCode, pairing.claim("000000".takeIf { it != offer.code } ?: "111111", "PC", "Win", "1"))
        assertTrue(pairing.claim(offer.code, "PC", "Win", "1") is PairOutcome.Paired)
        assertEquals(PairOutcome.WrongCode, pairing.claim(offer.code, "PC", "Win", "1"))
    }

    @Test fun expiredCodeIsRejected() = runBlocking {
        val offer = pairing.createOffer(); t += PairingManager.TTL_MS + 1
        assertEquals(PairOutcome.Expired, pairing.claim(offer.code, "PC", "Win", "1"))
    }

    @Test fun lockedAfterFiveWrongTries() = runBlocking {
        val offer = pairing.createOffer(); val bad = if (offer.code == "123456") "654321" else "123456"
        repeat(5) { pairing.claim(bad, "PC", "Win", "1") }
        assertEquals(PairOutcome.TooManyAttempts, pairing.claim(offer.code, "PC", "Win", "1"))
    }

    @Test fun revokedDeviceCannotSync() = runBlocking {
        val p = paired(); rule(); pairing.revoke(p.deviceId)
        assertNull(sync.ingest(p.deviceId, p.token, listOf(IncomingPcSession("a", "r1", t - 7_200_000, t - 3_600_000, 0))))
    }

    @Test fun resyncingSamePayloadDoesNotDoubleCount() = runBlocking {
        val p = paired(); rule()
        val s = listOf(IncomingPcSession("a", "r1", t - 8_100_000, t - 0, 0))
        assertEquals(1, sync.ingest(p.deviceId, p.token, s)!!.stored)
        assertEquals(1, sync.ingest(p.deviceId, p.token, s)!!.duplicates)
        assertEquals(1, dao.sessions.size)
    }

    @Test fun impossibleSessionsAreRejected() = runBlocking {
        val p = paired(); rule()
        val bad = listOf(
            IncomingPcSession("b1", "r1", t, t - 1000, 0),                          // ends before start
            IncomingPcSession("b2", "r1", t - 1000, t + 3_600_000, 0),              // in the future
            IncomingPcSession("b3", "r1", t - 60_000, t, 120),                      // idle longer than session
            IncomingPcSession("b4", "nope", t - 60_000, t, 0),                      // rule unknown
        )
        val r = sync.ingest(p.deviceId, p.token, bad)!!
        assertEquals(0, r.stored); assertEquals(3, r.rejected); assertEquals(1, r.unknownRule)
    }

    @Test fun idleTimeIsExcludedFromTrackedTime() = runBlocking {
        val p = paired(); rule()
        sync.ingest(p.deviceId, p.token, listOf(IncomingPcSession("a", "r1", t - 3_600_000, t, 600)))
        assertEquals(3_000_000L, dao.trackedMillis("game1").first())
    }
}
