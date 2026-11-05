package com.nexus.polish

import java.time.Instant

object CsvExport {
    /** RFC 4180 quoting. Cells starting with = + - @ get a leading apostrophe so spreadsheets do not run them as formulas. */
    fun cell(v: String): String {
        val safe = if (v.isNotEmpty() && v[0] in "=+-@") "'$v" else v
        return if (safe.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) "\"" + safe.replace("\"", "\"\"") + "\"" else safe
    }
    fun sessions(b: Backup): String {
        val titles = b.games.associate { it.id to it.title }
        val head = "game,source,started_utc,ended_utc,minutes"
        val rows = b.sessions.sortedBy { it.startedAt }.joinToString("\n") { s ->
            listOf(titles[s.gameId] ?: s.gameId, s.source, Instant.ofEpochMilli(s.startedAt).toString(), Instant.ofEpochMilli(s.endedAt).toString(), ((s.endedAt - s.startedAt) / 60_000).toString())
                .joinToString(",") { cell(it) }
        }
        return if (rows.isEmpty()) head else "$head\n$rows"
    }
}
