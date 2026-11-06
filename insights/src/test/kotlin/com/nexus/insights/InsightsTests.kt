package com.nexus.insights

import org.junit.Assert.*
import org.junit.Test
import java.time.*
import kotlin.random.Random

class InsightsTests {
    private val utc = ZoneOffset.UTC
    private fun ms(y: Int, m: Int, d: Int, h: Int = 12) = LocalDateTime.of(y, m, d, h, 0).toInstant(utc).toEpochMilli()
    private fun g(id: String, vararg f: Pair<String, Any?>) = GameFacts(id, id)

    @Test fun statsKeepStoreAndTrackedSeparate() {
        val games = listOf(
            GameFacts("a", "A", storeMinutes = 6000, pcMinutes = 600, sources = setOf("Steam"), genres = listOf("RPG")),
            GameFacts("b", "B", sources = setOf("Manual"), status = Status.COMPLETED, manualMinutes = 60),
            GameFacts("c", "C", status = Status.HIDDEN),
        )
        val s = Stats.library(games)
        assertEquals(2, s.totalGames); assertEquals(6000L, s.storeMinutesTotal); assertEquals(660L, s.trackedMinutesTotal)
        assertEquals(1, s.completed); assertEquals(0, s.unplayed)
    }

    @Test fun monthlyHasCoverageStartAndIgnoresBrokenSessions() {
        val s = listOf(
            SessionFact("a", ms(2026, 9, 3), ms(2026, 9, 3, 14), SessionSource.PC),
            SessionFact("a", ms(2026, 10, 1), ms(2026, 10, 1, 13), SessionSource.MANUAL),
            SessionFact("a", ms(2026, 10, 2), ms(2026, 10, 1), SessionSource.PC),
        )
        val m = Stats.monthly(s, utc)
        assertEquals(120, m.minutesPerMonth[YearMonth.of(2026, 9)]); assertEquals(60, m.minutesPerMonth[YearMonth.of(2026, 10)])
        assertEquals(ms(2026, 9, 3), m.coverageStart!!.toEpochMilli())
    }

    @Test fun wrappedIsNullWithoutSessionsAndUsesOnlyThatYear() {
        val games = listOf(GameFacts("a", "A", genres = listOf("RPG"), completedAt = ms(2026, 5, 1), addedAt = ms(2026, 1, 1)), GameFacts("b", "B"))
        assertNull(WrappedBuilder.build(2026, games, emptyList(), utc))
        val s = listOf(
            SessionFact("a", ms(2026, 3, 1), ms(2026, 3, 1, 15), SessionSource.PC),     // 3 h
            SessionFact("b", ms(2026, 3, 5), ms(2026, 3, 5, 13), SessionSource.PC),     // 1 h
            SessionFact("b", ms(2025, 12, 31), ms(2025, 12, 31, 20), SessionSource.PC), // other year
        )
        val w = WrappedBuilder.build(2026, games, s, utc)!!
        assertEquals(240, w.recordedMinutes); assertEquals("A", w.topGame!!.first); assertEquals(YearMonth.of(2026, 3), w.mostActiveMonth!!.first)
        assertEquals("RPG", w.topGenre); assertEquals(2, w.gamesPlayed); assertEquals(1, w.gamesCompleted); assertEquals(180, w.longestSessionMinutes); assertEquals(1, w.newGames)
    }

    @Test fun playNextSkipsFinishedAndUnknowns() {
        val games = listOf(
            GameFacts("short", "Short", hoursToBeat = 4), GameFacts("long", "Long", hoursToBeat = 60), GameFacts("unknown", "Unknown"),
            GameFacts("done", "Done", status = Status.COMPLETED, hoursToBeat = 2),
            GameFacts("close", "Close", achievementPercent = 90), GameFacts("far", "Far", achievementPercent = 20),
        )
        assertEquals(listOf("Short"), PlayNext.suggest(games, PlayNextMode.SHORT).map { it.title })
        assertEquals(listOf("Close"), PlayNext.suggest(games, PlayNextMode.CLOSE_TO_COMPLETION).map { it.title })
        assertTrue(PlayNext.suggest(games, PlayNextMode.RANDOM, random = Random(1)).none { it.title == "Done" })
        assertTrue(PlayNext.suggest(games, PlayNextMode.HIGHEST_RATED).isEmpty())
    }

    @Test fun smartCollectionExampleFromSpec() {
        val games = listOf(
            GameFacts("ok", "Ok", status = Status.BACKLOG, storeMinutes = 300, achievementPercent = 75),
            GameFacts("much", "Much", status = Status.BACKLOG, storeMinutes = 6000, achievementPercent = 80),
            GameFacts("noStore", "NoStore", status = Status.BACKLOG, achievementPercent = 90),
        )
        val c = SmartCollection("Nearly done", listOf(Rule.StatusIn(setOf(Status.BACKLOG)), Rule.PlaytimeUnder(600, PlaytimeBasis.STORE), Rule.AchievementAtLeast(70)))
        assertEquals(listOf("Ok"), c.apply(games).map { it.title })
        assertTrue(SmartCollection("empty", emptyList()).apply(games).isEmpty())
    }

    @Test fun compareShowsDashForMissingNotZero() {
        val rows = Compare.rows(listOf(GameFacts("a", "A", storeMinutes = 90), GameFacts("b", "B")))
        val store = rows.first { it.label == "Store playtime" }.values
        assertEquals("1 h 30 m", store[0]); assertEquals("—", store[1])
        assertEquals("—", rows.first { it.label == "Tracked in NEXUS" }.values[1])
    }
}
