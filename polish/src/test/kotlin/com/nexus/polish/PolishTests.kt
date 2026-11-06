package com.nexus.polish

import org.junit.Assert.*
import org.junit.Test

class PolishTests {
    private val backup = Backup(1, 1000, listOf(BGame("g1", "Hades", "Manual", "PLAYING", "great", true, true), BGame("g2", "Celeste, Deluxe", "Epic", "BACKLOG")),
        listOf(BSession("s1", "g1", 0, 3_600_000, "PC"), BSession("bad", "g1", 10, 5, "PC"), BSession("orphan", "zz", 0, 60_000, "PC")),
        listOf(BAdjustment("a1", "g1", 120, 5), BAdjustment("a2", "zz", 5, 5)), listOf(BCollection("c1", "Fav", listOf("g1", "zz"))))

    @Test fun roundTripKeepsManualDataAndDropsBrokenRows() {
        val r = BackupCodec.decode(BackupCodec.encode(backup)) as DecodeResult.Ok
        assertEquals(2, r.backup.games.size); assertEquals("great", r.backup.games[0].notes)
        assertEquals(listOf("s1"), r.backup.sessions.map { it.id }); assertEquals(listOf("a1"), r.backup.adjustments.map { it.id })
        assertEquals(listOf("g1"), r.backup.collections[0].gameIds); assertEquals(3, r.warnings.size)
    }
    @Test fun rejectsGarbageNewerSchemaAndDuplicateIds() {
        assertTrue(BackupCodec.decode("hello") is DecodeResult.Rejected)
        assertTrue(BackupCodec.decode(BackupCodec.encode(backup.copy(schemaVersion = 9))) is DecodeResult.Rejected)
        assertTrue(BackupCodec.decode(BackupCodec.encode(backup.copy(games = backup.games + backup.games[0]))) is DecodeResult.Rejected)
    }
    @Test fun restoreOnlyAddsMissingRows() {
        val p = RestorePlanner.plan(backup, setOf("g1"), emptySet(), setOf("a1"))
        assertEquals(listOf("g2"), p.newGames.map { it.id }); assertEquals(2, p.skippedExisting)
    }
    @Test fun csvQuotesAndNeutralisesFormulas() {
        assertEquals("\"Celeste, Deluxe\"", CsvExport.cell("Celeste, Deluxe"))
        assertEquals("'=SUM(A1)", CsvExport.cell("=SUM(A1)"))
        assertEquals("\"say \"\"hi\"\"\"", CsvExport.cell("say \"hi\""))
        assertTrue(CsvExport.sessions(backup).contains("Hades,PC,1970-01-01T00:00:00Z,1970-01-01T01:00:00Z,60"))
    }
    @Test fun weakDeviceDropsEffectsByItself() {
        assertEquals(EffectLevel.LOW, EffectsPolicy.resolve(DeviceInfo(2_000, false, false)).level)
        assertEquals(EffectLevel.HIGH, EffectsPolicy.resolve(DeviceInfo(8_000, false, false)).level)
        val saver = EffectsPolicy.resolve(DeviceInfo(8_000, true, false, userChoice = EffectLevel.HIGH))
        assertEquals(EffectLevel.LOW, saver.level); assertFalse(saver.blur)
        assertFalse(EffectsPolicy.resolve(DeviceInfo(8_000, false, true)).animations)
    }
    @Test fun durationsInBothLanguages() {
        assertEquals("2 h 15 m", Localization.duration(135, false)); assertEquals("۲ ساعت و ۱۵ دقیقه", Localization.duration(135, true))
        assertEquals("0 m", Localization.duration(0, false)); assertEquals("۳ ساعت", Localization.duration(180, true))
    }
}
