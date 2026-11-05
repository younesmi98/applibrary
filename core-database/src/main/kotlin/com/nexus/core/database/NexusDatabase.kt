package com.nexus.core.database

import android.content.Context
import androidx.room.*
import com.nexus.pc.*
import com.nexus.playtime.*
import com.nexus.steam.*

@Database(
    entities = [
        GameEntity::class, GameSourceEntity::class, GameInstallationEntity::class,
        CollectionEntity::class, CollectionItemEntity::class, GameTagEntity::class,
        // phase 3
        GameSessionEntity::class, SessionPauseEntity::class, PlaytimeAdjustmentEntity::class,
        // phase 4
        SteamPlaytimeSnapshotEntity::class,
        // phase 5
        PairedDeviceEntity::class, ProcessRuleEntity::class, PcSessionEntity::class,
        // phase 9: wishlist and journal
        WishlistEntity::class, JournalEntity::class,
    ],
    version = 5,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class NexusDatabase : RoomDatabase() {
    abstract fun games(): GameDao
    abstract fun details(): DetailDao
    abstract fun collections(): CollectionDao
    abstract fun sessions(): SessionDao
    abstract fun steam(): SteamDao
    abstract fun pc(): PcDao
    abstract fun insights(): InsightsDao

    companion object {
        fun build(context: Context): NexusDatabase =
            Room.databaseBuilder(context.applicationContext, NexusDatabase::class.java, "nexus.db")
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                .build()
    }
}
