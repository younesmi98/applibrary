package com.nexus.insights

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class Phase7bTests {
    private val day = 86_400_000L
    @Test fun wishlistOrderAndReminders() {
        val today = LocalDate.of(2026, 11, 4)
        val a = WishlistItem("a", "A", LocalDate.of(2026, 11, 11), remindDaysBefore = 7)
        val b = WishlistItem("b", "B", LocalDate.of(2026, 10, 1), remindDaysBefore = 7)
        val c = WishlistItem("c", "C")
        assertEquals(listOf("a", "c"), WishlistRules.upcoming(listOf(c, b, a), today).map { it.id })
        assertEquals(listOf("a"), WishlistRules.dueReminders(listOf(a, b, c), today).map { it.id })
    }
    @Test fun priceAlertNeedsProviderPriceAndTarget() {
        assertFalse(WishlistRules.belowTarget(WishlistItem("a", "A", targetPriceCents = 1000)))
        assertTrue(WishlistRules.belowTarget(WishlistItem("a", "A", targetPriceCents = 1000, lastKnownPriceCents = 999)))
    }
    @Test fun backlogSuggestsOnlyAndOnlyWhenStale() {
        val now = 1_000 * day
        val g = listOf(
            GameFacts("p", "P", status = Status.PLAYING, lastPlayedAt = now - 90 * day),
            GameFacts("fresh", "F", status = Status.PLAYING, lastPlayedAt = now - 5 * day),
            GameFacts("old", "O", status = Status.BACKLOG, addedAt = now - 300 * day),
            GameFacts("owned", "W", status = Status.BACKLOG, addedAt = now - 300 * day, storeMinutes = 30),
        )
        val s = BacklogAssistant.suggest(g, now)
        assertEquals(setOf("p", "old"), s.map { it.gameId }.toSet())
    }
    @Test fun milestonesAreNotOfficialAchievements() {
        val off = AchievementProgress.official(listOf(OfficialAchievement("x", "X", 5), OfficialAchievement("y", "Y", null)))
        assertEquals(50, off.percent)
        assertNull(AchievementProgress.official(emptyList()).percent)
        assertEquals(100, AchievementProgress.milestones(listOf(Milestone("1", "Beat it", 9))).percent)
    }
    @Test fun journalBlankSearchReturnsNothing() {
        val e = listOf(JournalEntry("1", "g", 1, "Great boss", "tough fight"), JournalEntry("2", "g", 2, "Ending", "loved it"))
        assertTrue(Journal.search(e, "  ").isEmpty())
        assertEquals(listOf("1"), Journal.search(e, "BOSS").map { it.id })
        assertEquals(listOf("2", "1"), Journal.forGame(e, "g").map { it.id })
    }
}
