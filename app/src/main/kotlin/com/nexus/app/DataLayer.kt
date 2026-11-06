package com.nexus.app

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.nexus.core.database.*
import com.nexus.importers.*
import com.nexus.playtime.*
import com.nexus.polish.*
import java.util.UUID

fun Context.readText(uri: Uri): String? =
    runCatching { contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } }.getOrNull()

fun Context.writeText(uri: Uri, text: String): Boolean =
    runCatching { contentResolver.openOutputStream(uri, "wt")?.bufferedWriter()?.use { it.write(text) } != null }.getOrDefault(false)

object BackupData {
    /** Manual session time has pauses removed. Windows sessions are not included: they are re-synced from the PC. */
    suspend fun build(db: NexusDatabase): Backup {
        val d = db.insights()
        val sources = d.allSources().groupBy { it.gameId }
        val pauses = d.allPauses().groupBy { it.sessionId }
        val items = d.collectionItems()
        return Backup(
            schemaVersion = BackupCodec.SCHEMA,
            exportedAt = System.currentTimeMillis(),
            games = d.games().map { g ->
                val ss = sources[g.id].orEmpty()
                val first = ss.firstOrNull()
                BGame(g.id, g.title, first?.type?.name ?: "MANUAL", g.status.name, g.notes.orEmpty(), g.favorite,
                    manual = ss.none { it.officiallyOwned }, externalId = first?.externalId)
            },
            sessions = d.finishedSessions().filter { it.method == SessionMethod.MANUAL }.mapNotNull { s ->
                val end = s.endedAt ?: return@mapNotNull null
                val active = PlaytimeCalc.activeMillis(s, pauses[s.id].orEmpty(), end)
                if (active < 1000) null else BSession(s.id, s.gameId, s.startedAt, s.startedAt + active, "MANUAL")
            },
            adjustments = d.adjustments().map { BAdjustment(it.id, it.gameId, (it.deltaMillis / 60_000L).toInt(), it.createdAt, it.reason.orEmpty()) },
            collections = d.collections().map { c -> BCollection(c.id, c.name, items.filter { it.collectionId == c.id }.map { it.gameId }) },
        )
    }

    suspend fun plan(db: NexusDatabase, b: Backup): RestorePlan {
        val d = db.insights()
        return RestorePlanner.plan(b, d.games().map { it.id }.toSet(), d.finishedSessions().map { it.id }.toSet(), d.adjustments().map { it.id }.toSet())
    }

    /** Adds only what is missing. Nothing existing is changed or deleted. */
    suspend fun restore(db: NexusDatabase, b: Backup, plan: RestorePlan) {
        val d = db.insights()
        val now = System.currentTimeMillis()
        db.withTransaction {
            val known = d.games().map { it.id }.toSet() + plan.newGames.map { it.id }
            d.addGames(plan.newGames.map { g ->
                GameEntity(g.id, g.title, status = runCatching { GameStatus.valueOf(g.status) }.getOrDefault(GameStatus.BACKLOG),
                    favorite = g.favorite, notes = g.notes.ifBlank { null }, createdAt = now, updatedAt = now)
            })
            d.addSources(plan.newGames.map { g ->
                GameSourceEntity(UUID.randomUUID().toString(), g.id, runCatching { SourceType.valueOf(g.source) }.getOrDefault(SourceType.MANUAL), g.externalId, false)
            })
            d.addSessions(plan.newSessions.map { GameSessionEntity(it.id, it.gameId, SessionMethod.MANUAL, SessionState.FINISHED, it.startedAt, it.endedAt) })
            d.addAdjustments(plan.newAdjustments.map { PlaytimeAdjustmentEntity(it.id, it.gameId, it.minutes * 60_000L, it.note.ifBlank { null }, it.at) })
            d.addCollections(b.collections.map { CollectionEntity(it.id, it.name) })
            d.addCollectionItems(b.collections.flatMap { c -> c.gameIds.filter { it in known }.map { CollectionItemEntity(c.id, it) } })
        }
    }
}

object ImportData {
    private fun SourceType.store() = when (this) { SourceType.EPIC -> Store.EPIC; SourceType.GOG -> Store.GOG; else -> Store.OTHER }
    private fun Store.type() = when (this) { Store.EPIC -> SourceType.EPIC; Store.GOG -> SourceType.GOG; Store.OTHER -> SourceType.OTHER_LAUNCHER }

    suspend fun plan(db: NexusDatabase, parsed: List<ImportedGame>): List<PlanItem> {
        val d = db.insights()
        val byKey = d.allSources().filter { it.externalId != null }.associate { (it.type.store() to it.externalId!!) to it.gameId }
        val byTitle = d.games().groupBy({ it.title.lowercase() }, { it.id })
        return ImportPlanner.plan(parsed, LibraryLookup({ st, id -> byKey[st to id] }, { t -> byTitle[t.lowercase()].orEmpty() }))
    }

    /** Imported rows are never marked officiallyOwned: they come from a file, not a connected account. */
    suspend fun apply(db: NexusDatabase, games: List<ImportedGame>): Int {
        val now = System.currentTimeMillis()
        val d = db.insights()
        db.withTransaction {
            val ids = games.map { UUID.randomUUID().toString() }
            d.addGames(games.mapIndexed { i, g -> GameEntity(ids[i], g.title, status = GameStatus.BACKLOG, createdAt = now, updatedAt = now) })
            d.addSources(games.mapIndexed { i, g -> GameSourceEntity(UUID.randomUUID().toString(), ids[i], g.store.type(), g.externalId, false) })
        }
        return games.size
    }
}
