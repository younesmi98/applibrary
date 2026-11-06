package com.nexus.core.database

import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.nexus.pc.PcSessionEntity
import com.nexus.playtime.GameSessionEntity
import com.nexus.playtime.PlaytimeAdjustmentEntity
import com.nexus.playtime.SessionPauseEntity
import kotlinx.coroutines.flow.Flow

/** Wishlist item. releaseEpochDay null = unknown date. No price is stored: no price provider exists. */
@Entity(tableName = "wishlist", indices = [Index("releaseEpochDay")])
data class WishlistEntity(
    @PrimaryKey val id: String,
    val title: String,
    val releaseEpochDay: Long?,
    val remindDaysBefore: Int?,
    val targetPriceCents: Int?,
    val createdAt: Long,
)

@Entity(
    tableName = "journal_entries",
    foreignKeys = [ForeignKey(GameEntity::class, ["id"], ["gameId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("gameId"), Index("createdAt")],
)
data class JournalEntity(
    @PrimaryKey val id: String,
    val gameId: String,
    val createdAt: Long,
    val title: String,
    val body: String,
    val rating: Float?,
)

/** Extra pictures a person attached to a game. The URI comes from the system file picker with a persisted read grant. */
@Entity(
    tableName = "game_images",
    foreignKeys = [ForeignKey(GameEntity::class, ["id"], ["gameId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("gameId")],
)
data class GameImageEntity(
    @PrimaryKey val id: String,
    val gameId: String,
    val uri: String,
    val createdAt: Long,
)

data class SourceRow(val gameId: String, val type: SourceType)
data class SteamRow(val gameId: String, val minutes: Int)

@Dao
interface InsightsDao {
    @Query("SELECT * FROM games") suspend fun games(): List<GameEntity>
    @Query("SELECT gameId, type FROM game_sources") suspend fun sourceRows(): List<SourceRow>
    @Query("SELECT gameId, type FROM game_sources") fun sourceRowsFlow(): Flow<List<SourceRow>>
    @Query("SELECT gameId, type, externalId, officiallyOwned, id FROM game_sources") suspend fun allSources(): List<GameSourceEntity>
    @Query(
        "SELECT s.gameId AS gameId, s.minutes AS minutes FROM steam_playtime_snapshots s " +
            "WHERE s.capturedAt = (SELECT MAX(x.capturedAt) FROM steam_playtime_snapshots x WHERE x.gameId = s.gameId)"
    ) suspend fun steamRows(): List<SteamRow>
    @Query("SELECT * FROM game_sessions WHERE state = 'FINISHED'") suspend fun finishedSessions(): List<GameSessionEntity>
    @Query("SELECT * FROM game_session_pauses") suspend fun allPauses(): List<SessionPauseEntity>
    @Query("SELECT * FROM pc_sessions") suspend fun pcSessions(): List<PcSessionEntity>
    @Query("SELECT * FROM playtime_adjustments") suspend fun adjustments(): List<PlaytimeAdjustmentEntity>
    @Query("SELECT * FROM collections") suspend fun collections(): List<CollectionEntity>
    @Query("SELECT * FROM collection_items") suspend fun collectionItems(): List<CollectionItemEntity>
    @Query("SELECT * FROM collection_items WHERE gameId = :gameId") fun collectionsOf(gameId: String): Flow<List<CollectionItemEntity>>

    @Query("SELECT * FROM wishlist ORDER BY releaseEpochDay IS NULL, releaseEpochDay") fun wishlist(): Flow<List<WishlistEntity>>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertWish(w: WishlistEntity)
    @Query("DELETE FROM wishlist WHERE id = :id") suspend fun deleteWish(id: String)

    @Query("SELECT * FROM game_images WHERE gameId = :gameId ORDER BY createdAt") fun images(gameId: String): Flow<List<GameImageEntity>>
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun addImages(items: List<GameImageEntity>)
    @Query("DELETE FROM game_images WHERE id = :id") suspend fun deleteImage(id: String)

    @Query("SELECT * FROM journal_entries ORDER BY createdAt DESC") fun journal(): Flow<List<JournalEntity>>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertJournal(j: JournalEntity)
    @Query("DELETE FROM journal_entries WHERE id = :id") suspend fun deleteJournal(id: String)

    // Restore / import: add only, never replace an existing row.
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun addGames(items: List<GameEntity>)
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun addSources(items: List<GameSourceEntity>)
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun addSessions(items: List<GameSessionEntity>)
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun addAdjustments(items: List<PlaytimeAdjustmentEntity>)
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun addCollections(items: List<CollectionEntity>)
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun addCollectionItems(items: List<CollectionItemEntity>)
}

val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS wishlist (id TEXT NOT NULL, title TEXT NOT NULL, releaseEpochDay INTEGER, remindDaysBefore INTEGER, targetPriceCents INTEGER, createdAt INTEGER NOT NULL, PRIMARY KEY(id))")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_wishlist_releaseEpochDay ON wishlist(releaseEpochDay)")
        db.execSQL("CREATE TABLE IF NOT EXISTS journal_entries (id TEXT NOT NULL, gameId TEXT NOT NULL, createdAt INTEGER NOT NULL, title TEXT NOT NULL, body TEXT NOT NULL, rating REAL, PRIMARY KEY(id), FOREIGN KEY(gameId) REFERENCES games(id) ON UPDATE NO ACTION ON DELETE CASCADE)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_journal_entries_gameId ON journal_entries(gameId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_journal_entries_createdAt ON journal_entries(createdAt)")
    }
}

val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS game_images (id TEXT NOT NULL, gameId TEXT NOT NULL, uri TEXT NOT NULL, createdAt INTEGER NOT NULL, PRIMARY KEY(id), FOREIGN KEY(gameId) REFERENCES games(id) ON UPDATE NO ACTION ON DELETE CASCADE)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_game_images_gameId ON game_images(gameId)")
    }
}
