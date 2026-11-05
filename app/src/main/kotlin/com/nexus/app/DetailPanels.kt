@file:OptIn(ExperimentalMaterial3Api::class)

package com.nexus.app

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nexus.pc.TrackingMode

@Composable
fun JournalPanel(vm: DetailViewModel) {
    val entries by vm.journal.collectAsStateWithLifecycle()
    var adding by remember { mutableStateOf(false) }
    Section(tr("Journal", "ژورنال")) {
        if (entries.isEmpty()) Hint(tr("No entries yet.", "هنوز یادداشتی ننوشتی."))
        entries.forEach { e ->
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(e.title.ifBlank { tr("(untitled)", "(بدون عنوان)") }, style = MaterialTheme.typography.titleMedium)
                Hint(formatDate(e.createdAt))
                if (e.body.isNotBlank()) Text(e.body, style = MaterialTheme.typography.bodyMedium)
                TextButton(onClick = { vm.deleteJournal(e.id) }) { Text(tr("Delete", "حذف"), color = NexusColors.Danger) }
            }
        }
        TextButton(onClick = { adding = true }) { Text(tr("Write an entry", "نوشتن یادداشت")) }
    }
    if (adding) {
        var t by remember { mutableStateOf("") }
        var b by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { adding = false },
            title = { Text(tr("New journal entry", "یادداشت جدید")) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(t, { t = it }, singleLine = true, label = { Text(tr("Title", "عنوان")) })
                    OutlinedTextField(b, { b = it }, minLines = 4, label = { Text(tr("What happened?", "چی شد؟")) })
                }
            },
            confirmButton = { TextButton(enabled = t.isNotBlank() || b.isNotBlank(), onClick = { vm.addJournal(t.trim(), b.trim()); adding = false }) { Text(tr("Save", "ذخیره")) } },
            dismissButton = { TextButton(onClick = { adding = false }) { Text(tr("Cancel", "انصراف")) } },
        )
    }
}

@Composable
fun CollectionsPanel(vm: DetailViewModel) {
    val all by vm.allCollections.collectAsStateWithLifecycle()
    val mine by vm.myCollectionIds.collectAsStateWithLifecycle()
    Section(tr("Collections", "کلکسیون‌ها")) {
        if (all.isEmpty()) Hint(tr("Create a collection from More > Collections first.", "اول از بخش بیشتر > کلکسیون‌ها یه کلکسیون بساز."))
        else ChipRow(all, null, { (if (it.id in mine) "✓ " else "") + it.name }) { vm.toggleCollection(it.id, it.id in mine) }
    }
}

@Composable
fun RulesPanel(vm: DetailViewModel) {
    val rules by vm.rules.collectAsStateWithLifecycle()
    var exe by remember { mutableStateOf("") }
    var mode by remember { mutableStateOf(TrackingMode.WHILE_PROCESS_RUNS) }
    var idle by remember { mutableStateOf("") }
    fun modeName(m: TrackingMode) = if (m == TrackingMode.WHILE_FOREGROUND) tr("Foreground only", "فقط وقتی جلوئه") else tr("While running", "تا وقتی اجراست")
    Section(tr("Windows tracking rule", "قانون ردیابی ویندوز")) {
        Hint(tr("Used by the optional Windows companion. Only the executable name is saved, never a folder path. The companion app is not built yet, so nothing is tracked from this rule today.", "برای برنامه‌ی همراه ویندوز (اختیاری). فقط اسم فایل اجرایی ذخیره می‌شه، نه مسیر پوشه. برنامه‌ی همراه هنوز ساخته نشده، پس فعلاً چیزی با این قانون ردیابی نمی‌شه."))
        rules.forEach { r ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Text(r.executableName + " · " + modeName(r.trackingMode) + (r.idleMinutes?.let { " · " + tr("idle ", "بی‌کاری ") + num(it) } ?: ""), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                TextButton(onClick = { vm.deleteRule(r.id) }) { Text(tr("Remove", "حذف")) }
            }
        }
        OutlinedTextField(exe, { exe = it }, Modifier.fillMaxWidth(), singleLine = true, label = { Text(tr("Executable name", "نام فایل اجرایی")) }, placeholder = { Text("eldenring.exe") })
        ChipRow(TrackingMode.values().toList(), mode, ::modeName) { mode = it }
        OutlinedTextField(idle, { idle = it.filter(Char::isDigit).take(3) }, Modifier.fillMaxWidth(), singleLine = true, label = { Text(tr("Idle minutes (optional)", "دقیقه‌ی بی‌کاری (اختیاری)")) })
        Button(enabled = exe.isNotBlank(), onClick = { vm.saveRule(exe, mode, idle.toIntOrNull()); exe = ""; idle = "" }, Modifier.fillMaxWidth()) { Text(tr("Save rule", "ذخیره‌ی قانون")) }
    }
}
