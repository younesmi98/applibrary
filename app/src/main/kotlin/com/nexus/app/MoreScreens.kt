@file:OptIn(ExperimentalMaterial3Api::class)

package com.nexus.app

import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.pm.PackageInfoCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.imageLoader
import com.nexus.polish.EffectLevel

/** The More tab. Every row opens something that works; nothing here is decoration. */
@Composable
fun MoreScreen(app: NexusApp, go: (Screen) -> Unit) {
    val ctx = LocalContext.current
    val acc by app.db.insights().account("STEAM").collectAsStateWithLifecycle(null)
    val syncing by app.platform.syncing.collectAsStateWithLifecycle()
    var confirmCache by remember { mutableStateOf(false) }
    fun r(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, value: String? = null, target: Screen) = GroupRow(label, icon, value = value, onClick = { go(target) })

    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(22.dp)) {
        PageTitle(tr("More", "بیشتر"))
        Section(tr("Account", "حساب")) {
            GroupedRows(listOf(
                r(tr("Profile", "پروفایل"), Icons.Default.Person, AppPrefs.profileName.ifBlank { null }, Screen.Profile),
                r(tr("Connected platforms", "پلتفرم‌های متصل"), Icons.Default.Share, when (PlatformSync.state(acc, syncing)) {
                    ConnectionState.NOT_CONNECTED -> tr("Not connected", "متصل نیست"); ConnectionState.CONNECTED -> tr("Steam connected", "استیم متصل")
                    ConnectionState.SYNCING -> tr("Syncing…", "در حال همگام‌سازی…"); ConnectionState.FAILED -> tr("Sync failed", "ناموفق"); ConnectionState.NEEDS_ACCESS -> tr("Needs access", "نیازمند دسترسی")
                }, Screen.Connections),
            ))
        }
        Section(tr("Library", "کتابخانه")) {
            GroupedRows(listOf(
                r(tr("Collections", "کلکسیون‌ها"), Icons.Default.Star, null, Screen.Collections),
                r(tr("Wishlist", "فهرست آرزوها"), Icons.Default.Favorite, null, Screen.Wishlist),
                r(tr("Journal", "ژورنال"), Icons.Default.Edit, null, Screen.Journal),
                r(tr("Play next", "بعدی چی بازی کنم"), Icons.Default.PlayArrow, null, Screen.PlayNext),
                r(tr("Backlog assistant", "دستیار صف بازی"), Icons.Default.Done, null, Screen.Backlog),
                r(tr("Import from file", "ورود از فایل"), Icons.Default.Add, null, Screen.Import),
                r(tr("Library preferences", "ترجیحات کتابخانه"), Icons.Default.Settings, null, Screen.LibraryPrefs),
            ))
        }
        Section(tr("Appearance", "ظاهر")) {
            GroupedRows(listOf(r(tr("Language, color, effects", "زبان، رنگ، جلوه‌ها"), Icons.Default.Face, null, Screen.Appearance)))
        }
        Section(tr("Data", "داده")) {
            GroupedRows(listOf(
                r(tr("Backup, restore and export", "پشتیبان، بازگردانی و خروجی"), Icons.Default.Refresh, null, Screen.Backup),
                GroupRow(tr("Clear image cache", "پاک‌کردن حافظه‌ی تصاویر"), Icons.Default.Delete, onClick = { confirmCache = true }),
            ))
        }
        Section(tr("About", "درباره")) {
            GroupedRows(listOf(r(tr("About NEXUS", "درباره‌ی NEXUS"), Icons.Default.Info, null, Screen.About)))
        }
        Spacer(Modifier.height(120.dp))
    }
    if (confirmCache) AlertDialog(
        onDismissRequest = { confirmCache = false },
        title = { Text(tr("Clear image cache?", "حافظه‌ی تصاویر پاک بشه؟")) },
        text = { Text(tr("Downloaded covers are removed from this phone and fetched again when needed. Your library and notes are not affected.", "کاورهای دانلودشده از گوشی پاک می‌شن و هر وقت لازم بشه دوباره گرفته می‌شن. کتابخانه و یادداشت‌هات تغییری نمی‌کنه.")) },
        confirmButton = {
            TextButton(onClick = {
                ctx.imageLoader.memoryCache?.clear(); ctx.imageLoader.diskCache?.clear(); confirmCache = false
                android.widget.Toast.makeText(ctx, tr("Image cache cleared", "حافظه‌ی تصاویر پاک شد"), android.widget.Toast.LENGTH_SHORT).show()
            }) { Text(tr("Clear", "پاک کن")) }
        },
        dismissButton = { TextButton(onClick = { confirmCache = false }) { Text(tr("Cancel", "انصراف")) } },
    )
}

@Composable
fun ProfileScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    var name by remember { mutableStateOf(AppPrefs.profileName) }
    var avatar by remember { mutableStateOf(AppPrefs.avatarUri) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching { ctx.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            avatar = uri.toString()
        }
    }
    SubScreen(tr("Profile", "پروفایل"), onBack) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Avatar(avatar, name.ifBlank { "N" }, 72)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                GlassButton(tr("Choose photo", "انتخاب عکس"), { picker.launch(arrayOf("image/*")) })
                if (avatar != null) GlassButton(tr("Remove photo", "حذف عکس"), { avatar = null })
            }
        }
        OutlinedTextField(name, { name = it.take(40) }, Modifier.fillMaxWidth(), singleLine = true, label = { Text(tr("Display name", "نام نمایشی")) })
        Hint(tr("Your profile stays on this phone. It is shown on the Home screen.", "پروفایلت روی همین گوشی می‌مونه و تو صفحه‌ی خانه نمایش داده می‌شه."))
        AccentButton(tr("Save", "ذخیره"), { AppPrefs.setProfile(ctx, name.trim(), avatar); onBack() }, Modifier.fillMaxWidth())
    }
}

@Composable
fun LibraryPrefsScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val sortIds = listOf(0, 3, 4, 1, 2)
    val sortNames = listOf(tr("Name", "نام"), tr("Playtime", "ساعت بازی"), tr("Last played", "آخرین اجرا"), tr("Recently added", "تازه‌ترین"), tr("Rating", "امتیاز"))
    SubScreen(tr("Library preferences", "ترجیحات کتابخانه"), onBack) {
        Section(tr("Default view", "نمای پیش‌فرض")) {
            val names = listOf(tr("List", "فهرست"), tr("Grid", "شبکه"))
            SegmentedControl(2, if (AppPrefs.defaultGrid) 1 else 0, { AppPrefs.setDefaultGrid(ctx, it == 1) }, Modifier.fillMaxWidth()) { i, on -> SegmentLabel(names[i], on) }
        }
        Section(tr("Default sort", "مرتب‌سازی پیش‌فرض")) {
            ChipRow(sortIds, AppPrefs.defaultSort, { sortNames[sortIds.indexOf(it)] }) { AppPrefs.setDefaultSort(ctx, it) }
            Hint(tr("Applies the next time the Library opens.", "دفعه‌ی بعد که کتابخانه باز بشه اعمال می‌شه."))
        }
        Section(tr("Automatic sync", "همگام‌سازی خودکار")) {
            GroupedRows(listOf(GroupRow(tr("Sync when NEXUS opens", "همگام‌سازی هنگام باز شدن اپ"), Icons.Default.Refresh, subtitle = tr("Only if Steam is connected and the last sync is over 6 hours old.", "فقط اگه استیم وصل باشه و آخرین همگام‌سازی بیشتر از ۶ ساعت پیش باشه."), trailing = { NexusSwitch(AppPrefs.autoSync) { AppPrefs.setAutoSync(ctx, it) } })))
        }
    }
}

@Composable
fun AppearanceScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val fx = rememberEffects()
    val fxOptions = listOf<EffectLevel?>(null, EffectLevel.HIGH, EffectLevel.BALANCED, EffectLevel.LOW)
    val lvl = when (fx.level) { EffectLevel.HIGH -> tr("high", "بالا"); EffectLevel.BALANCED -> tr("balanced", "متعادل"); EffectLevel.LOW -> tr("low", "کم") }
    val blurOn = fx.blur && Build.VERSION.SDK_INT >= 31
    SubScreen(tr("Appearance", "ظاهر"), onBack) {
        Section(tr("Language", "زبان")) {
            val names = listOf(tr("Auto", "خودکار"), "English", "فارسی")
            SegmentedControl(3, when (I18n.override) { null -> 0; Lang.EN -> 1; Lang.FA -> 2 }, { I18n.choose(ctx, listOf(null, Lang.EN, Lang.FA)[it]) }, Modifier.fillMaxWidth()) { i, on -> SegmentLabel(names[i], on) }
        }
        Section(tr("Accent color", "رنگ تأکیدی")) {
            val names = listOf(tr("Emerald", "زمردی"), tr("Lime", "لیمویی"), tr("Amber", "کهربایی"), tr("Sky", "آسمانی"))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                AppPrefs.accents.forEachIndexed { i, c ->
                    Column(Modifier.clip(CircleShape).clickable { AppPrefs.setAccent(ctx, i) }.padding(4.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(Modifier.size(40.dp).clip(CircleShape).background(c).border(if (AppPrefs.accent == i) 3.dp else 0.dp, NexusColors.TextPrimary, CircleShape))
                        Text(names[i], style = MaterialTheme.typography.bodySmall, color = if (AppPrefs.accent == i) NexusColors.TextPrimary else NexusColors.TextSecondary)
                    }
                }
            }
        }
        Section(tr("Background", "پس‌زمینه")) {
            val names = listOf(tr("Graphite", "گرافیتی"), tr("Deep black", "مشکی عمیق"))
            SegmentedControl(2, if (AppPrefs.deepBlack) 1 else 0, { AppPrefs.setDeepBlack(ctx, it == 1) }, Modifier.fillMaxWidth()) { i, on -> SegmentLabel(names[i], on) }
        }
        Section(tr("Effects and motion", "جلوه‌ها و حرکت")) {
            val names = listOf(tr("Auto", "خودکار"), tr("High", "بالا"), tr("Balanced", "متعادل"), tr("Low", "کم"))
            SegmentedControl(4, fxOptions.indexOf(FxState.choice), { FxState.setChoice(ctx, fxOptions[it]) }, Modifier.fillMaxWidth()) { i, on -> SegmentLabel(names[i], on) }
            GroupedRows(listOf(GroupRow(tr("Reduce motion", "کاهش حرکت"), Icons.Default.Settings, trailing = { NexusSwitch(FxState.reduceMotion) { FxState.setReduceMotion(ctx, it) } })))
            Hint(tr("In use now: $lvl, blur ${if (blurOn) "on" else "off"}, animations ${if (fx.animations) "on" else "off"}. Real blur needs Android 12 or newer and the High level, and is used only on the floating tab bar.",
                "الان فعال: $lvl، بلور ${if (blurOn) "روشن" else "خاموش"}، انیمیشن ${if (fx.animations) "روشن" else "خاموش"}. بلور واقعی به اندروید ۱۲ یا بالاتر و سطح «بالا» نیاز داره و فقط روی نوار تب شناور استفاده می‌شه."))
        }
        Section(tr("Fonts", "فونت‌ها")) {
            GroupedRows(listOf(
                GroupRow("Inter", subtitle = tr("English text", "متن انگلیسی"), value = if (Fonts.interBundled) tr("Bundled", "داخل اپ") else tr("Not bundled", "داخل اپ نیست")),
                GroupRow("Vazirmatn", subtitle = tr("Persian text", "متن فارسی"), value = if (Fonts.vazirmatnBundled) tr("Bundled", "داخل اپ") else tr("Not bundled", "داخل اپ نیست")),
            ))
        }
    }
}

@Composable
fun AboutScreen(onBack: () -> Unit, go: (Screen) -> Unit) {
    val ctx = LocalContext.current
    val info = remember { runCatching { ctx.packageManager.getPackageInfo(ctx.packageName, 0) }.getOrNull() }
    SubScreen(tr("About NEXUS", "درباره‌ی NEXUS"), onBack) {
        Section(tr("Version", "نسخه")) {
            LabeledValue(tr("Version", "نسخه"), info?.versionName ?: "—")
            LabeledValue(tr("Build number", "شماره‌ی ساخت"), info?.let { digits(PackageInfoCompat.getLongVersionCode(it).toString()) } ?: "—")
        }
        GroupedRows(listOf(
            GroupRow(tr("Changelog", "تغییرات نسخه‌ها"), Icons.Default.DateRange, onClick = { go(Screen.Changelog) }),
            GroupRow(tr("Open source licenses", "مجوزهای متن‌باز"), Icons.Default.Info, onClick = { go(Screen.Licenses) }),
        ))
        Section(tr("Build information", "اطلاعات ساخت")) {
            LabeledValue(tr("Package", "بسته"), ctx.packageName)
            LabeledValue("Android", digits("${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"))
            LabeledValue(tr("Device", "دستگاه"), "${Build.MANUFACTURER} ${Build.MODEL}")
            LabeledValue(tr("Library database", "پایگاه‌داده‌ی کتابخانه"), digits("v7"))
        }
    }
}

@Composable
fun ChangelogScreen(onBack: () -> Unit) {
    SubScreen(tr("Changelog", "تغییرات نسخه‌ها"), onBack) {
        Section("0.3.0") {
            listOf(
                tr("Added the eight-spoke iOS-style NEXUS startup loader with a local database-ready state and retry message.", "لودینگ آغازین هشت‌پره با سبک iOS اضافه شد؛ آماده‌شدن دیتابیس محلی و امکان تلاش دوباره هم نمایش داده می‌شه."),
                tr("Prepared Android 16 edge-to-edge layout with safe insets and transparent system bars.", "چیدمان لبه‌به‌لبه‌ی اندروید ۱۶ با رعایت حاشیه‌ی امن و نوارهای سیستم شفاف آماده شد."),
                tr("Updated release build settings and optional private-keystore signing. No signing key is bundled.", "تنظیمات ساخت نسخه‌ی انتشار و امضای اختیاری با کلید خصوصی به‌روزرسانی شد؛ هیچ کلید امضایی داخل پروژه نیست."),
                tr("Fixed duplicate theme imports, Material color tokens unavailable in the pinned Compose version, and the old session wording in game deletion.", "ایمپورت تکراری تم، رنگ‌هایی که در نسخه‌ی فعلی Compose وجود نداشتند و متن قدیمی جلسه در حذف بازی اصلاح شد."),
            ).forEach { Text("• $it", style = MaterialTheme.typography.bodyMedium) }
        }
        Section("0.2.0") {
            listOf(
                tr("Removed the session timer and manual playtime. Hours now come only from connected platforms.", "تایمر جلسه و ثبت دستی ساعت حذف شد. ساعت‌ها فقط از پلتفرم‌های متصل می‌یان."),
                tr("Steam connection with your own Web API key: library, total hours, last two weeks, last played, running game.", "اتصال استیم با کلید Web API خودت: کتابخانه، مجموع ساعت، دو هفته‌ی اخیر، آخرین اجرا، بازی در حال اجرا."),
                tr("New Home dashboard, library sorting by hours and last played, read-only platform stats on every game.", "داشبورد جدید خانه، مرتب‌سازی کتابخانه بر اساس ساعت و آخرین اجرا، آمار فقط‌خواندنی پلتفرم روی هر بازی."),
                tr("Emerald graphite design, fixed unreadable text, new More and Settings structure.", "طراحی گرافیتی زمردی، رفع متن‌های ناخوانا، ساختار جدید بیشتر و تنظیمات."),
                tr("Database moved to version 7. Old session tables are kept in the file but no longer used; a game set to Playing by hand became Backlog.", "پایگاه‌داده به نسخه‌ی ۷ رفت. جدول‌های قدیمی جلسه تو فایل می‌مونن ولی استفاده نمی‌شن؛ بازی‌هایی که دستی روی «در حال بازی» بودن به «در انتظار بازی» تبدیل شدن."),
            ).forEach { Text("• $it", style = MaterialTheme.typography.bodyMedium) }
        }
    }
}

@Composable
fun LicensesScreen(onBack: () -> Unit) {
    val items = listOf(
        "AndroidX (Compose, Room, Paging, Lifecycle, Navigation, Core)" to "Apache License 2.0",
        "Kotlin, kotlinx.coroutines, kotlinx.serialization" to "Apache License 2.0",
        "Coil" to "Apache License 2.0",
        "Haze (Chris Banes)" to "Apache License 2.0",
        "Inter font" to "SIL Open Font License 1.1",
        "Vazirmatn font" to "SIL Open Font License 1.1",
    )
    SubScreen(tr("Open source licenses", "مجوزهای متن‌باز"), onBack) {
        GroupedRows(items.map { (n, l) -> GroupRow(n, value = l) })
        Hint(tr("Font license texts are included in the app assets (fonts folder).", "متن مجوز فونت‌ها داخل assets اپ (پوشه‌ی fonts) هست."))
    }
}
