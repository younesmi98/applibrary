@file:OptIn(ExperimentalMaterial3Api::class)

package com.nexus.app

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.nexus.core.database.*

<<<<<<< HEAD
private fun statusColor(s: GameStatus) = when (s) {
    GameStatus.PLAYING, GameStatus.REPLAYING -> Color(0xFF22C55E)
    GameStatus.COMPLETED, GameStatus.FULL_COMPLETION -> NexusColors.Accent
    GameStatus.BACKLOG, GameStatus.WANT_TO_PLAY -> Color(0xFFFBBF24)
    GameStatus.ABANDONED -> Color(0xFFFF6B6B)
    else -> NexusColors.Muted
}

private fun hoursText(minutes: Long?): String = when {
    minutes == null || minutes <= 0L -> "—"
    minutes < 60 -> digits("${minutes}m")
    else -> digits("${minutes / 60}h")
}
=======
private val PlayingGreen = Color(0xFF22C55E)

private fun statusColor(s: GameStatus) = when (s) {
    GameStatus.COMPLETED, GameStatus.FULL_COMPLETION -> NexusColors.Accent
    GameStatus.BACKLOG, GameStatus.WANT_TO_PLAY -> NexusColors.Warning
    GameStatus.ABANDONED -> NexusColors.Danger
    else -> NexusColors.Muted
}
>>>>>>> 7d946ca (NEXUS 0.3.0 redesign and release preparation)

@Composable
fun LibraryScreen(vm: LibraryViewModel, onOpen: (String) -> Unit, onAdd: () -> Unit, onImport: () -> Unit, onConnections: () -> Unit) {
    val paged = vm.games.collectAsLazyPagingItems()
    val filter by vm.filter.collectAsStateWithLifecycle()
    val sources by vm.sources.collectAsStateWithLifecycle()
    val stats by vm.stats.collectAsStateWithLifecycle()
    val playing by vm.playingIds.collectAsStateWithLifecycle()
    val collections by vm.collections.collectAsStateWithLifecycle()
<<<<<<< HEAD
    val playtime by vm.playtime.collectAsStateWithLifecycle()
    var grid by rememberSaveable { mutableStateOf(true) }
    var sheet by remember { mutableStateOf(false) }
    var sortMenu by remember { mutableStateOf(false) }
    val filtering = filter != LibraryFilter()
    LaunchedEffect(paged.itemCount) { vm.refreshPlaytime() }

    val segment = when {
        filter.favoritesOnly -> 3
        filter.status == null -> 0
        filter.status == GameStatus.PLAYING -> 1
        filter.status == GameStatus.COMPLETED -> 2
        else -> -1
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        OutlinedTextField(
            value = filter.query, onValueChange = vm::setQuery, modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            singleLine = true, shape = RoundedCornerShape(26.dp),
            colors = OutlinedTextFieldDefaults.colors(unfocusedContainerColor = Color(0x14FFFFFF), focusedContainerColor = Color(0x1AFFFFFF), unfocusedBorderColor = NexusColors.GlassBorder, focusedBorderColor = NexusColors.Accent),
=======
    var grid by rememberSaveable { mutableStateOf(AppPrefs.defaultGrid) }
    var sheet by remember { mutableStateOf(false) }
    var sortMenu by remember { mutableStateOf(false) }
    val filtering = filter.copy(sort = AppPrefs.defaultSort) != LibraryFilter(sort = AppPrefs.defaultSort)

    val segment = when {
        filter.favoritesOnly -> 4
        filter.recentOnly -> 1
        filter.status == GameStatus.BACKLOG -> 2
        filter.status == GameStatus.COMPLETED -> 3
        filter.status == null -> 0
        else -> -1
    }
    val segmentNames = listOf(tr("All", "همه"), tr("Recently played", "اخیراً بازی‌شده"), tr("Backlog", "در انتظار بازی"), tr("Completed", "تمام‌شده"), tr("Favorites", "علاقه‌مندی"))
    val sortIds = listOf(0, 3, 4, 1, 2)
    val sortNames = listOf(tr("Name", "نام"), tr("Playtime", "ساعت بازی"), tr("Last played", "آخرین اجرا"), tr("Recently added", "تازه‌ترین"), tr("Rating", "امتیاز"))

    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = filter.query, onValueChange = vm::setQuery, modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            singleLine = true, shape = RoundedCornerShape(26.dp),
            colors = OutlinedTextFieldDefaults.colors(unfocusedContainerColor = NexusColors.Glass, focusedContainerColor = NexusColors.Glass, unfocusedBorderColor = NexusColors.GlassBorder, focusedBorderColor = NexusColors.Accent),
>>>>>>> 7d946ca (NEXUS 0.3.0 redesign and release preparation)
            placeholder = { Text(tr("Search your library…", "جستجو در کتابخانه…")) },
            leadingIcon = { Icon(Icons.Default.Search, null) },
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    buildAnnotatedString {
                        if (I18n.fa) { append("کتابخانه "); withStyle(SpanStyle(color = NexusColors.Accent)) { append("نکسوس") } }
                        else { append("Nexus "); withStyle(SpanStyle(color = NexusColors.Accent)) { append("Library") } }
                    },
                    style = MaterialTheme.typography.headlineMedium,
                )
                Text(tr("Your game collection", "مجموعه‌ی بازی‌های تو"), style = MaterialTheme.typography.bodyMedium, color = NexusColors.TextSecondary)
            }
            AccentButton(tr("Add game", "افزودن بازی"), onAdd, icon = NexusIcons.Plus)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
<<<<<<< HEAD
            val names = listOf(tr("All", "همه"), tr("Playing", "در حال بازی"), tr("Done", "تمام‌شده"), tr("Favorites", "علاقه‌مندی"))
            SegmentedControl(4, segment, vm::setSegment, Modifier.weight(1f)) { i, on -> SegmentLabel(names[i], on) }
=======
            Box(Modifier.weight(1f)) { ChipRow(listOf(0, 1, 2, 3, 4), segment, { segmentNames[it] }, vm::setSegment) }
>>>>>>> 7d946ca (NEXUS 0.3.0 redesign and release preparation)
            SegmentedControl(2, if (grid) 1 else 0, { grid = it == 1 }, Modifier.width(92.dp)) { i, on ->
                Icon(if (i == 1) NexusIcons.Library else NexusIcons.List, if (i == 1) tr("Grid", "شبکه") else tr("List", "فهرست"), Modifier.size(18.dp), tint = if (on) NexusColors.OnAccent else NexusColors.TextSecondary)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PillButton(tr("Filters", "فیلترها") + if (filtering) " •" else "", NexusIcons.Filter, Modifier.weight(1f)) { sheet = true }
            Box(Modifier.weight(1f)) {
<<<<<<< HEAD
                PillButton(tr("Sort", "مرتب‌سازی"), NexusIcons.Sort, Modifier.fillMaxWidth()) { sortMenu = true }
                DropdownMenu(sortMenu, { sortMenu = false }) {
                    listOf(tr("Name", "نام"), tr("Recently added", "تازه‌ترین"), tr("Rating", "امتیاز")).forEachIndexed { i, n ->
                        DropdownMenuItem(text = { Text(n, color = if (filter.sort == i) NexusColors.Accent else NexusColors.TextPrimary) }, onClick = { vm.setSort(i); sortMenu = false })
=======
                PillButton(sortNames[sortIds.indexOf(filter.sort).coerceAtLeast(0)], NexusIcons.Sort, Modifier.fillMaxWidth()) { sortMenu = true }
                DropdownMenu(sortMenu, { sortMenu = false }) {
                    sortNames.forEachIndexed { i, n ->
                        DropdownMenuItem(text = { Text(n, color = if (filter.sort == sortIds[i]) NexusColors.Accent else NexusColors.TextPrimary) }, onClick = { vm.setSort(sortIds[i]); sortMenu = false })
>>>>>>> 7d946ca (NEXUS 0.3.0 redesign and release preparation)
                    }
                }
            }
        }

        val loading = paged.loadState.refresh is LoadState.Loading && paged.itemCount == 0
        val empty = paged.loadState.refresh is LoadState.NotLoading && paged.itemCount == 0
        when {
            loading -> if (grid) GridSkeleton() else ListSkeleton(6)
<<<<<<< HEAD
            empty -> if (filtering) EmptyState(tr("No matches", "موردی پیدا نشد"), tr("Try removing a filter.", "یکی از فیلترها رو بردار."))
            else EmptyState(tr("Your library is empty", "کتابخانه‌ات خالیه"), tr("Add a game by hand, or import a list from a file.", "یه بازی رو دستی اضافه کن یا فهرست بازی‌ها رو از فایل وارد کن.")) {
                OutlinedButton(onClick = onImport, Modifier.height(44.dp)) { Text(tr("Import from file", "ورود از فایل")) }
            }
            grid -> LazyVerticalGrid(
                columns = GridCells.Fixed(3), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 120.dp),
            ) {
                items(count = paged.itemCount, key = paged.itemKey { it.id }) { idx ->
                    val g = paged[idx] ?: return@items
                    GameCard(g, sources[g.id].orEmpty(), playtime[g.id], { onOpen(g.id) }, { vm.toggleFavorite(g) })
                }
            }
            else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp), contentPadding = PaddingValues(bottom = 120.dp)) {
                items(count = paged.itemCount, key = paged.itemKey { it.id }) { idx ->
                    val g = paged[idx] ?: return@items
                    Row(Modifier.fillMaxWidth().clickable { onOpen(g.id) }, horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Cover(g.coverUri, g.title, Modifier.size(width = 56.dp, height = 78.dp), showTitle = false)
                        Column(Modifier.weight(1f)) {
                            Text(g.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(listOfNotNull(g.status.label(), g.developer).joinToString(" · "), style = MaterialTheme.typography.bodySmall, color = NexusColors.TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
=======
            empty -> if (filtering) EmptyState(tr("No game matches", "بازی‌ای با این فیلتر پیدا نشد"), tr("Try removing a filter or searching for something else.", "یکی از فیلترها رو بردار یا چیز دیگه‌ای جستجو کن."))
            else EmptyState(tr("No games yet", "هنوز بازی‌ای نیست"), tr("Connect Steam, import a list, or add a game by hand.", "استیم رو وصل کن، فهرست وارد کن یا یه بازی رو دستی اضافه کن.")) {
                AccentButton(tr("Connect platforms", "اتصال پلتفرم‌ها"), onConnections)
                GlassButton(tr("Import from file", "ورود از فایل"), onImport)
            }
            else -> Crossfade(targetState = grid, animationSpec = tween(220), label = "view") { isGrid ->
                if (isGrid) LazyVerticalGrid(
                    columns = GridCells.Fixed(3), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 120.dp),
                ) {
                    items(count = paged.itemCount, key = paged.itemKey { it.id }) { idx ->
                        val g = paged[idx] ?: return@items
                        GameCard(g, sources[g.id].orEmpty(), stats[g.id], g.id in playing, { onOpen(g.id) }, { vm.toggleFavorite(g) })
                    }
                } else LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp), contentPadding = PaddingValues(bottom = 120.dp)) {
                    items(count = paged.itemCount, key = paged.itemKey { it.id }) { idx ->
                        val g = paged[idx] ?: return@items
                        Row(Modifier.fillMaxWidth().pressClick(true) { onOpen(g.id) }, horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Cover(g.coverUri, g.title, Modifier.size(width = 56.dp, height = 78.dp), showTitle = false)
                            Column(Modifier.weight(1f)) {
                                Text(g.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(
                                    (if (g.id in playing) tr("Playing now", "در حال بازی") else g.status.label()) + (g.developer?.let { " · $it" } ?: ""),
                                    style = MaterialTheme.typography.bodySmall, color = if (g.id in playing) PlayingGreen else NexusColors.TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis,
                                )
                            }
                            sources[g.id].orEmpty().take(2).forEach { PlatformBadge(it) }
                            Text(hoursLabel(stats[g.id]?.forever), style = MonoStyle)
>>>>>>> 7d946ca (NEXUS 0.3.0 redesign and release preparation)
                        }
                        sources[g.id].orEmpty().take(2).forEach { PlatformBadge(it) }
                        Text(hoursText(playtime[g.id]), style = MonoStyle)
                    }
                }
            }
        }
    }

    if (sheet) ModalBottomSheet(onDismissRequest = { sheet = false }, containerColor = NexusColors.SurfaceHigh) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
<<<<<<< HEAD
            Section(tr("Status", "وضعیت")) {
                ChipRow(listOf<GameStatus?>(null) + GameStatus.values().filter { it != GameStatus.HIDDEN }, filter.status, { it?.label() ?: tr("All", "همه") }, vm::setStatus)
            }
            Section(tr("Source", "منبع")) {
=======
            Section(tr("Platform", "پلتفرم")) {
>>>>>>> 7d946ca (NEXUS 0.3.0 redesign and release preparation)
                ChipRow(listOf<SourceType?>(null) + SourceType.values(), filter.source, { it?.label() ?: tr("All", "همه") }, vm::setSource)
            }
            Section(tr("Status", "وضعیت")) {
                ChipRow(listOf<GameStatus?>(null) + PersonalStatuses, filter.status, { it?.label() ?: tr("All", "همه") }, vm::setStatus)
            }
            if (collections.isNotEmpty()) Section(tr("Collection", "کلکسیون")) {
                ChipRow(listOf<CollectionEntity?>(null) + collections, collections.firstOrNull { it.id == filter.collectionId }, { it?.name ?: tr("All", "همه") }) { vm.setCollection(it?.id) }
            }
<<<<<<< HEAD
            TextButton(onClick = { vm.filter.value = LibraryFilter(); sheet = false }) { Text(tr("Clear all filters", "پاک‌کردن همه‌ی فیلترها")) }
=======
            TextButton(onClick = { vm.filter.value = LibraryFilter(sort = AppPrefs.defaultSort); sheet = false }) { Text(tr("Clear all filters", "پاک‌کردن همه‌ی فیلترها")) }
>>>>>>> 7d946ca (NEXUS 0.3.0 redesign and release preparation)
        }
    }
}

/** Statuses a person can set. "Playing" is not one of them: it only appears when a platform reports a running game. */
val PersonalStatuses = listOf(GameStatus.WANT_TO_PLAY, GameStatus.BACKLOG, GameStatus.COMPLETED, GameStatus.ABANDONED, GameStatus.ON_HOLD)

@Composable
private fun PillButton(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier, onClick: () -> Unit) {
<<<<<<< HEAD
    GlassSurface(modifier.height(44.dp).pressClick(true, onClick), RoundedCornerShape(22.dp), Color(0xFF232325)) {
        Row(Modifier.align(Alignment.Center), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, Modifier.size(18.dp), tint = NexusColors.TextPrimary)
            Text(text, style = MaterialTheme.typography.labelLarge)
=======
    GlassSurface(modifier.height(44.dp).pressClick(true, onClick), RoundedCornerShape(22.dp), NexusColors.SurfaceHigh) {
        Row(Modifier.align(Alignment.Center), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, Modifier.size(18.dp), tint = NexusColors.TextPrimary)
            Text(text, style = MaterialTheme.typography.labelLarge, maxLines = 1)
>>>>>>> 7d946ca (NEXUS 0.3.0 redesign and release preparation)
            Icon(NexusIcons.ChevronDown, null, Modifier.size(16.dp), tint = NexusColors.Muted)
        }
    }
}

<<<<<<< HEAD
/** Library card: artwork fills it, the platform mark sits top-start and a working heart top-end. No blur here, so scrolling stays cheap. */
@Composable
private fun GameCard(g: GameEntity, sources: List<SourceType>, minutes: Long?, onClick: () -> Unit, onFavorite: () -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    Box(Modifier.fillMaxWidth().aspectRatio(0.72f).clip(shape).border(1.dp, Brush.verticalGradient(listOf(Color(0x40FFFFFF), Color(0x0FFFFFFF))), shape).clickable(onClick = onClick)) {
=======
/** Library card. Artwork fills it; a soft gradient keeps the text readable. No blur on cards, so scrolling stays cheap. */
@Composable
private fun GameCard(g: GameEntity, sources: List<SourceType>, stats: GameStats?, playing: Boolean, onClick: () -> Unit, onFavorite: () -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    Box(Modifier.fillMaxWidth().aspectRatio(0.72f).clip(shape).border(1.dp, Brush.verticalGradient(listOf(Color(0x40FFFFFF), Color(0x0FFFFFFF))), shape).pressClick(true, onClick)) {
>>>>>>> 7d946ca (NEXUS 0.3.0 redesign and release preparation)
        Cover(g.coverUri, g.title, Modifier.fillMaxSize(), radius = 0, showTitle = false)
        Box(Modifier.fillMaxWidth().fillMaxHeight(0.6f).align(Alignment.BottomCenter).background(ScrimBrush))
        Row(Modifier.align(Alignment.TopStart).padding(8.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) { sources.take(2).forEach { PlatformBadge(it) } }
        Box(
            Modifier.align(Alignment.TopEnd).padding(6.dp).size(30.dp).clip(CircleShape).background(Color(0x66000000)).clickable(onClick = onFavorite),
            contentAlignment = Alignment.Center,
        ) {
            Icon(if (g.favorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder, if (g.favorite) tr("Remove from favorites", "حذف از موردعلاقه‌ها") else tr("Add to favorites", "افزودن به موردعلاقه‌ها"), Modifier.size(16.dp), tint = if (g.favorite) NexusColors.Accent else Color.White)
        }
        Column(Modifier.align(Alignment.BottomStart).padding(8.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(g.title, style = MaterialTheme.typography.labelLarge, color = Color.White, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 17.sp)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
<<<<<<< HEAD
                Box(Modifier.size(6.dp).clip(CircleShape).background(statusColor(g.status)))
                Text(g.status.label(), style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.85f), maxLines = 1, modifier = Modifier.weight(1f, fill = false), fontSize = 10.sp)
                Spacer(Modifier.weight(1f))
                Icon(NexusIcons.Clock, null, Modifier.size(11.dp), tint = Color.White.copy(alpha = 0.8f))
                Text(hoursText(minutes), style = MonoStyle.copy(color = Color.White.copy(alpha = 0.9f), fontSize = 10.sp))
=======
                Box(Modifier.size(6.dp).clip(CircleShape).background(if (playing) PlayingGreen else statusColor(g.status)))
                Text(if (playing) tr("Playing now", "در حال بازی") else g.status.label(), style = MaterialTheme.typography.bodySmall, color = if (playing) PlayingGreen else Color.White.copy(alpha = 0.85f), maxLines = 1, modifier = Modifier.weight(1f, fill = false), fontSize = 10.sp)
                Spacer(Modifier.weight(1f))
                Icon(NexusIcons.Clock, null, Modifier.size(11.dp), tint = Color.White.copy(alpha = 0.8f))
                Text(hoursLabel(stats?.forever), style = MonoStyle.copy(color = Color.White.copy(alpha = 0.9f), fontSize = 10.sp))
>>>>>>> 7d946ca (NEXUS 0.3.0 redesign and release preparation)
            }
        }
    }
}
