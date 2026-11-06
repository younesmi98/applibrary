package com.nexus.insights

import java.time.LocalDate

data class WishlistItem(
    val id: String, val title: String,
    val releaseDate: LocalDate? = null,          // null = unknown, never guessed
    val remindDaysBefore: Int? = null,           // null = no reminder
    val targetPriceCents: Int? = null,
    val lastKnownPriceCents: Int? = null,        // only set by a real price provider; none is wired in yet
    val priceCheckedAt: Long? = null,
)

object WishlistRules {
    /** Items released today or later, soonest first. Unknown dates go last. */
    fun upcoming(items: List<WishlistItem>, today: LocalDate): List<WishlistItem> =
        items.filter { it.releaseDate == null || !it.releaseDate.isBefore(today) }
            .sortedWith(compareBy({ it.releaseDate == null }, { it.releaseDate }))

    /** Reminders that should fire today: exactly remindDaysBefore days before release, or on release day. */
    fun dueReminders(items: List<WishlistItem>, today: LocalDate): List<WishlistItem> = items.filter {
        val r = it.releaseDate ?: return@filter false
        val before = it.remindDaysBefore ?: return@filter false
        today == r.minusDays(before.toLong()) || today == r
    }

    /** Only true when a provider supplied a price AND the user set a target. */
    fun belowTarget(i: WishlistItem) = i.targetPriceCents != null && i.lastKnownPriceCents != null && i.lastKnownPriceCents <= i.targetPriceCents
}
