@file:OptIn(ExperimentalMaterial3Api::class)

package com.nexus.app

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nexus.core.database.SourceType

/** One card per platform, each with its own state. Steam works with your own Web API key; Epic is explained honestly. */
@Composable
fun ConnectionsScreen(app: NexusApp, onBack: () -> Unit, onImport: () -> Unit) {
    val ctx = LocalContext.current
    val vm: ConnectionsViewModel = viewModel(factory = remember { NexusVmFactory(app) })
    val acc by vm.account.collectAsStateWithLifecycle()
    val syncing by vm.syncing.collectAsStateWithLifecycle()
    val outcome by vm.outcome.collectAsStateWithLifecycle()
    val state = PlatformSync.state(acc, syncing)
    var idInput by remember { mutableStateOf("") }
    var keyInput by remember { mutableStateOf("") }
    var confirmDisconnect by remember { mutableStateOf(false) }

    SubScreen(tr("Connected platforms", "پلتفرم‌های متصل"), onBack) {
        Section("Steam") {
            Panel {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (acc?.accountId != null) Avatar(acc?.avatarUrl, acc?.displayName ?: "S", 44) else PlatformBadge(SourceType.STEAM)
                    Column(Modifier.weight(1f)) {
                        Text(acc?.displayName ?: "Steam", style = MaterialTheme.typography.titleMedium)
                        Text(
                            when (state) {
                                ConnectionState.NOT_CONNECTED -> tr("Not connected", "متصل نیست")
                                ConnectionState.CONNECTED -> tr("Connected", "متصل")
                                ConnectionState.SYNCING -> tr("Syncing…", "در حال همگام‌سازی…")
                                ConnectionState.FAILED -> tr("Last sync failed", "همگام‌سازی ناموفق بود")
                                ConnectionState.NEEDS_ACCESS -> tr("Needs access", "نیازمند بررسی دسترسی")
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = when (state) { ConnectionState.CONNECTED -> NexusColors.Accent; ConnectionState.FAILED, ConnectionState.NEEDS_ACCESS -> NexusColors.Danger; else -> NexusColors.TextSecondary },
                        )
                    }
                }
                if (acc?.accountId != null) {
                    LabeledValue(tr("Last successful sync", "آخرین همگام‌سازی موفق"), acc?.lastSyncAt?.let { formatDate(it) } ?: "—")
                    acc?.lastError?.let { Text(errorText(it) + ". " + tr("Your saved library is still shown.", "کتابخانه‌ی ذخیره‌شده‌ات هنوز نمایش داده می‌شه."), style = MaterialTheme.typography.bodySmall, color = NexusColors.Danger) }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AccentButton(if (syncing) tr("Syncing…", "در حال همگام‌سازی…") else tr("Sync now", "همگام‌سازی"), { if (!syncing) vm.sync() }, Modifier.weight(1f))
                        GlassButton(tr("Disconnect", "قطع اتصال"), { confirmDisconnect = true }, Modifier.weight(1f))
                    }
                } else {
                    Text(tr("NEXUS reads your library with your own Steam Web API key. The key stays encrypted on this phone, is never put in the app, and your Steam password is never asked for.", "NEXUS کتابخانه‌ات رو با کلید Steam Web API خودت می‌خونه. کلید رمزنگاری‌شده روی همین گوشی می‌مونه، داخل اپ قرار نمی‌گیره و رمز استیمت هیچ‌وقت خواسته نمی‌شه."), style = MaterialTheme.typography.bodySmall, color = NexusColors.TextSecondary)
                    OutlinedTextField(idInput, { idInput = it }, Modifier.fillMaxWidth(), singleLine = true, label = { Text(tr("SteamID64, profile link or profile name", "SteamID64، لینک یا نام پروفایل")) })
                    OutlinedTextField(keyInput, { keyInput = it }, Modifier.fillMaxWidth(), singleLine = true, label = { Text(tr("Steam Web API key", "کلید Steam Web API")) }, visualTransformation = PasswordVisualTransformation())
                    TextButton(onClick = { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://steamcommunity.com/dev/apikey")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }) { Text(tr("Get a key from Steam", "گرفتن کلید از استیم")) }
                    AccentButton(if (syncing) tr("Connecting…", "در حال اتصال…") else tr("Connect and sync", "اتصال و همگام‌سازی"), { if (!syncing && idInput.isNotBlank() && keyInput.isNotBlank()) vm.connect(idInput, keyInput) }, Modifier.fillMaxWidth())
                }
                outcome?.let { Text(outcomeText(it), style = MaterialTheme.typography.bodyMedium, color = if (it is SyncOutcome.Success) NexusColors.Accent else NexusColors.Danger) }
                Hint(tr("Steam only returns your games and hours if Game details are public in your Steam privacy settings.", "استیم فقط وقتی بازی‌ها و ساعت‌ها رو می‌ده که «جزئیات بازی» تو تنظیمات حریم خصوصی عمومی باشه."))
            }
        }
        Section("Epic Games") {
            Panel {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    PlatformBadge(SourceType.EPIC)
                    Column(Modifier.weight(1f)) {
                        Text("Epic Games Store", style = MaterialTheme.typography.titleMedium)
                        Text(tr("Not available", "در دسترس نیست"), style = MaterialTheme.typography.bodySmall, color = NexusColors.TextSecondary)
                    }
                }
                Text(tr("Epic offers no official way for an app to read your library or playtime. Unofficial logins put your account at risk, so NEXUS does not use them. The code is organised so a platform can be added later without rewriting the app.", "اپیک راه رسمی برای خواندن کتابخانه یا ساعت بازی توسط اپ نداره. ورود غیررسمی حسابت رو به خطر می‌ندازه و NEXUS ازش استفاده نمی‌کنه. کد طوری سازمان‌دهی شده که بشه بعداً یه پلتفرم رو بدون بازنویسی اپ اضافه کرد."), style = MaterialTheme.typography.bodySmall, color = NexusColors.TextSecondary)
                GlassButton(tr("Import Epic games from a file", "ورود بازی‌های اپیک از فایل"), onImport, Modifier.fillMaxWidth())
            }
        }
    }
    if (confirmDisconnect) AlertDialog(
        onDismissRequest = { confirmDisconnect = false },
        title = { Text(tr("Disconnect Steam?", "اتصال استیم قطع بشه؟")) },
        text = { Text(tr("The saved key is removed. Your games, notes, collections and favorites stay in NEXUS.", "کلید ذخیره‌شده حذف می‌شه. بازی‌ها، یادداشت‌ها، کلکسیون‌ها و موردعلاقه‌هات تو NEXUS می‌مونن.")) },
        confirmButton = { TextButton(onClick = { confirmDisconnect = false; vm.disconnect() }) { Text(tr("Disconnect", "قطع اتصال"), color = NexusColors.Danger) } },
        dismissButton = { TextButton(onClick = { confirmDisconnect = false }) { Text(tr("Cancel", "انصراف")) } },
    )
}
