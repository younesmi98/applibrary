package com.nexus.core.database

import androidx.paging.PagingSource
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface GameDao {
    @Insert suspend fun insert(game: GameEntity)
    @Update suspend fun update(game: GameEntity)
    /** Removes the library entry and its rows only. Game files on disk are never touched. */
    @Query("DELETE FROM games WHERE id = :id") suspend fun delete(id: String)
    @Query("SELECT * FROM games WHERE id = :id") fun observe(id: String): Flow<GameEntity?>

    /**
     * Paged library query so thousands of games stay cheap.
     * Null filter = no filter. sort: 0 name, 1 recently added, 2 personal rating.
     * Search also matches original title, developer, publisher, notes and tags.
     */
    @Query(
        """
        SELECT g.* FROM games g
        WHERE g.status != 'HIDDEN'
          AND (:query = '' OR g.title LIKE '%' || :query || '%' OR g.originalTitle LIKE '%' || :query || '%'
               OR g.developer LIKE '%' || :query || '%' OR g.publisher LIKE '%' || :query || '%'
               OR g.notes LIKE '%' || :query || '%'
               OR EXISTS (SELECT 1 FROM game_tags t WHERE t.gameId = g.id AND t.tag LIKE '%' || :query || '%'))
          AND (:source IS NULL OR EXISTS (SELECT 1 FROM game_sources s WHERE s.gameId = g.id AND s.type = :source))
          AND (:status IS NULL OR g.status = :status)
          AND (:favoritesOnly = 0 OR g.favorite = 1)
          AND (:collectionId IS NULL OR EXISTS (SELECT 1 FROM collection_items c WHERE c.gameId = g.id AND c.collectionId = :collectionId))
        ORDER BY
          CASE WHEN :sort = 1 THEN g.createdAt END DESC,
          CASE WHEN :sort = 2 THEN g.rating END DESC,
          g.title COLLATE NOCASE ASC
        """
    )
    fun library(
        query: String, source: SourceType?, status: GameStatus?,
        favoritesOnly: Int, collectionId: String?, sort: Int,
    ): PagingSource<Int, GameEntity>

    @Query("SELECT COUNT(*) FROM game_sources WHERE type = 'MANUAL' OR officiallyOwned = 0") fun manualCount(): Flow<Int>
}

@Dao
interface DetailDao {
    @Insert suspend fun insertSources(items: List<GameSourceEntity>)
    @Insert suspend fun insertInstallation(item: GameInstallationEntity)
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertTags(items: List<GameTagEntity>)
    @Query("DELETE FROM game_tags WHERE gameId = :gameId AND tag = :tag") suspend fun removeTag(gameId: String, tag: String)
    @Query("SELECT * FROM game_sources WHERE gameId = :gameId") fun sources(gameId: String): Flow<List<GameSourceEntity>>
    @Query("SELECT tag FROM game_tags WHERE gameId = :gameId ORDER BY tag") fun tags(gameId: String): Flow<List<String>>
}

@Dao
interface CollectionDao {
    @Insert suspend fun insert(c: CollectionEntity)
    @Update suspend fun update(c: CollectionEntity)
    @Query("DELETE FROM collections WHERE id = :id") suspend fun delete(id: String)
    @Query("SELECT * FROM collections ORDER BY position, name COLLATE NOCASE") fun all(): Flow<List<CollectionEntity>>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun addItem(item: CollectionItemEntity)
    @Query("DELETE FROM collection_items WHERE collectionId = :cid AND gameId = :gid") suspend fun removeItem(cid: String, gid: String)
}
