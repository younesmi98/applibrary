package com.nexus.app

import com.nexus.core.database.PlatformAccountEntity
import com.nexus.steam.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** What a sync attempt ended as. The UI turns each into a clear message; none of them invents data. */
sealed interface SyncOutcome {
    data class Success(val added: Int, val updated: Int, val toReview: List<String>) : SyncOutcome
    data object NotConnected : SyncOutcome
    data object NotFound : SyncOutcome            // the SteamID / profile name could not be resolved
    data object InvalidKey : SyncOutcome          // key rejected or access denied
    data object Private : SyncOutcome             // profile or game details are private
    data object RateLimited : SyncOutcome
    data object Offline : SyncOutcome
    data class Failed(val code: Int) : SyncOutcome
}

/** How a platform looks in the Connections screen. */
enum class ConnectionState { NOT_CONNECTED, CONNECTED, SYNCING, FAILED, NEEDS_ACCESS }

/**
 * The Steam provider. It reads the library with the person's own Web API key (typed in, stored encrypted).
 * Epic has no official library API for third-party apps, so there is deliberately no Epic provider here.
 */
class PlatformSync(private val app: NexusApp) {
    private val _syncing = MutableStateFlow(false)
    val syncing: StateFlow<Boolean> = _syncing

    private val db get() = app.db

    private fun creds(): Pair<String, String>? {
        val key = SecureStore.get(app, "steam_key") ?: return null
        val id = SecureStore.get(app, "steam_id") ?: return null
        return id to key
    }

    fun isConnected(): Boolean = creds() != null

    /** Accepts a SteamID64, a profile link, or a custom profile name. */
    suspend fun connect(input: String, apiKey: String): SyncOutcome {
        val key = apiKey.trim()
        var raw = input.trim().trimEnd('/')
        if (key.isEmpty() || raw.isEmpty()) return SyncOutcome.NotFound
        val http = SteamHttp(key)
        val fromProfiles = Regex("""steamcommunity\.com/profiles/(\d{17})""").find(raw)?.groupValues?.get(1)
        val fromVanity = Regex("""steamcommunity\.com/id/([^/?#]+)""").find(raw)?.groupValues?.get(1)
        val id = when {
            Regex("""\d{17}""").matches(raw) -> raw
            fromProfiles != null -> fromProfiles
            else -> http.resolveVanity(fromVanity ?: raw) ?: return SyncOutcome.NotFound
        }
        SecureStore.put(app, "steam_key", key)
        SecureStore.put(app, "steam_id", id)
        val result = sync()
        // A key that does not work must not stay stored as if the account were connected.
        if (result is SyncOutcome.InvalidKey || result is SyncOutcome.NotConnected) disconnect()
        return result
    }

    suspend fun sync(): SyncOutcome {
        val (steamId, key) = creds() ?: return SyncOutcome.NotConnected
        if (!_syncing.compareAndSet(false, true)) return SyncOutcome.Failed(0)
        try {
            val dao = db.insights()
            val old = dao.accountOnce("STEAM")
            val now = System.currentTimeMillis()
            fun failed(code: String) = (old ?: PlatformAccountEntity("STEAM", steamId, null, null, null, null, null, null, null)).copy(lastError = code)
            return when (val r = SteamHttp(key).fetch(steamId)) {
                is SteamResult.Ok -> {
                    if (r.fetch is SteamFetch.PrivateOrEmpty) { dao.upsertAccount(failed("PRIVATE")); SyncOutcome.Private }
                    else if (r.fetch !is SteamFetch.Ok) { dao.upsertAccount(failed("HTTP")); SyncOutcome.Failed(0) }
                    else {
                        val report = SteamSyncer(db, db.steam()).sync(r.fetch)
                        val p = r.profile
                        dao.upsertAccount(PlatformAccountEntity(
                            "STEAM", steamId, p?.name ?: old?.displayName, p?.avatarUrl ?: old?.avatarUrl, now, null,
                            p?.currentAppId, p?.currentGameName, if (p != null) now else old?.currentCheckedAt,
                        ))
                        SyncOutcome.Success(report.added, report.updated, report.needsReview)
                    }
                }
                SteamResult.InvalidKeyOrAccess -> { dao.upsertAccount(failed("AUTH")); SyncOutcome.InvalidKey }
                SteamResult.RateLimited -> { dao.upsertAccount(failed("RATE")); SyncOutcome.RateLimited }
                SteamResult.Offline -> { dao.upsertAccount(failed("OFFLINE")); SyncOutcome.Offline }
                is SteamResult.Failed -> { dao.upsertAccount(failed("HTTP")); SyncOutcome.Failed(r.httpCode) }
            }
        } finally { _syncing.value = false }
    }

    /** Light check of "is a game running right now" from Steam's own profile data. Does not touch the library. */
    suspend fun refreshPresence() {
        val (steamId, key) = creds() ?: return
        val acc = db.insights().accountOnce("STEAM") ?: return
        val p = SteamHttp(key).profile(steamId) ?: return
        db.insights().upsertAccount(acc.copy(currentAppId = p.currentAppId, currentGameName = p.currentGameName, currentCheckedAt = System.currentTimeMillis()))
    }

    /** Removes the key and the account link. Games, notes, collections and every personal choice stay. */
    suspend fun disconnect() {
        SecureStore.remove(app, "steam_key"); SecureStore.remove(app, "steam_id")
        db.insights().deleteAccount("STEAM")
    }

    companion object {
        fun state(acc: PlatformAccountEntity?, syncing: Boolean): ConnectionState = when {
            syncing -> ConnectionState.SYNCING
            acc == null || acc.accountId == null -> ConnectionState.NOT_CONNECTED
            acc.lastError == "AUTH" -> ConnectionState.NEEDS_ACCESS
            acc.lastError != null -> ConnectionState.FAILED
            else -> ConnectionState.CONNECTED
        }
    }
}
