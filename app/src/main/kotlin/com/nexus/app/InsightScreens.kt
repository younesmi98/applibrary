@file:OptIn(ExperimentalMaterial3Api::class)

package com.nexus.app

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.nexus.insights.*
import java.time.LocalDate
import java.time.ZoneId

@Composable
fun MoreScreen(go: (Screen) -> Unit) {
    val items = listOf(
        tr("Collections", "کلکسیون‌ها") to Screen.Collections,
        tr("Play next", "بعدی چی بازی کنم") to Screen.PlayNext,
        tr("Backlog assistant", "دستیار صف بازی") to Screen.Backlog,
        tr("Wrapped", "مرور سال") to Screen.Wrapped,
        tr("Wishlist", "فهرست آرزوها") to Screen.Wishlist,
        tr("Journal", "ژورنال") to Screen.Journal,
        tr("Import from file", "ورود از فایل") to Screen.Import,
        tr("Backup and export", "پشتیبان‌گیری و خروجی") to Screen.Backup,
        tr("PC devices", "دستگاه‌های PC") to Screen.Devices,
        tr("Settings and about", "تنظیمات و درباره") to Screen.Settings,
    )
    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        PageTitle(tr("More", "بیشتر"))
        Spacer(Modifier.height(8.dp))
        items.forEach { (name, target) ->
            Row(Modifier.fillMaxWidth().heightIn(min = 56.dp).clickable { go(target) }, verticalAlignment = Alignment.CenterVertically) {
                Text(name, style = MaterialTheme.typography.bodyLarge)
            }
            HorizontalDivider(color = NexusColors.Border)
        }
    }
}

@Composable
fun StatsScreen(app: NexusApp) {
    val snap by produceState<InsightsSnapshot?>(null) { value = InsightsLoader.load(app.db) }
    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp).verticalScroll(androidx.compose.foundation.rememberScrollState()), verticalArrangement = Arrangement.spacedBy(28.dp)) {
        PageTitle(tr("Stats", "آمار"))
        val s = snap
        if (s == null) { Hint(tr("Loading…", "در حال بارگذاری…")); return@Column }
        if (s.games.isEmpty()) { EmptyState(tr("No data yet", "هنوز داده‌ای نیست"), tr("Stats appear once you add games and track time.", "با اضافه‌کردن بازی و ثبت زمان، آمار اینجا پر می‌شه.")); return@Column }
        val st = Stats.library(s.games)
        val zone = ZoneId.systemDefault()
        val monthly = Stats.monthly(s.sessions, zone)

        Section(tr("Library", "کتابخانه")) {
            LabeledValue(tr("Games", "بازی‌ها"), num(st.totalGames))
            LabeledValue(tr("Completed", "تمام‌شده"), num(st.completed))
            LabeledValue(tr("Currently playing", "در حال بازی"), num(st.currentlyPlaying))
            LabeledValue(tr("Never played", "هیچ‌وقت بازی‌نشده"), num(st.unplayed))
            LabeledValue(tr("Favorites", "موردعلاقه‌ها"), num(st.favorites))
        }
        Section(tr("Playtime, two separate numbers", "زمان بازی، دو عدد جدا")) {
            LabeledValue(tr("Store reported", "گزارش استور"), formatDuration(st.storeMinutesTotal * 60_000L), tr("Only games where a store gave a number", "فقط بازی‌هایی که استور عدد داده"))
            LabeledValue(tr("Recorded by NEXUS", "ثبت‌شده توسط NEXUS"), formatDuration(st.trackedMinutesTotal * 60_000L), tr("PC and phone sessions, without manual adjustments", "جلسه‌های PC و گوشی، بدون تعدیل دستی"))
        }
        if (st.gamesPerSource.isNotEmpty()) Section(tr("Games per source", "بازی‌ها به تفکیک منبع")) {
            val max = st.gamesPerSource.values.max().toFloat()
            st.gamesPerSource.entries.sortedByDescending { it.value }.forEach { BarRow(it.key, it.value.toFloat(), max, num(it.value)) }
        }
        if (st.mostTracked.isNotEmpty()) Section(tr("Most tracked games", "بیشترین زمان ثبت‌شده")) {
            val max = st.mostTracked.first().second.toFloat()
            st.mostTracked.forEach { (t, m) -> BarRow(t, m.toFloat(), max, formatDuration(m * 60_000L)) }
        }
        Section(tr("Monthly activity", "فعالیت ماهانه")) {
            val start = monthly.coverageStart
            if (start == null) Hint(tr("No sessions recorded yet. This fills in as you track time.", "هنوز جلسه‌ای ثبت نشده. با ثبت زمان این بخش پر می‌شه."))
            else {
                Hint(tr("History starts ", "تاریخچه از ") + formatDay(start.atZone(zone).toLocalDate()) + tr(". Nothing before that was recorded.", " شروع می‌شه. قبل از اون چیزی ثبت نشده."))
                val rows = monthly.minutesPerMonth.entries.toList().takeLast(12)
                val max = rows.maxOf { it.value }.toFloat()
                rows.forEach { BarRow(digits(it.key.toString()), it.value.toFloat(), max, formatDuration(it.value * 60_000L)) }
            }
        }
        Hint(tr("Genre and achievement charts need data that is not stored yet, so they are not shown.", "نمودار ژانر و اچیومنت به داده‌ای نیاز داره که هنوز ذخیره نمی‌شه، برای همین نشون داده نمی‌شه."))
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
fun WrappedScreen(app: NexusApp, onBack: () -> Unit) {
    val ctx = LocalContext.current
    val snap by produceState<InsightsSnapshot?>(null) { value = InsightsLoader.load(app.db) }
    val thisYear = LocalDate.now().year
    var year by remember { mutableStateOf(thisYear) }
    SubScreen(tr("Wrapped", "مرور سال"), onBack) {
        ChipRow((thisYear downTo thisYear - 3).toList(), year, { num(it) }) { year = it }
        val s = snap
        if (s == null) { Hint(tr("Loading…", "در حال بارگذاری…")); return@SubScreen }
        val w = WrappedBuilder.build(year, s.games, s.sessions, ZoneId.systemDefault())
        if (w == null) {
            EmptyState(tr("Not enough data for ${num(year)}", "برای ${num(year)} داده‌ی کافی نیست"), tr("Wrapped only uses sessions NEXUS recorded in that year. Store hours have no dates, so they are not counted.", "مرور سال فقط از جلسه‌هایی استفاده می‌کنه که NEXUS تو اون سال ثبت کرده. ساعت‌های استور تاریخ ندارن و حساب نمی‌شن."))
            return@SubScreen
        }
        Panel {
            Text("NEXUS ${num(year)}", style = MaterialTheme.typography.titleLarge, color = NexusColors.Accent)
            WrappedImage.lines(w).forEach { (l, v) -> LabeledValue(l, v) }
        }
        Button(onClick = { WrappedImage.share(ctx, w) }, Modifier.fillMaxWidth().height(52.dp)) { Text(tr("Share as image", "اشتراک‌گذاری به‌صورت تصویر")) }
        Hint(tr("Completed-game counts are left out: completion dates are not recorded yet.", "تعداد بازی‌های تمام‌شده نمایش داده نمی‌شه چون تاریخ تکمیل هنوز ثبت نمی‌شه."))
    }
}

@Composable
fun PlayNextScreen(app: NexusApp, onBack: () -> Unit, onOpen: (String) -> Unit) {
    val snap by produceState<InsightsSnapshot?>(null) { value = InsightsLoader.load(app.db) }
    var mode by remember { mutableStateOf(PlayNextMode.RANDOM) }
    var seed by remember { mutableStateOf(0) }
    fun name(m: PlayNextMode) = when (m) {
        PlayNextMode.RANDOM -> tr("Random", "تصادفی")
        PlayNextMode.SHORT -> tr("Short", "کوتاه")
        PlayNextMode.LEAST_PLAYED -> tr("Least played", "کم‌بازی‌شده")
        PlayNextMode.UNPLAYED -> tr("Unplayed", "بازی‌نشده")
        PlayNextMode.CLOSE_TO_COMPLETION -> tr("Close to done", "نزدیک به پایان")
        PlayNextMode.RECENTLY_ADDED -> tr("Recently added", "تازه‌اضافه‌شده")
        PlayNextMode.HIGHEST_RATED -> tr("Highest rated", "بالاترین امتیاز")
    }
    SubScreen(tr("Play next", "بعدی چی بازی کنم"), onBack) {
        ChipRow(PlayNextMode.values().toList(), mode, ::name) { mode = it }
        val s = snap
        if (s == null) { Hint(tr("Loading…", "در حال بارگذاری…")); return@SubScreen }
        val picks = remember(s, mode, seed) { PlayNext.suggest(s.games, mode, 5, kotlin.random.Random(seed)) }
        if (mode == PlayNextMode.RANDOM || mode == PlayNextMode.UNPLAYED) OutlinedButton(onClick = { seed++ }) { Text(tr("Shuffle", "تصادفی دوباره")) }
        if (picks.isEmpty()) Hint(
            when (mode) {
                PlayNextMode.SHORT -> tr("No game has a known length yet, so nothing is guessed.", "طول هیچ بازی‌ای معلوم نیست، برای همین چیزی حدس زده نمی‌شه.")
                PlayNextMode.CLOSE_TO_COMPLETION -> tr("No game has achievement progress stored yet.", "هنوز پیشرفت اچیومنت هیچ بازی‌ای ذخیره نشده.")
                PlayNextMode.HIGHEST_RATED -> tr("Rate some games first.", "اول به چند بازی امتیاز بده.")
                else -> tr("Nothing matches in your library.", "چیزی تو کتابخانه‌ات با این حالت جور نیست.")
            },
        )
        picks.forEach { g ->
            Column(Modifier.fillMaxWidth().clickable { onOpen(g.id) }.padding(vertical = 4.dp)) {
                Text(g.title, style = MaterialTheme.typography.titleMedium)
                Hint(listOf(g.status.label(), g.sources.joinToString(" / ")).filter { it.isNotBlank() }.joinToString(" · "))
            }
        }
    }
}

@Composable
fun BacklogScreen(app: NexusApp, onBack: () -> Unit, onOpen: (String) -> Unit) {
    val snap by produceState<InsightsSnapshot?>(null) { value = InsightsLoader.load(app.db) }
    SubScreen(tr("Backlog assistant", "دستیار صف بازی"), onBack) {
        val s = snap
        if (s == null) { Hint(tr("Loading…", "در حال بارگذاری…")); return@SubScreen }
        val list = BacklogAssistant.suggest(s.games, System.currentTimeMillis())
        Hint(tr("Suggestions only. Nothing changes until you open a game and change its status.", "فقط پیشنهاده. تا خودت وضعیت یه بازی رو عوض نکنی چیزی تغییر نمی‌کنه."))
        if (list.isEmpty()) EmptyState(tr("No suggestions right now", "فعلاً پیشنهادی نیست"), tr("Your backlog looks tidy.", "صف بازی‌هات مرتبه."))
        list.forEach { b ->
            Column(Modifier.fillMaxWidth().clickable { onOpen(b.gameId) }, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(s.titles[b.gameId] ?: tr("Game", "بازی"), style = MaterialTheme.typography.titleMedium)
                val days = (System.currentTimeMillis() - (s.games.firstOrNull { it.id == b.gameId }?.let { g ->
                    when (b.type) { SuggestionType.START_OR_REMOVE -> g.addedAt; else -> g.lastPlayedAt ?: g.addedAt }
                } ?: 0L)) / 86_400_000L
                Hint(when (b.type) {
                    SuggestionType.MOVE_TO_ON_HOLD -> tr("Marked playing, last played ${num(days.toInt())} days ago", "وضعیتش «در حال بازی»ه ولی ${num(days.toInt())} روز پیش آخرین بار بازی شده")
                    SuggestionType.MOVE_TO_ABANDONED_REVIEW -> tr("On hold for ${num(days.toInt())} days", "${num(days.toInt())} روزه متوقفه")
                    SuggestionType.START_OR_REMOVE -> tr("Never played, added ${num(days.toInt())} days ago", "هیچ‌وقت بازی نشده، ${num(days.toInt())} روز پیش اضافه شده")
                })
                Text(when (b.type) {
                    SuggestionType.MOVE_TO_ON_HOLD -> tr("Consider: On hold", "پیشنهاد: متوقف")
                    SuggestionType.MOVE_TO_ABANDONED_REVIEW -> tr("Consider: Abandoned", "پیشنهاد: رهاشده")
                    SuggestionType.START_OR_REMOVE -> tr("Consider: start it or remove it", "پیشنهاد: شروعش کن یا حذفش کن")
                }, style = MaterialTheme.typography.labelMedium, color = NexusColors.Accent)
            }
        }
    }
}
