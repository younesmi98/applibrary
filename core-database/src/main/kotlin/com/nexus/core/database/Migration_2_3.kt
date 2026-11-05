package com.nexus.core.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

// Add SteamPlaytimeSnapshotEntity to NexusDatabase (version = 3), then register this migration.
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS steam_playtime_snapshots (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, gameId TEXT NOT NULL, minutes INTEGER NOT NULL, capturedAt INTEGER NOT NULL)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_steam_playtime_snapshots_gameId ON steam_playtime_snapshots(gameId)")
    }
}
