package com.nexus.steam

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

sealed interface SteamResult {
    data class Ok(val fetch: SteamFetch, val profile: SteamProfile?) : SteamResult
    data object InvalidKeyOrAccess : SteamResult   // HTTP 401/403: key rejected or access denied
    data object RateLimited : SteamResult          // HTTP 429
    data object Offline : SteamResult              // no connection or timeout
    data class Failed(val httpCode: Int) : SteamResult
}

private class SteamHttpError(val code: Int) : IOException("HTTP $code")

/**
 * Calls the documented Steam Web API directly with the person's own key.
 * The key is typed by the person and kept encrypted on this phone; it is never part of the APK and never logged.
 */
class SteamHttp(private val apiKey: String) {
    private fun q(v: String) = URLEncoder.encode(v, "UTF-8")

    private fun get(path: String, params: String): String {
        val c = URL("https://api.steampowered.com/$path/?key=${q(apiKey)}&$params&format=json").openConnection() as HttpURLConnection
        try {
            c.connectTimeout = 15_000; c.readTimeout = 20_000; c.requestMethod = "GET"
            val code = c.responseCode
            if (code != 200) throw SteamHttpError(code)
            return c.inputStream.bufferedReader().use { it.readText() }
        } finally { c.disconnect() }
    }

    suspend fun resolveVanity(name: String): String? = withContext(Dispatchers.IO) {
        runCatching { SteamJson.parseVanity(get("ISteamUser/ResolveVanityURL/v1", "vanityurl=${q(name)}")) }.getOrNull()
    }

    suspend fun profile(steamId: String): SteamProfile? = withContext(Dispatchers.IO) {
        runCatching { SteamJson.parseSummary(get("ISteamUser/GetPlayerSummaries/v2", "steamids=${q(steamId)}")) }.getOrNull()
    }

    suspend fun fetch(steamId: String): SteamResult = withContext(Dispatchers.IO) {
        try {
            val owned = SteamJson.parseOwned(get("IPlayerService/GetOwnedGames/v1", "steamid=${q(steamId)}&include_appinfo=1&include_played_free_games=1"))
            val recent = SteamJson.parseRecent(get("IPlayerService/GetRecentlyPlayedGames/v1", "steamid=${q(steamId)}"))
            val profile = SteamJson.parseSummary(get("ISteamUser/GetPlayerSummaries/v2", "steamids=${q(steamId)}"))
            val merged = if (owned is SteamFetch.Ok) SteamFetch.Ok(owned.games.map { g -> g.copy(playtime2WeeksMinutes = recent[g.appId] ?: g.playtime2WeeksMinutes) }) else owned
            SteamResult.Ok(merged, profile)
        } catch (e: SteamHttpError) {
            when (e.code) { 401, 403 -> SteamResult.InvalidKeyOrAccess; 429 -> SteamResult.RateLimited; else -> SteamResult.Failed(e.code) }
        } catch (e: IOException) {
            SteamResult.Offline
        }
    }
}
