package com.nexus.playtime

import android.app.*
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat

/**
 * Keeps a manual session visible while the app is in the background.
 * The notification uses a system chronometer anchored to the stored start time,
 * so there is no in-memory ticking counter to lose when the process dies.
 */
class SessionService : Service() {
    override fun onBind(i: Intent?): IBinder? = null
    override fun onCreate() { super.onCreate(); running = true }
    override fun onDestroy() { running = false; super.onDestroy() }

    override fun onStartCommand(i: Intent?, flags: Int, startId: Int): Int {
        when (i?.action) {
            ACTION_STOP -> { stopForeground(STOP_FOREGROUND_REMOVE); stopSelf(); return START_NOT_STICKY }
        }
        val title = i?.getStringExtra(EXTRA_TITLE) ?: return START_NOT_STICKY
        val sessionId = i.getStringExtra(EXTRA_SESSION_ID) ?: return START_NOT_STICKY
        val activeSince = i.getLongExtra(EXTRA_ACTIVE_SINCE, System.currentTimeMillis()) // start + paused time
        val paused = i.getBooleanExtra(EXTRA_PAUSED, false)
        ensureChannel(i.getStringExtra(EXTRA_L_CHANNEL) ?: "Active session")
        val n = NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(applicationInfo.icon)
            .setContentTitle(title)
            .setContentText(i.getStringExtra(if (paused) EXTRA_L_PAUSED else EXTRA_L_RUNNING) ?: if (paused) "Paused" else "Session running")
            .setUsesChronometer(!paused).setShowWhen(!paused).setWhen(activeSince)
            .setOngoing(true).setOnlyAlertOnce(true)
            .addAction(0, i.getStringExtra(if (paused) EXTRA_L_RESUME else EXTRA_L_PAUSE) ?: if (paused) "Resume" else "Pause", action(ACTION_TOGGLE, sessionId))
            .addAction(0, i.getStringExtra(EXTRA_L_FINISH) ?: "Finish", action(ACTION_FINISH, sessionId))
            .setContentIntent(openApp())
            .build()
        ServiceCompat.startForeground(this, NOTIF_ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        return START_REDELIVER_INTENT
    }

    // Broadcast target (SessionActionReceiver) calls SessionManager then re-sends this service intent.
    private fun action(a: String, id: String) = PendingIntent.getBroadcast(
        this, a.hashCode(), Intent(a).setPackage(packageName).putExtra(EXTRA_SESSION_ID, id),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private fun openApp() = PendingIntent.getActivity(
        this, 0, packageManager.getLaunchIntentForPackage(packageName), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private fun ensureChannel(name: String) {
        val nm = getSystemService(NotificationManager::class.java)
        if (nm.getNotificationChannel(CHANNEL) == null)
            nm.createNotificationChannel(NotificationChannel(CHANNEL, name, NotificationManager.IMPORTANCE_LOW))
    }

    companion object {
        @Volatile var running = false
        const val CHANNEL = "active_session"; const val NOTIF_ID = 1001
        const val ACTION_STOP = "com.nexus.STOP"; const val ACTION_TOGGLE = "com.nexus.TOGGLE"; const val ACTION_FINISH = "com.nexus.FINISH"
        const val EXTRA_TITLE = "title"; const val EXTRA_SESSION_ID = "sid"; const val EXTRA_ACTIVE_SINCE = "since"; const val EXTRA_PAUSED = "paused"
        const val EXTRA_L_RUNNING = "l_running"; const val EXTRA_L_PAUSED = "l_paused"; const val EXTRA_L_PAUSE = "l_pause"
        const val EXTRA_L_RESUME = "l_resume"; const val EXTRA_L_FINISH = "l_finish"; const val EXTRA_L_CHANNEL = "l_channel"
    }
}
