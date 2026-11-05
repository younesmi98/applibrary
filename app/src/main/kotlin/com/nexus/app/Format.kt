package com.nexus.app

import com.nexus.core.database.GameStatus
import com.nexus.core.database.SourceType
import com.nexus.insights.Status
import com.nexus.polish.Localization
import java.text.DateFormat
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date

fun formatDuration(ms: Long): String = Localization.duration((ms / 60_000).toInt(), I18n.fa)

fun formatClock(ms: Long): String {
    val s = ms / 1000
    return digits("%02d:%02d:%02d".format(s / 3600, (s % 3600) / 60, s % 60))
}

/** Persian uses the Jalali calendar and Persian digits through ICU. */
fun formatDate(utcMillis: Long): String =
    if (I18n.fa) android.icu.text.DateFormat.getDateTimeInstance(
        android.icu.text.DateFormat.MEDIUM, android.icu.text.DateFormat.SHORT, android.icu.util.ULocale("fa_IR@calendar=persian"),
    ).format(Date(utcMillis))
    else DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(utcMillis))

fun formatDay(d: LocalDate): String {
    val ms = d.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    return if (I18n.fa) android.icu.text.DateFormat.getDateInstance(
        android.icu.text.DateFormat.MEDIUM, android.icu.util.ULocale("fa_IR@calendar=persian"),
    ).format(Date(ms))
    else DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(ms))
}

fun GameStatus.label() = when (this) {
    GameStatus.WANT_TO_PLAY -> tr("Want to play", "می‌خوام بازی کنم")
    GameStatus.BACKLOG -> tr("Backlog", "در صف")
    GameStatus.PLAYING -> tr("Playing", "در حال بازی")
    GameStatus.ON_HOLD -> tr("On hold", "متوقف")
    GameStatus.COMPLETED -> tr("Completed", "تمام‌شده")
    GameStatus.FULL_COMPLETION -> tr("100% done", "۱۰۰٪ تمام")
    GameStatus.ABANDONED -> tr("Abandoned", "رهاشده")
    GameStatus.REPLAYING -> tr("Replaying", "بازی مجدد")
    GameStatus.HIDDEN -> tr("Hidden", "پنهان")
}

fun Status.label() = when (this) {
    Status.WANT -> GameStatus.WANT_TO_PLAY.label()
    Status.BACKLOG -> GameStatus.BACKLOG.label()
    Status.PLAYING -> GameStatus.PLAYING.label()
    Status.ON_HOLD -> GameStatus.ON_HOLD.label()
    Status.COMPLETED -> GameStatus.COMPLETED.label()
    Status.PERFECT -> GameStatus.FULL_COMPLETION.label()
    Status.ABANDONED -> GameStatus.ABANDONED.label()
    Status.REPLAYING -> GameStatus.REPLAYING.label()
    Status.HIDDEN -> GameStatus.HIDDEN.label()
}

fun SourceType.label() = when (this) {
    SourceType.STEAM -> "Steam"
    SourceType.EPIC -> "Epic"
    SourceType.GOG -> "GOG"
    SourceType.OTHER_LAUNCHER -> tr("Other launcher", "لانچر دیگر")
    SourceType.STANDALONE -> tr("Standalone", "مستقل")
    SourceType.DRM_FREE -> tr("DRM-free", "بدون DRM")
    SourceType.EMULATOR -> tr("Emulator", "امولاتور")
    SourceType.MANUAL -> tr("Manual", "دستی")
    SourceType.CUSTOM -> tr("Custom", "سفارشی")
    SourceType.PHYSICAL -> tr("Physical", "فیزیکی")
}
