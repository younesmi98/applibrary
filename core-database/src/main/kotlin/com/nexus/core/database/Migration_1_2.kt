package com.nexus.core.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

// Add the three playtime entities to NexusDatabase (version = 2), then register this migration.
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS game_sessions (id TEXT NOT NULL PRIMARY KEY, gameId TEXT NOT NULL, method TEXT NOT NULL, state TEXT NOT NULL, startedAt INTEGER NOT NULL, endedAt INTEGER, note TEXT)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_game_sessions_gameId ON game_sessions(gameId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_game_sessions_state ON game_sessions(state)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_game_sessions_startedAt ON game_sessions(startedAt)")
        db.execSQL("CREATE TABLE IF NOT EXISTS game_session_pauses (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, sessionId TEXT NOT NULL, pausedAt INTEGER NOT NULL, resumedAt INTEGER)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_game_session_pauses_sessionId ON game_session_pauses(sessionId)")
        db.execSQL("CREATE TABLE IF NOT EXISTS playtime_adjustments (id TEXT NOT NULL PRIMARY KEY, gameId TEXT NOT NULL, deltaMillis INTEGER NOT NULL, reason TEXT, createdAt INTEGER NOT NULL)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_playtime_adjustments_gameId ON playtime_adjustments(gameId)")
    }
}
