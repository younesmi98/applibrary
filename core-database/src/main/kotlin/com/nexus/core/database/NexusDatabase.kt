package com.nexus.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.nexus.pc.PairedDeviceEntity
import com.nexus.pc.PcSessionEntity
import com.nexus.pc.ProcessRuleEntity
import com.nexus.steam.SteamPlaytimeSnapshotEntity

@Database(
    entities = [
        GameEntity::class,
        GameSourceEntity::class,
        GameInstallationEntity::class,
        CollectionEntity::class,
        CollectionItemEntity::class,
        GameTagEntity::class,
        SteamPlaytimeSnapshotEntity::class,
        PairedDeviceEntity::class,
        ProcessRuleEntity::class,
        PcSessionEntity::class,
        WishlistEntity::class,
        JournalEntity::class,
        GameImageEntity::class,
        PlatformAccountEntity::class,
    ],
    version = 7,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class NexusDatabase : RoomDatabase() {

    abstract fun games(): GameDao
    abstract fun details(): DetailDao
    abstract fun collections(): CollectionDao
    abstract fun steam(): SteamDao
    abstract fun pc(): PcDao
    abstract fun insights(): InsightsDao

    companion object {
        fun build(context: Context): NexusDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                NexusDatabase::class.java,
                "nexus.db",
            )
                .addMigrations(
                    MIGRATION_1_2,
                    MIGRATION_2_3,
                    MIGRATION_3_4,
                    MIGRATION_4_5,
                    MIGRATION_5_6,
                    MIGRATION_6_7,
                )
                .build()
        }
    }
}
