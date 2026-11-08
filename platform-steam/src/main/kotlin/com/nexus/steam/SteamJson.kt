package com.nexus.steam

import kotlinx.serialization.json.*

/** Who the Steam account is, as far as the profile's privacy settings let the API say. */
data class SteamProfile(
    val steamId: String, val name: String?, val avatarUrl: String?, val profileUrl: String?,
    val currentAppId: String?,        // only set when Steam itself reports a running game
    val currentGameName: String?,
)

/** Pure parsing of Steam Web API responses, so it can be unit tested without a network. */
object SteamJson {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private fun response(body: String): JsonObject? =
        runCatching { json.parseToJsonElement(body).jsonObject["response"]?.jsonObject }.getOrNull()

    /**
     * IPlayerService/GetOwnedGames. An empty "response" object means the game details are private (or the key/id are wrong),
     * which is reported as PrivateOrEmpty instead of an empty library.
     */
    fun parseOwned(body: String): SteamFetch {
        val r = response(body) ?: return SteamFetch.Failed(retryable = true)
        val games = r["games"]?.jsonArray
        if (games == null) return if (r["game_count"]?.jsonPrimitive?.intOrNull == 0) SteamFetch.Ok(emptyList()) else SteamFetch.PrivateOrEmpty
        return SteamFetch.Ok(games.mapNotNull { e ->
            val o = e.jsonObject
            val id = o["appid"]?.jsonPrimitive?.longOrNull ?: return@mapNotNull null
            val name = o["name"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
            val lastPlayed = o["rtime_last_played"]?.jsonPrimitive?.longOrNull?.takeIf { it > 0 }
            SteamGame(id, name, o["playtime_forever"]?.jsonPrimitive?.intOrNull, lastPlayed, o["playtime_2weeks"]?.jsonPrimitive?.intOrNull)
        })
    }

    /** IPlayerService/GetRecentlyPlayedGames: appid to minutes played in the last two weeks. */
    fun parseRecent(body: String): Map<Long, Int> {
        val games = response(body)?.get("games")?.jsonArray ?: return emptyMap()
        return games.mapNotNull { e ->
            val o = e.jsonObject
            val id = o["appid"]?.jsonPrimitive?.longOrNull ?: return@mapNotNull null
            val m = o["playtime_2weeks"]?.jsonPrimitive?.intOrNull ?: return@mapNotNull null
            id to m
        }.toMap()
    }

    /** ISteamUser/GetPlayerSummaries. `gameid` is present only while a game is running and the profile shows it. */
    fun parseSummary(body: String): SteamProfile? {
        val p = response(body)?.get("players")?.jsonArray?.firstOrNull()?.jsonObject ?: return null
        val id = p["steamid"]?.jsonPrimitive?.contentOrNull ?: return null
        return SteamProfile(
            id, p["personaname"]?.jsonPrimitive?.contentOrNull, p["avatarfull"]?.jsonPrimitive?.contentOrNull,
            p["profileurl"]?.jsonPrimitive?.contentOrNull, p["gameid"]?.jsonPrimitive?.contentOrNull, p["gameextrainfo"]?.jsonPrimitive?.contentOrNull,
        )
    }

    /** ISteamUser/ResolveVanityURL: success == 1 gives the SteamID64. */
    fun parseVanity(body: String): String? {
        val r = response(body) ?: return null
        return if (r["success"]?.jsonPrimitive?.intOrNull == 1) r["steamid"]?.jsonPrimitive?.contentOrNull else null
    }
}
