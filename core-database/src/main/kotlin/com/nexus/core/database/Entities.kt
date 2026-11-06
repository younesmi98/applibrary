package com.nexus.core.database

import androidx.room.*

enum class GameStatus { WANT_TO_PLAY, BACKLOG, PLAYING, ON_HOLD, COMPLETED, FULL_COMPLETION, ABANDONED, REPLAYING, HIDDEN }

/** Where a copy of the game comes from. Kept apart from ownership: MANUAL entries are never "officially owned". */
enum class SourceType { STEAM, EPIC, GOG, OTHER_LAUNCHER, STANDALONE, DRM_FREE, EMULATOR, MANUAL, CUSTOM, PHYSICAL }

/** One canonical row per real game. Ids are UUID strings, times are UTC epoch millis. */
@Entity(tableName = "games", indices = [Index("title"), Index("status"), Index("favorite"), Index("createdAt")])
data class GameEntity(
    @PrimaryKey val id: String,
    val title: String,
    val originalTitle: String? = null,
    val developer: String? = null,
    val publisher: String? = null,
    val releaseYear: Int? = null,
    val description: String? = null,
    val status: GameStatus = GameStatus.BACKLOG,
    val favorite: Boolean = false,
    val rating: Int? = null,          // 1..10, personal
    val notes: String? = null,
    val coverUri: String? = null,
    val backgroundUri: String? = null,
    val createdAt: Long,
    val updatedAt: Long,
)

/** A game can have several sources (Steam + GOG + manual install). */
@Entity(
    tableName = "game_sources",
    foreignKeys = [ForeignKey(GameEntity::class, ["id"], ["gameId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("gameId"), Index("type")],
)
data class GameSourceEntity(
    @PrimaryKey val id: String,
    val gameId: String,
    val type: SourceType,
    val externalId: String? = null,   // e.g. Steam AppID, null for manual
    val officiallyOwned: Boolean,     // true only for verified store imports
)

/** All fields optional: an executable is never required. Only stored locally, never uploaded. */
@Entity(
    tableName = "game_installations",
    foreignKeys = [ForeignKey(GameEntity::class, ["id"], ["gameId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("gameId")],
)
data class GameInstallationEntity(
    @PrimaryKey val id: String,
    val gameId: String,
    val directory: String? = null,
    val executableName: String? = null,
    val launchArguments: String? = null,
    val deviceName: String? = null,
    val installed: Boolean = false,
)

@Entity(tableName = "collections")
data class CollectionEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String? = null,
    val position: Int = 0,
)

@Entity(
    tableName = "collection_items",
    primaryKeys = ["collectionId", "gameId"],
    foreignKeys = [
        ForeignKey(CollectionEntity::class, ["id"], ["collectionId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(GameEntity::class, ["id"], ["gameId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("gameId")],
)
data class CollectionItemEntity(val collectionId: String, val gameId: String, val position: Int = 0)

@Entity(
    tableName = "game_tags",
    primaryKeys = ["gameId", "tag"],
    foreignKeys = [ForeignKey(GameEntity::class, ["id"], ["gameId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("tag")],
)
data class GameTagEntity(val gameId: String, val tag: String)

class Converters {
    @TypeConverter fun fromStatus(v: GameStatus) = v.name
    @TypeConverter fun toStatus(v: String) = GameStatus.valueOf(v)
    @TypeConverter fun fromSource(v: SourceType) = v.name
    @TypeConverter fun toSource(v: String) = SourceType.valueOf(v)
    @TypeConverter fun fromSessionMethod(v: com.nexus.playtime.SessionMethod) = v.name
    @TypeConverter fun toSessionMethod(v: String) = com.nexus.playtime.SessionMethod.valueOf(v)
    @TypeConverter fun fromSessionState(v: com.nexus.playtime.SessionState) = v.name
    @TypeConverter fun toSessionState(v: String) = com.nexus.playtime.SessionState.valueOf(v)
    @TypeConverter fun fromTracking(v: com.nexus.pc.TrackingMode) = v.name
    @TypeConverter fun toTracking(v: String) = com.nexus.pc.TrackingMode.valueOf(v)
}
