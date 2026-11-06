package com.nexus.app

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.nexus.core.database.GameRepository
import com.nexus.core.database.NexusDatabase
import com.nexus.playtime.*
import kotlinx.coroutines.flow.firstOrNull

class NexusApp : Application() {
    override fun onCreate() { super.onCreate(); I18n.load(this); Fonts.load(this); FxState.load(this) }
    val db: NexusDatabase by lazy { NexusDatabase.build(this) }
    val repo: GameRepository by lazy { GameRepository(db) }
    val sessions: SessionManager by lazy { SessionManager(db.sessions()) }
}

val Context.nexus: NexusApp get() = applicationContext as NexusApp

/** Builds the foreground-service intent from stored session data (no in-memory timer). */
object SessionNotifier {
    suspend fun push(ctx: Context, sessionId: String) {
        val app = ctx.nexus
        val s = app.db.sessions().get(sessionId) ?: return
        val title = app.db.games().observe(s.gameId).firstOrNull()?.title ?: "Game"
        val pauses = app.db.sessions().pauses(sessionId)
        val now = System.currentTimeMillis()
        val activeSince = now - PlaytimeCalc.activeMillis(s, pauses, now)
        val i = Intent(ctx, SessionService::class.java)
            .putExtra(SessionService.EXTRA_TITLE, title)
            .putExtra(SessionService.EXTRA_SESSION_ID, sessionId)
            .putExtra(SessionService.EXTRA_ACTIVE_SINCE, activeSince)
            .putExtra(SessionService.EXTRA_PAUSED, s.state == SessionState.PAUSED)
            .putExtra(SessionService.EXTRA_L_RUNNING, tr("Session running", "جلسه در حال اجراست"))
            .putExtra(SessionService.EXTRA_L_PAUSED, tr("Paused", "متوقف‌شده"))
            .putExtra(SessionService.EXTRA_L_PAUSE, tr("Pause", "توقف"))
            .putExtra(SessionService.EXTRA_L_RESUME, tr("Resume", "ادامه"))
            .putExtra(SessionService.EXTRA_L_FINISH, tr("Finish", "پایان"))
            .putExtra(SessionService.EXTRA_L_CHANNEL, tr("Active session", "جلسه‌ی فعال"))
        ContextCompat.startForegroundService(ctx, i)
    }

    fun stop(ctx: Context) {
        ctx.startService(Intent(ctx, SessionService::class.java).setAction(SessionService.ACTION_STOP))
    }
}
