@file:OptIn(ExperimentalMaterial3Api::class)

package com.nexus.app

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nexus.core.database.GameStatus
import com.nexus.core.database.NewGame
import com.nexus.core.database.SourceType
import kotlinx.coroutines.launch

/** First step of "Add game": bring games in from a platform when possible, add by hand only for what no platform has. */
@Composable
fun AddChooserScreen(app: NexusApp, onBack: () -> Unit, onConnections: () -> Unit, onImport: () -> Unit, onManual: () -> Unit, onDone: () -> Unit) {
    val scope = rememberCoroutineScope()
    val acc by app.db.insights().account("STEAM").collectAsStateWithLifecycle(null)
    val syncing by app.platform.syncing.collectAsStateWithLifecycle()
    var result by remember { mutableStateOf<SyncOutcome?>(null) }
    var epicInfo by remember { mutableStateOf(false) }
    val connected = acc?.accountId != null

    SubScreen(tr("Add a game", "افزودن بازی"), onBack) {
        GroupedRows(listOf(
            GroupRow(
                tr("Sync from Steam", "همگام‌سازی از استیم"), NexusIcons.Library,
                subtitle = if (connected) tr("Last sync ", "آخرین همگام‌سازی ") + relativeDay(acc?.lastSyncAt) else tr("Connect your account first", "اول حسابت رو وصل کن"),
                trailing = if (syncing) { { CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp, color = NexusColors.Accent) } } else null,
                onClick = { if (!connected) onConnections() else if (!syncing) scope.launch { result = app.platform.sync() } },
            ),
            GroupRow(tr("Sync from Epic Games", "همگام‌سازی از اپیک"), NexusIcons.Library, subtitle = tr("Not available. Tap to see why.", "در دسترس نیست. برای دیدن دلیل بزن."), onClick = { epicInfo = true }),
            GroupRow(tr("Import from a file", "ورود از فایل"), NexusIcons.List, subtitle = tr("Epic (Legendary/Heroic), GOG or CSV lists", "فهرست Epic (لجندری/هیروئیک)، GOG یا CSV"), onClick = onImport),
            GroupRow(tr("Add manually", "افزودن دستی"), NexusIcons.Plus, subtitle = tr("For games no connected platform has", "برای بازی‌هایی که هیچ پلتفرم متصلی ندارتشون"), onClick = onManual),
        ))
        result?.let { r ->
            Panel {
                Text(outcomeText(r), style = MaterialTheme.typography.bodyMedium)
                if (r is SyncOutcome.Success) AccentButton(tr("See library", "دیدن کتابخانه"), onDone)
                else if (r !is SyncOutcome.NotConnected) GlassButton(tr("Platform settings", "تنظیمات پلتفرم‌ها"), onConnections)
            }
        }
    }
    if (epicInfo) AlertDialog(
        onDismissRequest = { epicInfo = false },
        title = { Text(tr("Epic Games is not available", "اپیک در دسترس نیست")) },
        text = { Text(tr("Epic has no official way for an app to read your library, and using unofficial login would put your account at risk, so NEXUS does not offer it. You can still bring Epic games in from a Legendary or Heroic export.", "اپیک راه رسمی برای خواندن کتابخانه توسط اپ نداره و ورود غیررسمی حسابت رو به خطر می‌ندازه، برای همین NEXUS ارائه‌اش نمی‌کنه. بازی‌های اپیک رو می‌تونی از خروجی Legendary یا Heroic وارد کنی.")) },
        confirmButton = { TextButton(onClick = { epicInfo = false; onImport() }) { Text(tr("Import from file", "ورود از فایل")) } },
        dismissButton = { TextButton(onClick = { epicInfo = false }) { Text(tr("Close", "بستن")) } },
    )
}

fun outcomeText(r: SyncOutcome): String = when (r) {
    is SyncOutcome.Success -> tr("Sync finished: ${r.added} new, ${r.updated} updated.", "همگام‌سازی تموم شد: ${num(r.added)} جدید، ${num(r.updated)} به‌روزشده.") +
        if (r.toReview.isNotEmpty()) tr(" ${r.toReview.size} share a title with an existing game and were kept separate.", " ${num(r.toReview.size)} مورد هم‌نام بازی‌ای بودن که از قبل بود و جدا موندن.") else ""
    SyncOutcome.NotConnected -> tr("No account is connected.", "حسابی وصل نیست.")
    SyncOutcome.NotFound -> tr("That Steam profile could not be found. Check the SteamID or profile name.", "این پروفایل استیم پیدا نشد. شناسه یا نام پروفایل رو بررسی کن.")
    SyncOutcome.InvalidKey -> tr("Steam rejected the key. Check that it is correct and still active.", "استیم کلید رو قبول نکرد. درست و فعال بودنش رو بررسی کن.")
    SyncOutcome.Private -> tr("Your Steam profile or game details are private, so Steam returns no games. Set Game details to Public in Steam privacy settings, then sync again.", "پروفایل یا جزئیات بازی‌های استیمت خصوصیه و استیم بازی‌ای نمی‌ده. تو تنظیمات حریم خصوصی استیم «جزئیات بازی» رو روی عمومی بذار و دوباره همگام‌سازی کن.")
    SyncOutcome.RateLimited -> tr("Steam is asking apps to slow down. Try again in a few minutes.", "استیم می‌خواد کندتر درخواست بدیم. چند دقیقه بعد دوباره امتحان کن.")
    SyncOutcome.Offline -> tr("No connection. Your saved library is still available.", "اتصال نیست. کتابخانه‌ی ذخیره‌شده‌ات هنوز در دسترسه.")
    is SyncOutcome.Failed -> tr("Steam answered with an error (${r.code}). Try again later.", "استیم خطا داد (${num(r.code)}). بعداً دوباره امتحان کن.")
}

@Composable
fun AddGameScreen(vm: AddGameViewModel, onBack: () -> Unit, onSaved: (String) -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var title by remember { mutableStateOf("") }
    var developer by remember { mutableStateOf("") }
    var publisher by remember { mutableStateOf("") }
    var year by remember { mutableStateOf("") }
    var genres by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var platform by remember { mutableStateOf<String?>(null) }
    var cover by remember { mutableStateOf<Uri?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    val platforms = listOf("PC", "PlayStation", "Xbox", "Switch", tr("Mobile", "موبایل"), tr("Other", "دیگر"))

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching { ctx.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            cover = uri
        }
    }

    SubScreen(tr("Add manually", "افزودن دستی"), onBack) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Cover(cover?.toString(), title.ifBlank { "?" }, Modifier.size(width = 96.dp, height = 132.dp), radius = 14)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                GlassButton(tr("Choose cover", "انتخاب کاور"), { picker.launch(arrayOf("image/*")) })
                if (cover != null) GlassButton(tr("Remove cover", "حذف کاور"), { cover = null })
            }
        }
        Section(tr("Game", "بازی")) {
            OutlinedTextField(title, { title = it; error = null }, Modifier.fillMaxWidth(), label = { Text(tr("Title *", "عنوان *")) }, singleLine = true, isError = error != null, supportingText = { error?.let { Text(it) } })
            OutlinedTextField(developer, { developer = it }, Modifier.fillMaxWidth(), label = { Text(tr("Developer", "سازنده")) }, singleLine = true)
            OutlinedTextField(publisher, { publisher = it }, Modifier.fillMaxWidth(), label = { Text(tr("Publisher", "ناشر")) }, singleLine = true)
            OutlinedTextField(year, { year = it.filter(Char::isDigit).take(4) }, Modifier.fillMaxWidth(), label = { Text(tr("Release year", "سال انتشار")) }, singleLine = true)
            OutlinedTextField(genres, { genres = it }, Modifier.fillMaxWidth(), label = { Text(tr("Genre", "ژانر")) }, singleLine = true)
            OutlinedTextField(description, { description = it }, Modifier.fillMaxWidth(), label = { Text(tr("Description", "توضیحات")) }, minLines = 3)
        }
<<<<<<< HEAD
        Section(tr("Status", "وضعیت")) {
            ChipRow(GameStatus.values().filter { it != GameStatus.HIDDEN }, status, { it.label() }) { status = it }
        }
        Button(
            modifier = Modifier.fillMaxWidth().height(48.dp),
            onClick = {
                if (title.isBlank()) { error = tr("Title is required", "عنوان لازمه"); return@Button }
                scope.launch {
                    val id = vm.save(
                        NewGame(
                            title = title, source = SourceType.MANUAL, developer = developer.ifBlank { null },
                            releaseYear = year.toIntOrNull(), status = status, coverUri = cover?.toString(),
                        )
=======
        Section(tr("Platform", "پلتفرم")) {
            ChipRow(platforms, platform, { it }) { platform = if (platform == it) null else it }
        }
        Hint(tr("Only the title is required. Manual games have no platform playtime.", "فقط عنوان لازمه. بازی‌های دستی ساعت بازی ندارن."))
        AccentButton(tr("Save", "ذخیره"), {
            if (title.isBlank()) { error = tr("Title is required", "عنوان لازمه"); return@AccentButton }
            scope.launch {
                val id = vm.save(
                    NewGame(
                        title = title, source = SourceType.MANUAL, developer = developer.ifBlank { null }, publisher = publisher.ifBlank { null },
                        releaseYear = year.toIntOrNull(), status = GameStatus.BACKLOG, coverUri = cover?.toString(),
                        description = description.ifBlank { null }, genres = genres.ifBlank { null }, platformLabel = platform,
>>>>>>> 7d946ca (NEXUS 0.3.0 redesign and release preparation)
                    )
                )
                onSaved(id)
            }
        }, Modifier.fillMaxWidth())
    }
}
