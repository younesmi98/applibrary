package com.nexus.insights

data class OfficialAchievement(val apiName: String, val name: String, val unlockedAt: Long?)   // from the store, read-only
data class Milestone(val id: String, val title: String, val doneAt: Long?)                     // written by the user

data class Progress(val done: Int, val total: Int) { val percent get() = if (total == 0) null else done * 100 / total }

object AchievementProgress {
    /** Two separate numbers. A milestone never counts as an official achievement. Null percent = no data. */
    fun official(list: List<OfficialAchievement>) = Progress(list.count { it.unlockedAt != null }, list.size)
    fun milestones(list: List<Milestone>) = Progress(list.count { it.doneAt != null }, list.size)
}
