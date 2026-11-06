package com.nexus.insights

data class JournalEntry(val id: String, val gameId: String, val createdAt: Long, val title: String, val body: String, val rating: Float? = null)

object Journal {
    fun forGame(entries: List<JournalEntry>, gameId: String) = entries.filter { it.gameId == gameId }.sortedByDescending { it.createdAt }
    /** Case-insensitive match in title or body. Blank query returns nothing, not everything. */
    fun search(entries: List<JournalEntry>, q: String): List<JournalEntry> {
        val s = q.trim(); if (s.isEmpty()) return emptyList()
        return entries.filter { it.title.contains(s, true) || it.body.contains(s, true) }.sortedByDescending { it.createdAt }
    }
}
