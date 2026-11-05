@file:OptIn(ExperimentalMaterial3Api::class)

package com.nexus.app

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.nexus.core.database.GameStatus
import com.nexus.core.database.NewGame
import com.nexus.core.database.SourceType
import kotlinx.coroutines.launch

@Composable
fun AddGameScreen(vm: AddGameViewModel, onBack: () -> Unit, onSaved: (String) -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var title by remember { mutableStateOf("") }
    var original by remember { mutableStateOf("") }
    var developer by remember { mutableStateOf("") }
    var year by remember { mutableStateOf("") }
    var source by remember { mutableStateOf(SourceType.MANUAL) }
    var status by remember { mutableStateOf(GameStatus.BACKLOG) }
    var tags by remember { mutableStateOf("") }
    var exe by remember { mutableStateOf("") }
    var cover by remember { mutableStateOf<Uri?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching { ctx.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            cover = uri
        }
    }

    SubScreen(tr("Add a game", "افزودن بازی"), onBack) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Cover(cover?.toString(), title.ifBlank { "?" }, Modifier.size(width = 96.dp, height = 132.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { picker.launch(arrayOf("image/*")) }) { Text(tr("Choose cover", "انتخاب کاور")) }
                if (cover != null) TextButton(onClick = { cover = null }) { Text(tr("Remove cover", "حذف کاور")) }
            }
        }
        Section(tr("About the game", "درباره‌ی بازی")) {
            OutlinedTextField(title, { title = it; error = null }, Modifier.fillMaxWidth(), label = { Text(tr("Title *", "عنوان *")) }, singleLine = true, isError = error != null, supportingText = { error?.let { Text(it) } })
            OutlinedTextField(original, { original = it }, Modifier.fillMaxWidth(), label = { Text(tr("Original title", "عنوان اصلی")) }, singleLine = true)
            OutlinedTextField(developer, { developer = it }, Modifier.fillMaxWidth(), label = { Text(tr("Developer", "سازنده")) }, singleLine = true)
            OutlinedTextField(year, { year = it.filter(Char::isDigit).take(4) }, Modifier.fillMaxWidth(), label = { Text(tr("Release year", "سال انتشار")) }, singleLine = true)
        }
        Section(tr("Where it comes from", "از کجا اومده")) {
            ChipRow(SourceType.values().toList(), source, { it.label() }) { source = it }
            Hint(tr("This does not mean the game is store-owned. It only records where you play it.", "این به معنی خریداری‌شده از استور نیست؛ فقط می‌گه کجا بازیش می‌کنی."))
        }
        Section(tr("Your progress", "پیشرفت تو")) {
            ChipRow(GameStatus.values().filter { it != GameStatus.HIDDEN }, status, { it.label() }) { status = it }
            OutlinedTextField(tags, { tags = it }, Modifier.fillMaxWidth(), label = { Text(tr("Tags, comma separated", "برچسب‌ها، با کاما جدا کن")) }, singleLine = true)
        }
        Section(tr("Installation (optional)", "نصب (اختیاری)")) {
            OutlinedTextField(exe, { exe = it }, Modifier.fillMaxWidth(), label = { Text(tr("Executable name", "نام فایل اجرایی")) }, singleLine = true, placeholder = { Text("eldenring.exe") })
        }
        Button(
            modifier = Modifier.fillMaxWidth().height(52.dp),
            onClick = {
                if (title.isBlank()) { error = tr("Title is required", "عنوان لازمه"); return@Button }
                scope.launch {
                    val id = vm.save(
                        NewGame(
                            title = title, source = source, originalTitle = original.ifBlank { null }, developer = developer.ifBlank { null },
                            releaseYear = year.toIntOrNull(), status = status, coverUri = cover?.toString(), executableName = exe.ifBlank { null },
                            tags = tags.split(',', '،').map { it.trim() }.filter { it.isNotEmpty() },
                        )
                    )
                    onSaved(id)
                }
            },
        ) { Text(tr("Save", "ذخیره")) }
    }
}
