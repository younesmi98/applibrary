package com.nexus.steam

import org.junit.Assert.*
import org.junit.Test

class SteamJsonTest {
    @Test fun ownedGamesKeepNullAndRealZero() {
        val body = """{"response":{"game_count":3,"games":[
            {"appid":1091500,"name":"Cyberpunk 2077","playtime_forever":7200,"rtime_last_played":1700000000},
            {"appid":10,"name":"Counter-Strike","playtime_forever":0,"rtime_last_played":0},
            {"appid":20,"name":"No playtime field"}]}}"""
        val r = SteamJson.parseOwned(body) as SteamFetch.Ok
        assertEquals(3, r.games.size)
        assertEquals(7200, r.games[0].playtimeForeverMinutes)
        assertEquals(1700000000L, r.games[0].lastPlayedEpochSec)
        assertEquals(0, r.games[1].playtimeForeverMinutes)       // a real zero
        assertNull(r.games[1].lastPlayedEpochSec)                // 0 timestamp means never, not 1970
        assertNull(r.games[2].playtimeForeverMinutes)            // missing stays missing
    }

    @Test fun emptyResponseMeansPrivateNotEmptyLibrary() {
        assertTrue(SteamJson.parseOwned("""{"response":{}}""") is SteamFetch.PrivateOrEmpty)
        assertEquals(emptyList<SteamGame>(), (SteamJson.parseOwned("""{"response":{"game_count":0}}""") as SteamFetch.Ok).games)
    }

    @Test fun brokenJsonIsAFailureNotAnEmptyLibrary() {
        assertTrue(SteamJson.parseOwned("<html>") is SteamFetch.Failed)
    }

    @Test fun recentAndProfile() {
        assertEquals(mapOf(10L to 95), SteamJson.parseRecent("""{"response":{"games":[{"appid":10,"playtime_2weeks":95}]}}"""))
        val p = SteamJson.parseSummary("""{"response":{"players":[{"steamid":"7656","personaname":"Sam","gameid":"1091500","gameextrainfo":"Cyberpunk 2077"}]}}""")!!
        assertEquals("1091500", p.currentAppId)
        assertNull(SteamJson.parseSummary("""{"response":{"players":[{"steamid":"7656","personaname":"Sam"}]}}""")!!.currentAppId)
    }

    @Test fun vanity() {
        assertEquals("7656", SteamJson.parseVanity("""{"response":{"success":1,"steamid":"7656"}}"""))
        assertNull(SteamJson.parseVanity("""{"response":{"success":42}}"""))
    }
}
