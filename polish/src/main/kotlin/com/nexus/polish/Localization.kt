package com.nexus.polish

object Localization {
    private const val FA = "۰۱۲۳۴۵۶۷۸۹"
    fun persianDigits(s: String) = s.map { if (it in '0'..'9') FA[it - '0'] else it }.joinToString("")

    /** "2 h 15 m" in English, "۲ ساعت و ۱۵ دقیقه" in Persian. Zero hours or minutes are left out. */
    fun duration(totalMinutes: Int, persian: Boolean): String {
        val h = totalMinutes / 60; val m = totalMinutes % 60
        val en = listOfNotNull(h.takeIf { it > 0 }?.let { "$it h" }, m.takeIf { it > 0 || h == 0 }?.let { "$it m" }).joinToString(" ")
        if (!persian) return en
        val fa = listOfNotNull(h.takeIf { it > 0 }?.let { "$it ساعت" }, m.takeIf { it > 0 || h == 0 }?.let { "$it دقیقه" }).joinToString(" و ")
        return persianDigits(fa)
    }
}
