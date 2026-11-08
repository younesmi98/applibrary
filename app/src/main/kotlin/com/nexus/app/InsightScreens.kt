@file:OptIn(ExperimentalMaterial3Api::class)

package com.nexus.app

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nexus.insights.*
<<<<<<< HEAD
import java.time.LocalDate
import java.time.ZoneId

@Composable
fun MoreScreen(go: (Screen) -> Unit) {
    fun r(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, target: Screen) = GroupRow(label, icon, onClick = { go(target) })
    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp).verticalScroll(androidx.compose.foundation.rememberScrollState()), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        PageTitle(tr("More", "بیشتر"))
        GroupedRows(listOf(
            r(tr("Collections", "کلکسیون‌ها"), Icons.Default.Star, Screen.Collections),
            r(tr("Play next", "بعدی چی بازی کنم"), Icons.Default.PlayArrow, Screen.PlayNext),
            r(tr("Backlog assistant", "دستیار صف بازی"), Icons.Default.Done, Screen.Backlog),
            r(tr("Wrapped", "مرور سال"), Icons.Default.DateRange, Screen.Wrapped),
        ))
        GroupedRows(listOf(r(tr("Wishlist", "فهرست آرزوها"), Icons.Default.Favorite, Screen.Wishlist), r(tr("Journal", "ژورنال"), Icons.Default.Edit, Screen.Journal)))
        GroupedRows(listOf(
            r(tr("Import from file", "ورود از فایل"), Icons.Default.Add, Screen.Import),
            r(tr("Backup and export", "پشتیبان‌گیری و خروجی"), Icons.Default.Refresh, Screen.Backup),
            r(tr("PC devices", "دستگاه‌های PC"), Icons.Default.Info, Screen.Devices),
        ))
        GroupedRows(listOf(r(tr("Settings and about", "تنظیمات و درباره"), Icons.Default.Settings, Screen.Settings)))
        Spacer(Modifier.height(120.dp))
    }
}
=======
>>>>>>> 7d946ca (NEXUS 0.3.0 redesign and release preparation)

/** Statistics built only from what platforms reported. There is no manual time and no invented history. */
@Composable
fun StatsScreen(app: NexusApp) {
    val snap by produceState<InsightsSnapshot?>(null) { value = InsightsLoader.load(app.db) }
    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp).verticalScroll(androidx.compose.foundation.rememberScrollState()), verticalArrangement = Arrangement.spacedBy(28.dp)) {
        PageTitle(tr("Statistics", "آمار"))
        val s = snap
        if (s == null) { StatsSkeleton(); return@Column }
<<<<<<< HEAD
        if (s.games.isEmpty()) { EmptyState(tr("No data yet", "هنوز داده‌ای نیست"), tr("Stats appear once you add games and track time.", "با اضافه‌کردن بازی و ثبت زمان، آمار اینجا پر می‌شه.")); return@Column }
        val st = Stats.library(s.games)
        val zone = ZoneId.systemDefault()
        val monthly = Stats.monthly(s.sessions, zone)
=======
        if (s.cards.isEmpty()) { EmptyState(tr("No data yet", "هنوز داده‌ای نیست"), tr("Statistics appear once games are in your library.", "وقتی بازی‌ها وارد کتابخانه بشن، آمار اینجا پر می‌شه.")); return@Column }
        val cards = s.cards
        val known = cards.filter { it.stats.forever != null }
>>>>>>> 7d946ca (NEXUS 0.3.0 redesign and release preparation)

        Section(tr("Library", "کتابخانه")) {
            LabeledValue(tr("Games", "بازی‌ها"), num(cards.size))
            LabeledValue(tr("Favorites", "موردعلاقه‌ها"), num(cards.count { it.game.favorite }))
            LabeledValue(
                tr("Total playtime", "مجموع ساعت بازی"), if (known.isEmpty()) "—" else hoursLabel(known.sumOf { it.stats.forever!!.toLong() }.toInt()),
                if (known.isEmpty()) tr("No platform has reported playtime yet", "هنوز هیچ پلتفرمی ساعت بازی گزارش نکرده") else tr("As reported by connected platforms, for ${known.size} games", "طبق گزارش پلتفرم‌های متصل، برای ${num(known.size)} بازی"),
            )
        }
        val perPlatform = cards.flatMap { c -> c.sources.map { it.type }.distinct() }.groupingBy { it }.eachCount()
        if (perPlatform.isNotEmpty()) Section(tr("Games per platform", "بازی‌ها به تفکیک پلتفرم")) {
            val max = perPlatform.values.max().toFloat()
            perPlatform.entries.sortedByDescending { it.value }.forEach { BarRow(it.key.label(), it.value.toFloat(), max, num(it.value)) }
        }
        val minutesPerPlatform = cards.flatMap { it.sources }.filter { it.playtimeForeverMin != null }.groupBy { it.type }.mapValues { e -> e.value.sumOf { it.playtimeForeverMin!!.toLong() } }
        if (minutesPerPlatform.isNotEmpty()) Section(tr("Playtime per platform", "ساعت بازی به تفکیک پلتفرم")) {
            val max = minutesPerPlatform.values.max().toFloat()
            minutesPerPlatform.entries.sortedByDescending { it.value }.forEach { BarRow(it.key.label(), it.value.toFloat(), max, hoursLabel(it.value.toInt())) }
        }
        val top = known.filter { it.stats.forever!! > 0 }.sortedByDescending { it.stats.forever }.take(8)
        if (top.isNotEmpty()) Section(tr("Most played", "بیشترین ساعت بازی")) {
            val max = top.first().stats.forever!!.toFloat()
            top.forEach { BarRow(it.game.title, it.stats.forever!!.toFloat(), max, hoursLabel(it.stats.forever)) }
        }
        val recent2 = cards.filter { (it.stats.twoWeeks ?: 0) > 0 }.sortedByDescending { it.stats.twoWeeks }.take(5)
        if (recent2.isNotEmpty()) Section(tr("Last two weeks", "دو هفته‌ی اخیر")) {
            val max = recent2.first().stats.twoWeeks!!.toFloat()
            recent2.forEach { BarRow(it.game.title, it.stats.twoWeeks!!.toFloat(), max, hoursLabel(it.stats.twoWeeks)) }
        }
<<<<<<< HEAD
        Hint(tr("Genre and achievement charts need data that is not stored yet, so they are not shown.", "نمودار ژانر و اچیومنت به داده‌ای نیاز داره که هنوز ذخیره نمی‌شه، برای همین نشون داده نمی‌شه."))
        Spacer(Modifier.height(120.dp))
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
        if (s == null) { Loading(); return@SubScreen }
        val w = WrappedBuilder.build(year, s.games, s.sessions, ZoneId.systemDefault())
        if (w == null) {
            EmptyState(tr("Not enough data for ${num(year)}", "برای ${num(year)} داده‌ی کافی نیست"), tr("Wrapped only uses sessions NEXUS recorded in that year. Store hours have no dates, so they are not counted.", "مرور سال فقط از جلسه‌هایی استفاده می‌کنه که NEXUS تو اون سال ثبت کرده. ساعت‌های استور تاریخ ندارن و حساب نمی‌شن."))
            return@SubScreen
=======
        val lastPlayed = cards.filter { it.stats.lastPlayed != null }.sortedByDescending { it.stats.lastPlayed }.take(5)
        if (lastPlayed.isNotEmpty()) Section(tr("Recently played", "اخیراً بازی‌شده")) {
            lastPlayed.forEach { LabeledValue(it.game.title, relativeDay(it.stats.lastPlayed)) }
>>>>>>> 7d946ca (NEXUS 0.3.0 redesign and release preparation)
        }
        Section(tr("By status", "بر اساس وضعیت")) {
            val byStatus = cards.groupingBy { it.game.status }.eachCount()
            val max = byStatus.values.max().toFloat()
            byStatus.entries.sortedByDescending { it.value }.forEach { BarRow(it.key.label(), it.value.toFloat(), max, num(it.value)) }
        }
        Hint(tr("Steam only reports totals, so there is no day-by-day chart. Each sync is saved, and a per-game history appears on the game page as syncs add up.", "استیم فقط مجموع رو گزارش می‌ده، برای همین نمودار روزانه نیست. هر همگام‌سازی ذخیره می‌شه و تاریخچه‌ی هر بازی تو صفحه‌ی خودش با زیاد شدن همگام‌سازی‌ها ساخته می‌شه."))
        Spacer(Modifier.height(120.dp))
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
        if (s == null) { Loading(); return@SubScreen }
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
        if (s == null) { Loading(); return@SubScreen }
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
