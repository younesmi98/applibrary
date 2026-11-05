package com.nexus.polish

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable data class BGame(val id: String, val title: String, val source: String, val status: String, val notes: String = "", val favorite: Boolean = false, val manual: Boolean = false, val externalId: String? = null)
@Serializable data class BSession(val id: String, val gameId: String, val startedAt: Long, val endedAt: Long, val source: String)
@Serializable data class BAdjustment(val id: String, val gameId: String, val minutes: Int, val at: Long, val note: String = "")
@Serializable data class BCollection(val id: String, val name: String, val gameIds: List<String>)

@Serializable
data class Backup(
    val schemaVersion: Int, val exportedAt: Long,
    val games: List<BGame>, val sessions: List<BSession> = emptyList(),
    val adjustments: List<BAdjustment> = emptyList(), val collections: List<BCollection> = emptyList(),
)

sealed interface DecodeResult {
    data class Ok(val backup: Backup, val warnings: List<String>) : DecodeResult
    data class Rejected(val reason: String) : DecodeResult
}

/**
 * Not included in this backup: artwork files, Steam tokens, device tokens, store-reported playtime (re-synced from the store).
 * Restore never deletes anything; it only adds rows whose id is not already present (see RestorePlanner).
 */
object BackupCodec {
    const val SCHEMA = 1
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }

    fun encode(b: Backup) = json.encodeToString(Backup.serializer(), b)

    fun decode(text: String): DecodeResult {
        val b = runCatching { json.decodeFromString(Backup.serializer(), text) }.getOrElse { return DecodeResult.Rejected("Not a NEXUS backup file") }
        if (b.schemaVersion > SCHEMA) return DecodeResult.Rejected("Backup is from a newer NEXUS version (${b.schemaVersion})")
        if (b.games.map { it.id }.toSet().size != b.games.size) return DecodeResult.Rejected("Duplicate game ids")
        val ids = b.games.map { it.id }.toSet()
        val warnings = mutableListOf<String>()
        val goodSessions = b.sessions.filter { s -> (s.gameId in ids && s.endedAt > s.startedAt).also { ok -> if (!ok) warnings += "Dropped invalid session ${s.id}" } }
        val goodAdj = b.adjustments.filter { a -> (a.gameId in ids).also { ok -> if (!ok) warnings += "Dropped adjustment ${a.id} (unknown game)" } }
        val cols = b.collections.map { c -> c.copy(gameIds = c.gameIds.filter { it in ids }) }
        return DecodeResult.Ok(b.copy(sessions = goodSessions, adjustments = goodAdj, collections = cols), warnings)
    }
}

data class RestorePlan(val newGames: List<BGame>, val newSessions: List<BSession>, val newAdjustments: List<BAdjustment>, val skippedExisting: Int)

object RestorePlanner {
    fun plan(b: Backup, existingGameIds: Set<String>, existingSessionIds: Set<String>, existingAdjustmentIds: Set<String>): RestorePlan {
        val ng = b.games.filter { it.id !in existingGameIds }
        val ns = b.sessions.filter { it.id !in existingSessionIds }
        val na = b.adjustments.filter { it.id !in existingAdjustmentIds }
        return RestorePlan(ng, ns, na, (b.games.size - ng.size) + (b.sessions.size - ns.size) + (b.adjustments.size - na.size))
    }
}
