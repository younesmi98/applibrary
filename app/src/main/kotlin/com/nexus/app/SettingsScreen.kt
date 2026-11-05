@file:OptIn(ExperimentalMaterial3Api::class)

package com.nexus.app

import androidx.compose.foundation.layout.*
import androidx.compose.ui.unit.dp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable

private data class Row3(val name: String, val state: String, val note: String)

@Composable
fun AboutRows() {
    val rows = listOf(
        Row3(tr("Library, add game, detail", "کتابخانه، افزودن بازی، جزئیات"), tr("In this build", "در این نسخه"), tr("Manual games, status, favorites, notes, tags, collections, journal, delete. Not yet checked on a device.", "بازی دستی، وضعیت، موردعلاقه، یادداشت، برچسب، کلکسیون، ژورنال، حذف. هنوز روی دستگاه بررسی نشده.")),
        Row3(tr("Session timer", "تایمر جلسه"), tr("In this build", "در این نسخه"), tr("Start, pause, resume, finish, notification, recovery dialog. Not yet checked on a device.", "شروع، توقف، ادامه، پایان، اعلان، بازیابی جلسه‌ی نیمه‌تمام. هنوز روی دستگاه بررسی نشده.")),
        Row3(tr("Home, stats, Wrapped, Play next, Backlog, Wishlist", "خانه، آمار، مرور سال، بازی بعدی، صف، آرزوها"), tr("In this build", "در این نسخه"), tr("Read real data. Genre, length and achievement based features stay empty because that data is not stored yet.", "از داده‌ی واقعی می‌خونن. بخش‌های وابسته به ژانر، طول بازی و اچیومنت خالی می‌مونن چون این داده‌ها هنوز ذخیره نمی‌شن.")),
        Row3(tr("Backup, CSV, file import", "پشتیبان، CSV، ورود از فایل"), tr("In this build", "در این نسخه"), tr("JSON and CSV through the system file picker. Not yet checked on a device.", "JSON و CSV از طریق انتخاب‌گر فایل سیستم. هنوز روی دستگاه بررسی نشده.")),
        Row3("Steam", tr("Code only", "فقط کد"), tr("Sync logic and server functions exist. No sign-in screen, no network client, never run.", "منطق همگام‌سازی و توابع سرور هست. صفحه‌ی ورود و کلاینت شبکه نداره و هیچ‌وقت اجرا نشده.")),
        Row3(tr("Windows PC", "ویندوز"), tr("Partly", "ناقص"), tr("Pairing codes, rules and session storage exist. No relay and no Windows app yet, so nothing is tracked from a PC.", "کد جفت‌سازی، قانون و ذخیره‌ی جلسه هست. رله و برنامه‌ی ویندوز نیست، پس چیزی از PC ردیابی نمی‌شه.")),
        Row3(tr("Not built", "ساخته نشده"), tr("Missing", "نیست"), tr("Metadata search, artwork download, duplicate merge, achievements, smart collections, comparison.", "جستجوی متادیتا، دانلود آرت‌ورک، ادغام تکراری‌ها، اچیومنت، کلکسیون هوشمند، مقایسه.")),
    )
    Section("NEXUS 0.1.0 debug") {
        rows.forEach { r ->
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                LabeledValue(r.name, r.state)
                Hint(r.note)
            }
        }
    }
}
