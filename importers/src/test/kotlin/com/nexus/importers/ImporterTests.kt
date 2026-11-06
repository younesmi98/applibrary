package com.nexus.importers

import org.junit.Assert.*
import org.junit.Test

class ImporterTests {
    @Test fun epicArrayAndHeroicObjectBothParse() {
        val arr = """[{"app_name":"Anemone","app_title":"World of Goo"},{"app_name":"x"}]"""
        val obj = """{"library":[{"app_name":"Sugar","title":"Celeste"}]}"""
        EpicLegendaryParser.parse(arr).let { assertEquals(1, it.games.size); assertEquals(1, it.skipped); assertEquals("World of Goo", it.games[0].title) }
        assertEquals("Sugar", EpicLegendaryParser.parse(obj).games[0].externalId)
    }

    @Test fun badJsonReportsErrorAndImportsNothing() {
        val r = EpicLegendaryParser.parse("not json")
        assertTrue(r.games.isEmpty()); assertNotNull(r.error)
    }

    @Test fun csvHandlesHeaderQuotesAndOptionalId() {
        val r = CsvTitleParser.parse("title,id\n\"Hades, Complete\",1207658791\nCeleste\n,\n", Store.GOG)
        assertEquals(listOf("Hades, Complete", "Celeste"), r.games.map { it.title })
        assertEquals("1207658791", r.games[0].externalId); assertNull(r.games[1].externalId); assertEquals(1, r.skipped)
    }

    @Test fun plannerNeverMergesByTitleAndKeepsKnownIds() {
        val lookup = LibraryLookup(
            gameIdForSource = { s, id -> if (s == Store.EPIC && id == "known") "g1" else null },
            gameIdsWithTitle = { if (it.equals("Hades", true)) listOf("manual1") else emptyList() },
        )
        val plan = ImportPlanner.plan(listOf(
            ImportedGame(Store.EPIC, "known", "Whatever"),
            ImportedGame(Store.GOG, null, "hades"),
            ImportedGame(Store.GOG, null, "Celeste"),
        ), lookup)
        assertTrue(plan[0] is PlanItem.AlreadyThere)
        assertTrue(plan[1] is PlanItem.NeedsReview)
        assertTrue(plan[2] is PlanItem.New)
    }
}
