package com.nexus.core.database

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.room.withTransaction
import java.util.UUID

data class NewGame(
    val title: String,
    val source: SourceType,
    val originalTitle: String? = null,
    val developer: String? = null,
    val publisher: String? = null,
    val releaseYear: Int? = null,
    val status: GameStatus = GameStatus.BACKLOG,
    val coverUri: String? = null,
    val executableName: String? = null,
    val directory: String? = null,
    val tags: List<String> = emptyList(),
)

data class LibraryFilter(
    val query: String = "", val source: SourceType? = null, val status: GameStatus? = null,
    val favoritesOnly: Boolean = false, val collectionId: String? = null, val sort: Int = 0,
)

class GameRepository(private val db: NexusDatabase, private val now: () -> Long = System::currentTimeMillis) {

    fun library(f: LibraryFilter) = Pager(PagingConfig(pageSize = 40, enablePlaceholders = false)) {
        db.games().library(f.query.trim(), f.source, f.status, if (f.favoritesOnly) 1 else 0, f.collectionId, f.sort)
    }.flow

    /** Manual games are marked officiallyOwned = false and need no store account. Returns the new game id. */
    suspend fun addGame(g: NewGame): String {
        require(g.title.isNotBlank()) { "Title is required" }
        val id = UUID.randomUUID().toString()
        val t = now()
        db.withTransaction {
            db.games().insert(
                GameEntity(id, g.title.trim(), g.originalTitle, g.developer, g.publisher, g.releaseYear,
                    status = g.status, coverUri = g.coverUri, createdAt = t, updatedAt = t)
            )
            db.details().insertSources(listOf(GameSourceEntity(UUID.randomUUID().toString(), id, g.source, null, false)))
            if (g.executableName != null || g.directory != null) {
                db.details().insertInstallation(
                    GameInstallationEntity(UUID.randomUUID().toString(), id, g.directory, g.executableName, installed = true)
                )
            }
            db.details().insertTags(g.tags.filter { it.isNotBlank() }.map { GameTagEntity(id, it.trim()) })
        }
        return id
    }

    suspend fun setStatus(game: GameEntity, status: GameStatus) = db.games().update(game.copy(status = status, updatedAt = now()))
    suspend fun toggleFavorite(game: GameEntity) = db.games().update(game.copy(favorite = !game.favorite, updatedAt = now()))
    suspend fun deleteEntry(id: String) = db.games().delete(id)
}
