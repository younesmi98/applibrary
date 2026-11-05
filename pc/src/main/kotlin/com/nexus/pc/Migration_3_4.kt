package com.nexus.pc

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

// Add PairedDeviceEntity, ProcessRuleEntity, PcSessionEntity and PcDao to NexusDatabase (version = 4), then register this.
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS paired_devices (id TEXT NOT NULL PRIMARY KEY, name TEXT NOT NULL, os TEXT NOT NULL, companionVersion TEXT NOT NULL, tokenHash TEXT NOT NULL, pairedAt INTEGER NOT NULL, lastSeenAt INTEGER, currentGameId TEXT, revokedAt INTEGER)")
        db.execSQL("CREATE TABLE IF NOT EXISTS process_rules (id TEXT NOT NULL PRIMARY KEY, gameId TEXT NOT NULL, deviceId TEXT, executableName TEXT NOT NULL, trackingMode TEXT NOT NULL, idleMinutes INTEGER, enabled INTEGER NOT NULL)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_process_rules_gameId ON process_rules(gameId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_process_rules_deviceId ON process_rules(deviceId)")
        db.execSQL("CREATE TABLE IF NOT EXISTS pc_sessions (id TEXT NOT NULL PRIMARY KEY, gameId TEXT NOT NULL, deviceId TEXT NOT NULL, remoteSessionId TEXT NOT NULL, startedAt INTEGER NOT NULL, endedAt INTEGER NOT NULL, idleSeconds INTEGER NOT NULL)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_pc_sessions_gameId ON pc_sessions(gameId)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_pc_sessions_deviceId_remoteSessionId ON pc_sessions(deviceId, remoteSessionId)")
    }
}
