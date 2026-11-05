@file:OptIn(ExperimentalMaterial3Api::class)

package com.nexus.app

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nexus.core.database.*
import com.nexus.insights.WishlistItem
import com.nexus.insights.WishlistRules
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.UUID

@Composable
fun WishlistScreen(app: NexusApp, onBack: () -> Unit) {
    val dao = app.db.insights()
    val scope = rememberCoroutineScope()
    val rows by dao.wishlist().collectAsStateWithLifecycle(emptyList())
    var title by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("") }
    var remind by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val today = LocalDate.now()
    val items = rows.map { WishlistItem(it.id, it.title, it.releaseEpochDay?.let(LocalDate::ofEpochDay), it.remindDaysBefore, it.targetPriceCents) }
    val due = WishlistRules.dueReminders(items, today)

    SubScreen(tr("Wishlist", "فهرست آرزوها"), onBack) {
        if (due.isNotEmpty()) Panel {
            Text(tr("Reminders due today", "یادآورهای امروز"), style = MaterialTheme.typography.titleMedium)
            due.forEach { Text(it.title, color = NexusColors.Accent) }
        }
        Section(tr("Add a game", "افزودن بازی")) {
            OutlinedTextField(title, { title = it; error = null }, Modifier.fillMaxWidth(), singleLine = true, label = { Text(tr("Game title", "نام بازی")) })
            OutlinedTextField(date, { date = it }, Modifier.fillMaxWidth(), singleLine = true, label = { Text(tr("Release date YYYY-MM-DD (optional)", "تاریخ انتشار به‌صورت YYYY-MM-DD میلادی (اختیاری)")) })
            OutlinedTextField(remind, { remind = it.filter(Char::isDigit).take(3) }, Modifier.fillMaxWidth(), singleLine = true, label = { Text(tr("Remind me this many days before (optional)", "چند روز قبل یادآوری شود (اختیاری)")) })
            error?.let { Text(it, color = NexusColors.Danger, style = MaterialTheme.typography.bodySmall) }
            Button(onClick = {
                if (title.isBlank()) { error = tr("Title is required", "نام بازی لازمه"); return@Button }
                val d = if (date.isBlank()) null else runCatching { LocalDate.parse(date.trim()) }.getOrNull()
                if (date.isNotBlank() && d == null) { error = tr("Date must look like 2027-03-14", "تاریخ باید مثل 2027-03-14 باشه"); return@Button }
                scope.launch {
                    dao.upsertWish(WishlistEntity(UUID.randomUUID().toString(), title.trim(), d?.toEpochDay(), remind.toIntOrNull(), null, System.currentTimeMillis()))
                    title = ""; date = ""; remind = ""
                }
            }, Modifier.fillMaxWidth().height(48.dp)) { Text(tr("Add to wishlist", "افزودن به فهرست")) }
        }
        val ordered = WishlistRules.upcoming(items, today) + items.filter { it.releaseDate != null && it.releaseDate.isBefore(today) }
        if (ordered.isEmpty()) Hint(tr("Nothing on your wishlist yet.", "هنوز چیزی تو فهرست آرزوهات نیست."))
        ordered.forEach { w ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(w.title, style = MaterialTheme.typography.titleMedium)
                    val rd = w.releaseDate
                    Hint(when {
                        rd == null -> tr("Release date unknown", "تاریخ انتشار نامشخص")
                        rd.isBefore(today) -> tr("Released ", "منتشر شده: ") + formatDay(rd)
                        rd == today -> tr("Releases today", "امروز منتشر می‌شه")
                        else -> formatDay(rd) + " · " + tr("in ${ChronoUnit.DAYS.between(today, rd)} days", "${num(ChronoUnit.DAYS.between(today, rd).toInt())} روز دیگه")
                    })
                }
                TextButton(onClick = { scope.launch { dao.deleteWish(w.id) } }) { Text(tr("Remove", "حذف")) }
            }
        }
        Hint(tr("No price source is connected, so prices are not tracked. Reminders show here when you open the app; no background notification is scheduled yet.", "منبع قیمت وصل نیست، پس قیمت‌ها ردیابی نمی‌شن. یادآورها وقتی اپ رو باز می‌کنی اینجا نشون داده می‌شن؛ هنوز اعلان پس‌زمینه تنظیم نشده."))
    }
}

@Composable
fun JournalScreen(app: NexusApp, onBack: () -> Unit, onOpen: (String) -> Unit) {
    val dao = app.db.insights()
    val scope = rememberCoroutineScope()
    val entries by dao.journal().collectAsStateWithLifecycle(emptyList())
    val titles by produceState<Map<String, String>>(emptyMap(), entries) { value = dao.games().associate { it.id to it.title } }
    var q by remember { mutableStateOf("") }
    SubScreen(tr("Journal", "ژورنال"), onBack) {
        OutlinedTextField(q, { q = it }, Modifier.fillMaxWidth(), singleLine = true, placeholder = { Text(tr("Search entries", "جستجو در یادداشت‌ها")) })
        val shown = if (q.isBlank()) entries else com.nexus.insights.Journal.search(entries.map { com.nexus.insights.JournalEntry(it.id, it.gameId, it.createdAt, it.title, it.body, it.rating) }, q).map { r -> entries.first { it.id == r.id } }
        if (shown.isEmpty()) EmptyState(
            if (q.isBlank()) tr("No entries yet", "هنوز یادداشتی نیست") else tr("No match", "موردی پیدا نشد"),
            if (q.isBlank()) tr("Write one from a game's page.", "از صفحه‌ی یه بازی یکی بنویس.") else "",
        )
        shown.forEach { e ->
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(e.title.ifBlank { tr("(untitled)", "(بدون عنوان)") }, style = MaterialTheme.typography.titleMedium)
                Hint((titles[e.gameId] ?: tr("Game", "بازی")) + " · " + formatDate(e.createdAt))
                if (e.body.isNotBlank()) Text(e.body, style = MaterialTheme.typography.bodyMedium)
                Row {
                    TextButton(onClick = { onOpen(e.gameId) }) { Text(tr("Open game", "باز‌کردن بازی")) }
                    TextButton(onClick = { scope.launch { dao.deleteJournal(e.id) } }) { Text(tr("Delete", "حذف"), color = NexusColors.Danger) }
                }
            }
        }
    }
}

@Composable
fun CollectionsScreen(app: NexusApp, onBack: () -> Unit) {
    val dao = app.db.collections()
    val scope = rememberCoroutineScope()
    val cols by dao.all().collectAsStateWithLifecycle(emptyList())
    var name by remember { mutableStateOf("") }
    SubScreen(tr("Collections", "کلکسیون‌ها"), onBack) {
        Section(tr("New collection", "کلکسیون جدید")) {
            OutlinedTextField(name, { name = it }, Modifier.fillMaxWidth(), singleLine = true, label = { Text(tr("Name", "نام")) })
            Button(enabled = name.isNotBlank(), onClick = {
                scope.launch { dao.insert(CollectionEntity(UUID.randomUUID().toString(), name.trim(), position = cols.size)); name = "" }
            }, Modifier.fillMaxWidth().height(48.dp)) { Text(tr("Create", "ساختن")) }
        }
        if (cols.isEmpty()) Hint(tr("No collections yet. Add games to one from a game's page.", "هنوز کلکسیونی نیست. از صفحه‌ی یه بازی می‌تونی بازی‌ها رو اضافه کنی."))
        cols.forEach { c ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(c.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                TextButton(onClick = { scope.launch { dao.delete(c.id) } }) { Text(tr("Delete", "حذف"), color = NexusColors.Danger) }
            }
        }
        if (cols.isNotEmpty()) Hint(tr("Deleting a collection never deletes the games in it.", "حذف کلکسیون هیچ‌وقت بازی‌های داخلش رو حذف نمی‌کنه."))
    }
}
