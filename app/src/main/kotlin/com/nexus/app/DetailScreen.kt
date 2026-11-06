@file:OptIn(ExperimentalMaterial3Api::class)

package com.nexus.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nexus.core.database.GameStatus
import com.nexus.playtime.PlaytimeCalc
import com.nexus.playtime.SessionState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private fun LazyListScope.padded(content: @Composable ColumnScope.() -> Unit) {
    item { Column(Modifier.padding(horizontal = 20.dp), content = content) }
}

private fun android.content.Context.keepAccess(uri: Uri) {
    runCatching { contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
}

@Composable
fun DetailScreen(vm: DetailViewModel, onBack: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val ready by vm.ready.collectAsStateWithLifecycle()
    val game by vm.game.collectAsStateWithLifecycle()
    val sources by vm.sources.collectAsStateWithLifecycle()
    val tags by vm.tags.collectAsStateWithLifecycle()
    val live by vm.activeSession.collectAsStateWithLifecycle()
    val otherRunning by vm.otherGameRunning.collectAsStateWithLifecycle()
    val pt by vm.playtime.collectAsStateWithLifecycle()
    val history by vm.history.collectAsStateWithLifecycle()
    val adjustments by vm.adjustments.collectAsStateWithLifecycle()
    val images by vm.images.collectAsStateWithLifecycle()

    var tab by rememberSaveable { mutableIntStateOf(0) }
    var showAdjust by remember { mutableStateOf(false) }
    var showDelete by remember { mutableStateOf(false) }
    var showPermInfo by remember { mutableStateOf(false) }
    var notes by remember(game?.id) { mutableStateOf(game?.notes ?: "") }

    fun startSession() = scope.launch {
        val id = vm.start()
        if (id != null) SessionNotifier.push(ctx, id)
    }
    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { startSession() }
    val pickCover = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> uri?.let { ctx.keepAccess(it); vm.setCover(it.toString()) } }
    val pickBackground = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> uri?.let { ctx.keepAccess(it); vm.setBackground(it.toString()) } }
    val pickGallery = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isNotEmpty()) { uris.forEach { ctx.keepAccess(it) }; vm.addImages(uris.map { it.toString() }) }
    }

    val g = game
    if (g == null) {
        if (!ready) DetailSkeleton()
        else Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(tr("Game not found", "بازی پیدا نشد")) }
        return
    }

    LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(24.dp), contentPadding = PaddingValues(bottom = 48.dp)) {
        // ---- hero: artwork first, text over a scrim
        item {
            Box(Modifier.fillMaxWidth().height(260.dp).clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))) {
                Cover(g.backgroundUri ?: g.coverUri, g.title, Modifier.fillMaxSize(), radius = 0, showTitle = false)
                Box(Modifier.fillMaxSize().background(ScrimBrush))
                IconButton(onClick = onBack, Modifier.align(Alignment.TopStart).padding(8.dp)) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, tr("Back", "بازگشت"), Modifier.size(32.dp), tint = Color.White)
                }
                IconButton(onClick = vm::toggleFavorite, Modifier.align(Alignment.TopEnd).padding(8.dp)) {
                    Icon(if (g.favorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder, if (g.favorite) tr("Remove from favorites", "حذف از موردعلاقه‌ها") else tr("Add to favorites", "افزودن به موردعلاقه‌ها"), tint = if (g.favorite) NexusColors.Accent else Color.White)
                }
                Column(Modifier.align(Alignment.BottomStart).padding(20.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(g.title, style = MaterialTheme.typography.headlineMedium, color = Color.White)
                    g.originalTitle?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.8f)) }
                    val meta = listOfNotNull(g.developer, g.releaseYear?.let { num(it) }).joinToString(" · ")
                    if (meta.isNotBlank()) Text(meta, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.8f))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        sources.forEach { PlatformBadge(it.type) }
                        Text(g.status.label(), style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.85f))
                    }
                }
            }
        }

        // ---- tabs: each one has its own content
        padded {
            val names = listOf(tr("Overview", "نمای کلی"), tr("Statistics", "آمار"), tr("Media", "رسانه"))
            SegmentedControl(3, tab, { tab = it }, Modifier.fillMaxWidth()) { i, on -> SegmentLabel(names[i], on) }
        }

        when (tab) {
            0 -> {
                padded {
                    Section(tr("Status", "وضعیت")) {
                        ChipRow(GameStatus.values().filter { it != GameStatus.HIDDEN }, g.status, { it.label() }) { vm.setStatus(it) }
                    }
                }
                padded {
                    Panel {
                        val l = live
                        if (l == null) {
                            if (otherRunning) Hint(tr("Another game has a session running. Finish it first.", "یه بازی دیگه جلسه‌ی فعال داره. اول اون رو تموم کن."))
                            AccentButton(tr("Start session", "شروع جلسه"), {
                                if (otherRunning) return@AccentButton
                                val needs = Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                                if (needs) showPermInfo = true else startSession()
                            }, Modifier.fillMaxWidth(), icon = NexusIcons.Clock)
                        } else {
                            val tick by produceState(System.currentTimeMillis()) { while (true) { value = System.currentTimeMillis(); delay(1000) } }
                            val paused = l.session.state == SessionState.PAUSED
                            Text(formatClock(PlaytimeCalc.activeMillis(l.session, l.pauses, tick)), style = MaterialTheme.typography.headlineMedium, color = if (paused) NexusColors.TextSecondary else NexusColors.Accent)
                            Hint(if (paused) tr("Paused", "متوقف‌شده") else tr("Running since ", "شروع: ") + formatDate(l.session.startedAt))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                GlassButton(if (paused) tr("Resume", "ادامه") else tr("Pause", "توقف"), {
                                    scope.launch { if (paused) vm.resume(l.session.id) else vm.pause(l.session.id); SessionNotifier.push(ctx, l.session.id) }
                                }, Modifier.weight(1f))
                                AccentButton(tr("Finish", "پایان"), { scope.launch { vm.finish(l.session.id); SessionNotifier.stop(ctx) } }, Modifier.weight(1f))
                            }
                        }
                    }
                }
                padded {
                    Section(tr("Notes", "یادداشت‌ها")) {
                        OutlinedTextField(notes, { notes = it }, Modifier.fillMaxWidth(), minLines = 3, placeholder = { Text(tr("Your notes", "یادداشت‌های تو")) })
                        if (notes != (g.notes ?: "")) TextButton(onClick = { vm.saveNotes(notes) }) { Text(tr("Save notes", "ذخیره‌ی یادداشت")) }
                        if (tags.isNotEmpty()) Hint(tr("Tags: ", "برچسب‌ها: ") + tags.joinToString(tr(", ", "، ")))
                    }
                }
                padded { CollectionsPanel(vm) }
                padded { JournalPanel(vm) }
                padded { TextButton(onClick = { showDelete = true }) { Text(tr("Remove from library", "حذف از کتابخانه"), color = NexusColors.Danger) } }
            }

            1 -> {
                padded {
                    Section(tr("Playtime by source", "زمان بازی به تفکیک منبع")) {
                        LabeledValue(tr("Steam reported", "گزارش استیم"), pt.steamMinutes?.let { formatDuration(it * 60_000L) } ?: "—", tr("Store figure; may already include other time", "عدد خود استور؛ ممکنه زمان‌های دیگه رو هم شامل بشه"))
                        LabeledValue(tr("Windows tracked", "ثبت‌شده در ویندوز"), if (pt.windowsMillis > 0) formatDuration(pt.windowsMillis) else "—", tr("From a paired PC", "از PC جفت‌شده"))
                        LabeledValue(tr("Phone sessions", "جلسه‌های گوشی"), if (pt.phoneSessionMillis > 0) formatDuration(pt.phoneSessionMillis) else "—", tr("Sessions started in this app", "جلسه‌هایی که تو همین اپ شروع شدن"))
                        LabeledValue(tr("Manual adjustments", "تعدیل دستی"), if (pt.adjustmentMillis != 0L) (if (pt.adjustmentMillis > 0) "+" else "−") + formatDuration(kotlin.math.abs(pt.adjustmentMillis)) else "—", tr("One-time entries", "ورودی‌های یک‌باره"))
                        Hint(tr("These numbers are never added together, because they can overlap.", "این اعدادها جمع زده نمی‌شن چون ممکنه هم‌پوشانی داشته باشن."))
                        TextButton(onClick = { showAdjust = true }) { Text(tr("Add or remove time by hand", "افزودن یا کم‌کردن زمان دستی")) }
                    }
                }
                val bars = listOf(
                    tr("Steam", "استیم") to (pt.steamMinutes ?: 0).toFloat(),
                    tr("Windows", "ویندوز") to pt.windowsMillis / 60_000f,
                    tr("Phone", "گوشی") to pt.phoneSessionMillis / 60_000f,
                )
                if (bars.any { it.second > 0f }) padded {
                    Section(tr("Side by side, not summed", "کنار هم، بدون جمع")) {
                        val max = bars.maxOf { it.second }
                        bars.forEach { (n, v) -> BarRow(n, v, max, if (v > 0f) formatDuration((v * 60_000f).toLong()) else "—") }
                    }
                }
                val recent = history.take(8)
                if (recent.isNotEmpty()) padded {
                    Section(tr("Recent sessions", "جلسه‌های اخیر")) {
                        val max = recent.maxOf { it.second }.toFloat()
                        recent.forEach { (s, ms) -> BarRow(formatDate(s.startedAt), ms.toFloat(), max, formatDuration(ms)) }
                    }
                } else padded { Hint(tr("No finished sessions yet. Charts appear after you track time.", "هنوز جلسه‌ی تمام‌شده‌ای نیست. بعد از ثبت زمان، نمودارها پر می‌شن.")) }

                if (adjustments.isNotEmpty()) padded { Section(tr("Adjustment history", "تاریخچه‌ی تعدیل‌ها")) {} }
                items(adjustments, key = { it.id }) { a ->
                    Row(Modifier.padding(horizontal = 20.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text((if (a.deltaMillis >= 0) "+" else "−") + formatDuration(kotlin.math.abs(a.deltaMillis)), style = MaterialTheme.typography.bodyLarge)
                            Hint(listOfNotNull(formatDate(a.createdAt), a.reason).joinToString(" · "))
                        }
                        TextButton(onClick = { vm.undoAdjustment(a.id) }) { Text(tr("Undo", "بازگردانی")) }
                    }
                }
                if (history.isNotEmpty()) padded { Section(tr("Session history", "تاریخچه‌ی جلسه‌ها")) {} }
                items(history, key = { it.first.id }) { (s, ms) ->
                    Row(Modifier.padding(horizontal = 20.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) { Text(formatDuration(ms), style = MaterialTheme.typography.bodyLarge); Hint(formatDate(s.startedAt)) }
                        TextButton(onClick = { vm.deleteSession(s.id) }) { Text(tr("Delete", "حذف")) }
                    }
                }
                padded { RulesPanel(vm) }
            }

            else -> {
                padded {
                    Section(tr("Cover", "کاور")) {
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Cover(g.coverUri, g.title, Modifier.size(width = 110.dp, height = 154.dp), radius = 14, showTitle = false)
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                GlassButton(tr("Choose image", "انتخاب تصویر"), { pickCover.launch(arrayOf("image/*")) })
                                if (g.coverUri != null) GlassButton(tr("Remove", "حذف"), { vm.setCover(null) })
                            }
                        }
                    }
                }
                padded {
                    Section(tr("Background", "پس‌زمینه")) {
                        Cover(g.backgroundUri, g.title, Modifier.fillMaxWidth().aspectRatio(16f / 9f), radius = 14, showTitle = false)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            GlassButton(tr("Choose image", "انتخاب تصویر"), { pickBackground.launch(arrayOf("image/*")) }, Modifier.weight(1f))
                            if (g.backgroundUri != null) GlassButton(tr("Remove", "حذف"), { vm.setBackground(null) }, Modifier.weight(1f))
                        }
                        Hint(tr("The background is used at the top of this page. Without one, the cover is used.", "پس‌زمینه بالای همین صفحه استفاده می‌شه. اگه نباشه، کاور استفاده می‌شه."))
                    }
                }
                padded {
                    Section(tr("Gallery", "گالری")) {
                        if (images.isEmpty()) Hint(tr("No images yet. Add screenshots or artwork from your phone.", "هنوز تصویری نیست. اسکرین‌شات یا آرت‌ورک رو از گوشی اضافه کن."))
                        images.chunked(3).forEach { row ->
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                row.forEach { img ->
                                    Box(Modifier.weight(1f).aspectRatio(1f)) {
                                        Cover(img.uri, "", Modifier.fillMaxSize(), radius = 12, showTitle = false)
                                        Box(
                                            Modifier.align(Alignment.TopEnd).padding(4.dp).size(26.dp).clip(CircleShape).background(Color(0x99000000)).clickable { vm.removeImage(img.id) },
                                            contentAlignment = Alignment.Center,
                                        ) { Icon(Icons.Default.Close, tr("Remove image", "حذف تصویر"), Modifier.size(14.dp), tint = Color.White) }
                                    }
                                }
                                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                            }
                        }
                        AccentButton(tr("Add images", "افزودن تصویر"), { pickGallery.launch(arrayOf("image/*")) }, Modifier.fillMaxWidth(), icon = NexusIcons.Plus)
                    }
                }
            }
        }
    }

    if (showPermInfo) AlertDialog(
        onDismissRequest = { showPermInfo = false },
        title = { Text(tr("Show the running timer?", "تایمر در اعلان نمایش داده بشه؟")) },
        text = { Text(tr("NEXUS needs notification permission to show your session timer with Pause and Finish buttons while the app is in the background. You can still track without it.", "برای نمایش تایمر جلسه با دکمه‌های توقف و پایان وقتی اپ در پس‌زمینه‌ست، NEXUS به اجازه‌ی اعلان نیاز داره. بدون اون هم می‌تونی زمان رو ثبت کنی.")) },
        confirmButton = { TextButton(onClick = { showPermInfo = false; if (Build.VERSION.SDK_INT >= 33) permLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) else startSession() }) { Text(tr("Continue", "ادامه")) } },
        dismissButton = { TextButton(onClick = { showPermInfo = false; startSession() }) { Text(tr("Skip", "رد‌کردن")) } },
    )

    if (showAdjust) {
        var minutes by remember { mutableStateOf("") }
        var reason by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAdjust = false },
            title = { Text(tr("Adjust playtime", "تعدیل زمان بازی")) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Hint(tr("Minutes to add. Use a minus sign to remove.", "دقیقه‌ای که اضافه می‌شه. برای کم‌کردن علامت منفی بذار."))
                    OutlinedTextField(minutes, { minutes = it.filterIndexed { i, c -> c.isDigit() || (i == 0 && c == '-') }.take(6) }, singleLine = true, label = { Text(tr("Minutes", "دقیقه")) })
                    OutlinedTextField(reason, { reason = it }, singleLine = true, label = { Text(tr("Reason (optional)", "دلیل (اختیاری)")) })
                }
            },
            confirmButton = { TextButton(onClick = { minutes.toLongOrNull()?.let { vm.addAdjustment(it, reason.ifBlank { null }) }; showAdjust = false }) { Text(tr("Save", "ذخیره")) } },
            dismissButton = { TextButton(onClick = { showAdjust = false }) { Text(tr("Cancel", "انصراف")) } },
        )
    }

    if (showDelete) AlertDialog(
        onDismissRequest = { showDelete = false },
        title = { Text(tr("Remove ${g.title}?", "حذف ${g.title}؟")) },
        text = { Text(tr("This removes the library entry with its notes, sessions and gallery. Game files on your devices are never touched.", "این کار ورودی کتابخانه رو همراه یادداشت‌ها، جلسه‌ها و گالری‌اش حذف می‌کنه. فایل‌های بازی روی دستگاه‌هات هیچ‌وقت دست نمی‌خورن.")) },
        confirmButton = { TextButton(onClick = { showDelete = false; vm.delete(); onBack() }) { Text(tr("Remove", "حذف"), color = NexusColors.Danger) } },
        dismissButton = { TextButton(onClick = { showDelete = false }) { Text(tr("Cancel", "انصراف")) } },
    )
}
