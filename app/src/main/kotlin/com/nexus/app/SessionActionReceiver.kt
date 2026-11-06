package com.nexus.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.nexus.playtime.SessionService
import com.nexus.playtime.SessionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Handles Pause/Resume and Finish taps from the ongoing notification. */
class SessionActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getStringExtra(SessionService.EXTRA_SESSION_ID) ?: return
        val action = intent.action
        val pending = goAsync()
        val app = context.nexus
        CoroutineScope(Dispatchers.IO).launch {
            try {
                when (action) {
                    SessionService.ACTION_TOGGLE -> {
                        val s = app.db.sessions().get(id)
                        if (s?.state == SessionState.ACTIVE) app.sessions.pause(id) else app.sessions.resume(id)
                        SessionNotifier.push(context, id)
                    }
                    SessionService.ACTION_FINISH -> {
                        app.sessions.finish(id)
                        SessionNotifier.stop(context)
                    }
                }
            } finally { pending.finish() }
        }
    }
}
