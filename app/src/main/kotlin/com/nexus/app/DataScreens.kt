@file:OptIn(ExperimentalMaterial3Api::class)

package com.nexus.app

import android.app.ActivityManager
import android.content.Context
import android.os.PowerManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nexus.importers.*
import com.nexus.pc.DevicesViewModel
import com.nexus.polish.*
import kotlinx.coroutines.launch

@Composable
fun BackupScreen(app: NexusApp, onBack: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var message by remember { mutableStateOf<String?>(null) }
    var pending by remember { mutableStateOf<Pair<DecodeResult.Ok, RestorePlan>?>(null) }
    val failWrite = tr("Could not write the file.", "نوشتن فایل ممکن نشد.")
    val failRead = tr("Could not read the file.", "خواندن فایل ممکن نشد.")

    val exportJson = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) scope.launch { message = if (ctx.writeText(uri, BackupCodec.encode(BackupData.build(app.db)))) tr("Backup saved.", "پشتیبان ذخیره شد.") else failWrite }
    }
    val exportCsv = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri != null) scope.launch { message = if (ctx.writeText(uri, CsvExport.sessions(BackupData.build(app.db)))) tr("Session CSV saved.", "فایل CSV جلسه‌ها ذخیره شد.") else failWrite }
    }
    val importJson = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            val text = ctx.readText(uri)
            if (text == null) { message = failRead; return@launch }
            when (val r = BackupCodec.decode(text)) {
                is DecodeResult.Rejected -> { pending = null; message = r.reason }
                is DecodeResult.Ok -> { pending = r to BackupData.plan(app.db, r.backup); message = null }
            }
        }
    }

    SubScreen(tr("Backup and export", "پشتیبان‌گیری و خروجی"), onBack) {
        Section(tr("Export", "خروجی")) {
            Button(onClick = { exportJson.launch("nexus-backup.json") }, Modifier.fillMaxWidth().height(48.dp)) { Text(tr("Save backup (JSON)", "ذخیره‌ی پشتیبان (JSON)")) }
            OutlinedButton(onClick = { exportCsv.launch("nexus-sessions.csv") }, Modifier.fillMaxWidth().height(48.dp)) { Text(tr("Save sessions (CSV)", "ذخیره‌ی جلسه‌ها (CSV)")) }
            Hint(tr("Included: games, status, favorites, notes, phone sessions, manual adjustments, collections. Not included: artwork files, developer and year, tags, journal, wishlist, Windows sessions, store-reported playtime. Steam data comes back with a sync.", "شامل: بازی‌ها، وضعیت، موردعلاقه‌ها، یادداشت‌ها، جلسه‌های گوشی، تعدیل‌های دستی، کلکسیون‌ها. شامل نیست: فایل آرت‌ورک، سازنده و سال، برچسب‌ها، ژورنال، فهرست آرزوها، جلسه‌های ویندوز و زمان گزارش‌شده‌ی استور. داده‌ی استیم با همگام‌سازی برمی‌گرده."))
        }
        Section(tr("Restore", "بازگردانی")) {
            OutlinedButton(onClick = { importJson.launch(arrayOf("*/*")) }, Modifier.fillMaxWidth().height(48.dp)) { Text(tr("Choose a backup file", "انتخاب فایل پشتیبان")) }
            Hint(tr("Restore only adds what is missing. Nothing in your library is replaced or deleted.", "بازگردانی فقط چیزهای جاافتاده رو اضافه می‌کنه. هیچ چیزی از کتابخانه‌ات جایگزین یا حذف نمی‌شه."))
            pending?.let { (ok, plan) ->
                Text(tr("${plan.newGames.size} new games, ${plan.newSessions.size} sessions, ${plan.newAdjustments.size} adjustments. ${plan.skippedExisting} already present.",
                    "${num(plan.newGames.size)} بازی‌ی جدید، ${num(plan.newSessions.size)} جلسه، ${num(plan.newAdjustments.size)} تعدیل. ${num(plan.skippedExisting)} مورد از قبل وجود داره."))
                ok.warnings.take(5).forEach { Text(it, style = MaterialTheme.typography.bodySmall, color = NexusColors.Danger) }
                Button(onClick = { scope.launch { BackupData.restore(app.db, ok.backup, plan); message = tr("Restored.", "بازگردانی شد."); pending = null } }, Modifier.fillMaxWidth().height(48.dp)) { Text(tr("Restore now", "الان بازگردانی کن")) }
            }
        }
        message?.let { Text(it, color = NexusColors.Accent) }
    }
}

@Composable
fun ImportScreen(app: NexusApp, onBack: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var store by remember { mutableStateOf(Store.EPIC) }
    var plan by remember { mutableStateOf<List<PlanItem>?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    var skipped by remember { mutableStateOf(0) }
    val failRead = tr("Could not read the file.", "خواندن فایل ممکن نشد.")

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            val text = ctx.readText(uri)
            if (text == null) { message = failRead; return@launch }
            val parsed = if (store == Store.EPIC) EpicLegendaryParser.parse(text) else CsvTitleParser.parse(text, store)
            if (parsed.error != null) { message = parsed.error; plan = null; return@launch }
            skipped = parsed.skipped
            plan = ImportData.plan(app.db, parsed.games)
            message = null
        }
    }
    SubScreen(tr("Import from file", "ورود از فایل"), onBack) {
        Section(tr("Source", "منبع")) {
            ChipRow(Store.values().toList(), store, {
                when (it) {
                    Store.EPIC -> tr("Epic (Legendary/Heroic JSON)", "Epic (فایل JSON لجندری/هیروئیک)")
                    Store.GOG -> tr("GOG (CSV or text)", "GOG (فایل CSV یا متنی)")
                    Store.OTHER -> tr("Other launcher (CSV)", "لانچر دیگر (CSV)")
                }
            }) { store = it; plan = null }
            Hint(tr("Imports from a file you provide. No account is connected, so imported games are not marked as store-verified.", "از فایلی که خودت می‌دی وارد می‌کنه. حسابی وصل نمی‌شه، پس بازی‌های واردشده «تأییدشده‌ی استور» علامت نمی‌خورن."))
            Button(onClick = { picker.launch(arrayOf("*/*")) }, Modifier.fillMaxWidth().height(48.dp)) { Text(tr("Choose file", "انتخاب فایل")) }
        }
        plan?.let { p ->
            val fresh = p.filterIsInstance<PlanItem.New>().map { it.game }
            val review = p.filterIsInstance<PlanItem.NeedsReview>().map { it.game }
            val there = p.filterIsInstance<PlanItem.AlreadyThere>().size
            Section(tr("Review", "بازبینی")) {
                LabeledValue(tr("New games", "بازی‌های جدید"), num(fresh.size))
                LabeledValue(tr("Already in library", "از قبل تو کتابخانه"), num(there))
                LabeledValue(tr("Same title exists", "هم‌نام موجوده"), num(review.size), tr("Never merged automatically", "هیچ‌وقت خودکار ادغام نمی‌شه"))
                if (skipped > 0) LabeledValue(tr("Skipped lines", "خط‌های ردشده"), num(skipped))
                review.take(8).forEach { Hint("• " + it.title) }
                Button(enabled = fresh.isNotEmpty(), onClick = { scope.launch { val n = ImportData.apply(app.db, fresh); message = tr("Added $n games.", "${num(n)} بازی اضافه شد."); plan = null } }, modifier = Modifier.fillMaxWidth().height(48.dp)) {
                    Text(tr("Add ${fresh.size} new games", "افزودن ${num(fresh.size)} بازی‌ی جدید"))
                }
                OutlinedButton(enabled = review.isNotEmpty(), onClick = { scope.launch { val n = ImportData.apply(app.db, review); message = tr("Added $n games as separate entries.", "${num(n)} بازی به‌صورت ورودی جدا اضافه شد."); plan = null } }, modifier = Modifier.fillMaxWidth().height(48.dp)) {
                    Text(tr("Add ${review.size} same-title games separately", "افزودن ${num(review.size)} بازی‌ی هم‌نام به‌صورت جدا"))
                }
            }
        }
        message?.let { Text(it, color = NexusColors.Accent) }
    }
}

@Composable
fun DevicesScreen(app: NexusApp, onBack: () -> Unit) {
    val vm: DevicesViewModel = viewModel(factory = remember { NexusVmFactory(app) })
    val st by vm.state.collectAsStateWithLifecycle()
    SubScreen(tr("PC devices", "دستگاه‌های PC"), onBack) {
        Section(tr("Pair a PC", "جفت‌کردن یه PC")) {
            Hint(tr("The Windows companion app does not exist yet and no relay is configured, so a PC cannot pair today. The code below is only generated on this phone.", "برنامه‌ی همراه ویندوز هنوز ساخته نشده و رله‌ای تنظیم نیست، پس امروز هیچ PC‌ای نمی‌تونه جفت بشه. کد زیر فقط روی همین گوشی ساخته می‌شه."))
            val offer = st.offer
            if (offer == null) Button(onClick = vm::startPairing, Modifier.fillMaxWidth().height(48.dp)) { Text(tr("Create pairing code", "ساخت کد جفت‌سازی")) }
            else {
                Text(digits(offer.code), style = MaterialTheme.typography.headlineMedium, color = NexusColors.Accent)
                Hint(tr("Valid for 5 minutes", "اعتبار: ۵ دقیقه"))
                st.relayError?.let { Text(tr("No relay is configured yet.", "هنوز رله‌ای تنظیم نشده."), color = NexusColors.Danger, style = MaterialTheme.typography.bodySmall) }
                TextButton(onClick = vm::cancelPairing) { Text(tr("Cancel", "انصراف")) }
            }
        }
        if (st.devices.isEmpty()) Hint(tr("No paired PCs.", "هیچ PC جفت‌شده‌ای نیست."))
        st.devices.forEach { d ->
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                LabeledValue(d.name, if (d.revokedAt != null) tr("Revoked", "لغوشده") else tr("Paired", "جفت‌شده"))
                Hint("${d.os} · " + tr("companion ", "نسخه‌ی همراه ") + digits(d.companionVersion) + (d.lastSeenAt?.let { " · " + tr("last seen ", "آخرین اتصال: ") + formatDate(it) } ?: ""))
                if (d.revokedAt == null) TextButton(onClick = { vm.revoke(d.id) }) { Text(tr("Revoke access", "لغو دسترسی"), color = NexusColors.Danger) }
            }
        }
    }
}

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val prefs = remember { ctx.getSharedPreferences("nexus_prefs", Context.MODE_PRIVATE) }
    var choice by remember { mutableStateOf(prefs.getString("effects", null)?.let { runCatching { EffectLevel.valueOf(it) }.getOrNull() }) }
    var reduced by remember { mutableStateOf(prefs.getBoolean("reduced_motion", false)) }
    val am = ctx.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    val mem = ActivityManager.MemoryInfo().also { am.getMemoryInfo(it) }
    val pm = ctx.getSystemService(Context.POWER_SERVICE) as PowerManager
    val systemNoAnim = android.provider.Settings.Global.getFloat(ctx.contentResolver, android.provider.Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    val fx = EffectsPolicy.resolve(DeviceInfo((mem.totalMem / 1_048_576L).toInt(), pm.isPowerSaveMode, systemNoAnim, choice, reduced))

    SubScreen(tr("Settings and about", "تنظیمات و درباره"), onBack) {
        Section(tr("Language", "زبان")) {
            ChipRow(listOf<Lang?>(null, Lang.EN, Lang.FA), I18n.override, { when (it) { null -> tr("Automatic", "خودکار"); Lang.EN -> "English"; Lang.FA -> "فارسی" } }) { I18n.choose(ctx, it) }
        }
        Section(tr("Visual effects", "جلوه‌های بصری")) {
            ChipRow(listOf<EffectLevel?>(null) + EffectLevel.values(), choice, {
                when (it) { null -> tr("Automatic", "خودکار"); EffectLevel.HIGH -> tr("High", "بالا"); EffectLevel.BALANCED -> tr("Balanced", "متعادل"); EffectLevel.LOW -> tr("Low", "کم") }
            }) { choice = it; prefs.edit().putString("effects", it?.name).apply() }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(tr("Reduce motion", "کاهش حرکت"), Modifier.weight(1f))
                Switch(reduced, { reduced = it; prefs.edit().putBoolean("reduced_motion", it).apply() })
            }
            Hint(tr("In use now: ${fx.level.name.lowercase()}, blur ${if (fx.blur) "on" else "off"}, animations ${if (fx.animations) "on" else "off"}. The current screens use no blur or heavy animation yet.",
                "الان فعال: ${fx.level.name.lowercase()}، بلور ${if (fx.blur) "روشن" else "خاموش"}، انیمیشن ${if (fx.animations) "روشن" else "خاموش"}. صفحه‌های فعلی هنوز بلور یا انیمیشن سنگین ندارن."))
        }
        AboutRows()
    }
}
