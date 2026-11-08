@file:OptIn(ExperimentalMaterial3Api::class)

package com.nexus.app

import android.app.ActivityManager
import android.content.Context
import android.os.PowerManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import com.nexus.polish.EffectLevel
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nexus.importers.*
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
        if (uri != null) scope.launch { message = if (ctx.writeText(uri, BackupData.libraryCsv(app.db))) tr("Library CSV saved.", "فایل CSV کتابخانه ذخیره شد.") else failWrite }
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
            OutlinedButton(onClick = { exportCsv.launch("nexus-library.csv") }, Modifier.fillMaxWidth().height(48.dp)) { Text(tr("Export library (CSV)", "خروجی کتابخانه (CSV)")) }
            Hint(tr("Backup includes games, status, favorites, notes and collections. It does not include artwork files, tags, journal, wishlist, or the hours platforms report; those come back with a sync.", "پشتیبان شامل بازی‌ها، وضعیت، موردعلاقه‌ها، یادداشت‌ها و کلکسیون‌هاست. فایل آرت‌ورک، برچسب‌ها، ژورنال، فهرست آرزوها و ساعت‌هایی که پلتفرم‌ها گزارش می‌دن توش نیست؛ اینا با همگام‌سازی برمی‌گردن."))
        }
        Section(tr("Restore", "بازگردانی")) {
            OutlinedButton(onClick = { importJson.launch(arrayOf("*/*")) }, Modifier.fillMaxWidth().height(48.dp)) { Text(tr("Choose a backup file", "انتخاب فایل پشتیبان")) }
            Hint(tr("Restore only adds what is missing. Nothing in your library is replaced or deleted.", "بازگردانی فقط چیزهای جاافتاده رو اضافه می‌کنه. هیچ چیزی از کتابخانه‌ات جایگزین یا حذف نمی‌شه."))
            pending?.let { (ok, plan) ->
                Text(tr("${plan.newGames.size} new games. ${plan.skippedExisting} already present.",
                    "${num(plan.newGames.size)} بازی‌ی جدید. ${num(plan.skippedExisting)} مورد از قبل وجود داره."))
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
<<<<<<< HEAD
                Button(
                    enabled = fresh.isNotEmpty(),
                    onClick = { scope.launch { val n = ImportData.apply(app.db, fresh); message = tr("Added $n games.", "${num(n)} بازی اضافه شد."); plan = null } },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                ) {
                    Text(tr("Add ${fresh.size} new games", "افزودن ${num(fresh.size)} بازی‌ی جدید"))
                }
                OutlinedButton(
                    enabled = review.isNotEmpty(),
                    onClick = { scope.launch { val n = ImportData.apply(app.db, review); message = tr("Added $n games as separate entries.", "${num(n)} بازی به‌صورت ورودی جدا اضافه شد."); plan = null } },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                ) {
=======
                Button(enabled = fresh.isNotEmpty(), onClick = { scope.launch { val n = ImportData.apply(app.db, fresh); message = tr("Added $n games.", "${num(n)} بازی اضافه شد."); plan = null } }, Modifier.fillMaxWidth().height(48.dp)) {
                    Text(tr("Add ${fresh.size} new games", "افزودن ${num(fresh.size)} بازی‌ی جدید"))
                }
                OutlinedButton(enabled = review.isNotEmpty(), onClick = { scope.launch { val n = ImportData.apply(app.db, review); message = tr("Added $n games as separate entries.", "${num(n)} بازی به‌صورت ورودی جدا اضافه شد."); plan = null } }, Modifier.fillMaxWidth().height(48.dp)) {
>>>>>>> 7d946ca (NEXUS 0.3.0 redesign and release preparation)
                    Text(tr("Add ${review.size} same-title games separately", "افزودن ${num(review.size)} بازی‌ی هم‌نام به‌صورت جدا"))
                }
            }
        }
        message?.let { Text(it, color = NexusColors.Accent) }
    }
}
<<<<<<< HEAD

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
    val fx = rememberEffects()
    val lvl = when (fx.level) { EffectLevel.HIGH -> tr("high", "بالا"); EffectLevel.BALANCED -> tr("balanced", "متعادل"); EffectLevel.LOW -> tr("low", "کم") }
    val langIndex = when (I18n.override) { null -> 0; Lang.EN -> 1; Lang.FA -> 2 }
    val fxOptions = listOf<EffectLevel?>(null, EffectLevel.HIGH, EffectLevel.BALANCED, EffectLevel.LOW)

    SubScreen(tr("Settings", "تنظیمات"), onBack) {
        Section(tr("Language", "زبان")) {
            val names = listOf(tr("Auto", "خودکار"), "English", "فارسی")
            SegmentedControl(3, langIndex, { I18n.choose(ctx, listOf(null, Lang.EN, Lang.FA)[it]) }, Modifier.fillMaxWidth()) { i, on -> SegmentLabel(names[i], on) }
        }
        Section(tr("Visual effects", "جلوه‌های بصری")) {
            val names = listOf(tr("Auto", "خودکار"), tr("High", "بالا"), tr("Balanced", "متعادل"), tr("Low", "کم"))
            SegmentedControl(4, fxOptions.indexOf(FxState.choice), { FxState.setChoice(ctx, fxOptions[it]) }, Modifier.fillMaxWidth()) { i, on -> SegmentLabel(names[i], on) }
            GroupedRows(listOf(GroupRow(tr("Reduce motion", "کاهش حرکت"), Icons.Default.Settings, trailing = { NexusSwitch(FxState.reduceMotion) { FxState.setReduceMotion(ctx, it) } })))
            Hint(tr("In use now: $lvl, blur ${if (fx.blur && android.os.Build.VERSION.SDK_INT >= 31) "on" else "off"}, animations ${if (fx.animations) "on" else "off"}. Real blur needs Android 12 or newer and the High level; otherwise a frosted tint is used. It is only applied to the floating tab bar, never to game cards.",
                "الان فعال: $lvl، بلور ${if (fx.blur && android.os.Build.VERSION.SDK_INT >= 31) "روشن" else "خاموش"}، انیمیشن ${if (fx.animations) "روشن" else "خاموش"}. بلور واقعی به اندروید ۱۲ یا بالاتر و سطح «بالا» نیاز داره؛ وگرنه یه رنگ مات شیشه‌ای استفاده می‌شه. فقط روی نوار تب شناور اعمال می‌شه، نه روی کارت بازی‌ها."))
        }
        Section(tr("Fonts", "فونت‌ها")) {
            GroupedRows(listOf(
                GroupRow("Inter", subtitle = tr("English text", "متن انگلیسی"), value = if (Fonts.interBundled) tr("Bundled", "داخل اپ") else tr("Not bundled", "داخل اپ نیست")),
                GroupRow("Vazirmatn", subtitle = tr("Persian text", "متن فارسی"), value = if (Fonts.vazirmatnBundled) tr("Bundled", "داخل اپ") else tr("Not bundled", "داخل اپ نیست")),
            ))
            Hint(tr("Both fonts use the SIL Open Font License 1.1. The licence texts ship in the app assets.", "هر دو فونت تحت مجوز SIL Open Font License 1.1 هستن. متن مجوزها داخل assets اپ قرار می‌گیره."))
        }
        AboutRows()
    }
}
=======
>>>>>>> 7d946ca (NEXUS 0.3.0 redesign and release preparation)
