package com.nexus.importers

import kotlinx.serialization.json.*

enum class Store { EPIC, GOG, OTHER }

/** One line from a user-supplied file. externalId is the store's own id when the file has one. */
data class ImportedGame(val store: Store, val externalId: String?, val title: String)

data class ParseResult(val games: List<ImportedGame>, val skipped: Int, val error: String? = null)

/**
 * Epic: reads JSON produced by Legendary (`legendary list --json`) or Heroic's cached library file.
 * Accepts a top-level array or an object with a "library" array. Field names are matched loosely
 * (app_name, title/app_title) because the format is unofficial and can change. Nothing is guessed:
 * entries without a usable id and title are counted as skipped.
 */
object EpicLegendaryParser {
    fun parse(text: String): ParseResult {
        val root = runCatching { Json.parseToJsonElement(text) }.getOrElse { return ParseResult(emptyList(), 0, "Not valid JSON") }
        val arr = when (root) {
            is JsonArray -> root
            is JsonObject -> root["library"] as? JsonArray
            else -> null
        } ?: return ParseResult(emptyList(), 0, "No game list found")
        var skipped = 0
        val out = mutableListOf<ImportedGame>()
        for (e in arr) {
            val o = e as? JsonObject
            val id = o?.str("app_name")
            val title = o?.str("title") ?: o?.str("app_title")
            if (id == null || title == null) { skipped++; continue }
            out += ImportedGame(Store.EPIC, id, title)
        }
        return ParseResult(out.distinctBy { it.externalId }, skipped)
    }
    private fun JsonObject.str(k: String) = (this[k] as? JsonPrimitive)?.takeIf { it.isString }?.content?.trim()?.takeIf { it.isNotEmpty() }
}

/**
 * Any store: CSV or plain text, one game per line. First column is the title; an optional second
 * column is the store's id. A header row named "title" is skipped. Quoted fields are supported.
 */
object CsvTitleParser {
    fun parse(text: String, store: Store): ParseResult {
        var skipped = 0
        val out = mutableListOf<ImportedGame>()
        text.lineSequence().map { it.trim() }.filter { it.isNotEmpty() }.forEachIndexed { i, line ->
            val cols = splitCsv(line)
            val title = cols.getOrNull(0)?.trim().orEmpty()
            if (i == 0 && title.equals("title", true)) return@forEachIndexed
            if (title.isEmpty()) { skipped++; return@forEachIndexed }
            out += ImportedGame(store, cols.getOrNull(1)?.trim()?.takeIf { it.isNotEmpty() }, title)
        }
        return ParseResult(out, skipped)
    }
    internal fun splitCsv(line: String): List<String> {
        val r = mutableListOf<String>(); val sb = StringBuilder(); var q = false; var i = 0
        while (i < line.length) {
            val c = line[i]
            when {
                c == '"' && q && line.getOrNull(i + 1) == '"' -> { sb.append('"'); i++ }
                c == '"' -> q = !q
                c == ',' && !q -> { r += sb.toString(); sb.clear() }
                else -> sb.append(c)
            }
            i++
        }
        return r + sb.toString()
    }
}
