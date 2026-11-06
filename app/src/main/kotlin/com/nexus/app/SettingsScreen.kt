package com.nexus.app

import androidx.compose.runtime.Composable

/** Honest status list. Nothing here is shown as ready unless it works in this build. */
@Composable
fun AboutRows() {
    fun row(name: String, state: String, note: String) = GroupRow(name, subtitle = note, value = state)
    Section("NEXUS 0.1.0 debug") {
        GroupedRows(listOf(
            row(tr("Library, add game, detail tabs", "کتابخانه، افزودن بازی، تب‌های جزئیات"), tr("In this build", "در این نسخه"), tr("Manual games, status, favorites, notes, collections, journal, media gallery, delete. Not yet checked on a device.", "بازی دستی، وضعیت، موردعلاقه، یادداشت، کلکسیون، ژورنال، گالری رسانه، حذف. هنوز روی دستگاه بررسی نشده.")),
            row(tr("Session timer", "تایمر جلسه"), tr("In this build", "در این نسخه"), tr("Start, pause, resume, finish, notification, recovery dialog. Not yet checked on a device.", "شروع، توقف، ادامه، پایان، اعلان، بازیابی جلسه‌ی نیمه‌تمام. هنوز روی دستگاه بررسی نشده.")),
            row(tr("Stats, Wrapped, Play next, Backlog, Wishlist", "آمار، مرور سال، بازی بعدی، صف، آرزوها"), tr("In this build", "در این نسخه"), tr("Read real data. Genre, length and achievement features stay empty because that data is not stored yet.", "از داده‌ی واقعی می‌خونن. بخش‌های ژانر، طول بازی و اچیومنت خالی می‌مونن چون این داده‌ها هنوز ذخیره نمی‌شن.")),
            row(tr("Backup, CSV, file import", "پشتیبان، CSV، ورود از فایل"), tr("In this build", "در این نسخه"), tr("JSON and CSV through the system file picker. Not yet checked on a device.", "JSON و CSV از طریق انتخاب‌گر فایل سیستم. هنوز روی دستگاه بررسی نشده.")),
            row("Steam", tr("Not available", "در دسترس نیست"), tr("No sign-in and no sync yet.", "هنوز ورود و همگام‌سازی نداره.")),
            row(tr("Windows PC", "ویندوز"), tr("Not available", "در دسترس نیست"), tr("Needs the Windows companion and a relay, which are not built. Nothing is tracked from a PC.", "به برنامه‌ی همراه ویندوز و رله نیاز داره که ساخته نشدن. چیزی از PC ردیابی نمی‌شه.")),
        ))
    }
}
