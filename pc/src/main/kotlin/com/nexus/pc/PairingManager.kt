package com.nexus.pc

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.UUID

data class PairingOffer(val code: String, val expiresAt: Long)
data class PairedResult(val deviceId: String, val token: String)

sealed interface PairOutcome {
    data class Paired(val result: PairedResult) : PairOutcome
    data object WrongCode : PairOutcome
    data object Expired : PairOutcome
    data object TooManyAttempts : PairOutcome
}

/**
 * One-time 6-digit code, valid for 5 minutes, max 5 wrong tries, single use.
 * The token handed to the PC is random 256-bit; only its hash is stored on the phone.
 */
class PairingManager(
    private val dao: PcDao,
    private val now: () -> Long = System::currentTimeMillis,
    private val rng: SecureRandom = SecureRandom(),
) {
    private var offer: PairingOffer? = null
    private var attempts = 0

    fun createOffer(): PairingOffer {
        val code = (rng.nextInt(900_000) + 100_000).toString()
        attempts = 0
        return PairingOffer(code, now() + TTL_MS).also { offer = it }
    }

    suspend fun claim(code: String, deviceName: String, os: String, version: String): PairOutcome {
        val o = offer ?: return PairOutcome.WrongCode
        if (now() > o.expiresAt) { offer = null; return PairOutcome.Expired }
        if (attempts >= MAX_ATTEMPTS) { offer = null; return PairOutcome.TooManyAttempts }
        if (!constantTimeEquals(code, o.code)) { attempts++; return PairOutcome.WrongCode }
        offer = null
        val token = ByteArray(32).also(rng::nextBytes).joinToString("") { "%02x".format(it) }
        val id = UUID.randomUUID().toString()
        dao.upsertDevice(PairedDeviceEntity(id, deviceName, os, version, sha256(token), now(), null, null, null))
        return PairOutcome.Paired(PairedResult(id, token))
    }

    /** True only for a known, non-revoked device whose token matches. */
    suspend fun authenticate(deviceId: String, token: String): Boolean {
        val d = dao.device(deviceId) ?: return false
        return d.revokedAt == null && constantTimeEquals(sha256(token), d.tokenHash)
    }

    suspend fun revoke(deviceId: String) = dao.revoke(deviceId, now())

    companion object {
        const val TTL_MS = 5 * 60_000L
        const val MAX_ATTEMPTS = 5
        fun sha256(s: String) = MessageDigest.getInstance("SHA-256").digest(s.toByteArray()).joinToString("") { "%02x".format(it) }
        private fun constantTimeEquals(a: String, b: String) = MessageDigest.isEqual(a.toByteArray(), b.toByteArray())
    }
}
