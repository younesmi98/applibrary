package com.nexus.importers

/** What the planner needs to know about the current library. Kept as lambdas so it stays pure and testable. */
class LibraryLookup(
    val gameIdForSource: (Store, String) -> String?,   // same store id already imported
    val gameIdsWithTitle: (String) -> List<String>,    // same title from any source, including manual games
)

sealed interface PlanItem {
    data class New(val game: ImportedGame) : PlanItem
    data class AlreadyThere(val game: ImportedGame, val gameId: String) : PlanItem
    /** Same title already exists from another source. Never merged automatically. */
    data class NeedsReview(val game: ImportedGame, val candidateIds: List<String>) : PlanItem
}

object ImportPlanner {
    fun plan(games: List<ImportedGame>, lookup: LibraryLookup): List<PlanItem> = games.map { g ->
        val byId = g.externalId?.let { lookup.gameIdForSource(g.store, it) }
        when {
            byId != null -> PlanItem.AlreadyThere(g, byId)
            lookup.gameIdsWithTitle(g.title).isNotEmpty() -> PlanItem.NeedsReview(g, lookup.gameIdsWithTitle(g.title))
            else -> PlanItem.New(g)
        }
    }
}

/**
 * Applying a plan (not covered by tests yet, needs the Room module):
 *  - New: insert GameEntity + GameSourceEntity(type = EPIC/GOG, externalId, officiallyOwned = false).
 *    officiallyOwned stays false because the data came from a file the user supplied, not a connected account.
 *  - NeedsReview: insert as a separate game too, and show it in the review list. The user merges by hand.
 *  - AlreadyThere: do nothing. Manual games, notes, collections and sessions are never touched.
 */
