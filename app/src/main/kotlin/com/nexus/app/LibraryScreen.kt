@file:OptIn(ExperimentalMaterial3Api::class)

package com.nexus.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.nexus.core.database.CollectionEntity
import com.nexus.core.database.GameEntity
import com.nexus.core.database.GameStatus
import com.nexus.core.database.LibraryFilter
import com.nexus.core.database.SourceType

@Composable
fun LibraryScreen(vm: LibraryViewModel, onOpen: (String) -> Unit, onAdd: () -> Unit, onImport: () -> Unit) {
    val paged = vm.games.collectAsLazyPagingItems()
    val filter by vm.filter.collectAsStateWithLifecycle()
    val sources by vm.sources.collectAsStateWithLifecycle()
    val collections by vm.collections.collectAsStateWithLifecycle()
    var grid by rememberSaveable { mutableStateOf(true) }
    var sheet by remember { mutableStateOf(false) }
    val filtering = filter != LibraryFilter()

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            PageTitle(tr("Library", "کتابخانه")) {
                TextButton(onClick = { grid = !grid }) { Text(if (grid) tr("List", "فهرست") else tr("Grid", "شبکه")) }
            }
            OutlinedTextField(
                value = filter.query, onValueChange = vm::setQuery,
                modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(28.dp),
                placeholder = { Text(tr("Search games, developers, notes", "جستجو در بازی‌ها، سازنده‌ها و یادداشت‌ها")) },
                leadingIcon = { Icon(Icons.Default.Search, null) },
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f)) {
                    ChipRow(
                        options = listOf<GameStatus?>(null) + GameStatus.values().filter { it != GameStatus.HIDDEN },
                        selected = filter.status, label = { it?.label() ?: tr("All", "همه") }, onPick = vm::setStatus,
                    )
                }
                TextButton(onClick = { sheet = true }) { Text(tr("Filters", "فیلترها") + if (filtering) " •" else "") }
            }

            val loaded = paged.loadState.refresh is LoadState.NotLoading
            if (loaded && paged.itemCount == 0) {
                if (filtering) EmptyState(tr("No matches", "موردی پیدا نشد"), tr("Try removing a filter.", "یکی از فیلترها رو بردار."))
                else EmptyState(tr("Your library is empty", "کتابخانه‌ات خالیه"), tr("Add a game by hand, or import a list from a file.", "یه بازی رو دستی اضافه کن یا فهرست بازی‌ها رو از فایل وارد کن.")) {
                    Button(onClick = onAdd) { Text(tr("Add a game", "افزودن بازی")) }
                    OutlinedButton(onClick = onImport) { Text(tr("Import from file", "ورود از فایل")) }
                }
            } else if (grid) {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(150.dp), horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(bottom = 96.dp),
                ) {
                    items(count = paged.itemCount, key = paged.itemKey { it.id }) { idx ->
                        val g = paged[idx] ?: return@items
                        GameTile(g, sources[g.id]?.firstOrNull()?.label()) { onOpen(g.id) }
                    }
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp), contentPadding = PaddingValues(bottom = 96.dp)) {
                    items(count = paged.itemCount, key = paged.itemKey { it.id }) { idx ->
                        val g = paged[idx] ?: return@items
                        Row(Modifier.fillMaxWidth().clickable { onOpen(g.id) }, horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Cover(g.coverUri, g.title, Modifier.size(width = 56.dp, height = 78.dp))
                            Column(Modifier.weight(1f)) {
                                Text(g.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(
                                    listOfNotNull(g.status.label(), sources[g.id]?.firstOrNull()?.label(), g.developer).joinToString(" · "),
                                    style = MaterialTheme.typography.bodySmall, color = NexusColors.TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis,
                                )
                            }
                            if (g.favorite) Icon(Icons.Default.Favorite, tr("Favorite", "موردعلاقه"), tint = NexusColors.Accent)
                        }
                    }
                }
            }
        }
        FloatingActionButton(
            onClick = onAdd, containerColor = NexusColors.Accent, contentColor = NexusColors.OnAccent,
            modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp),
        ) { Icon(Icons.Default.Add, tr("Add a game", "افزودن بازی")) }
    }

    if (sheet) ModalBottomSheet(onDismissRequest = { sheet = false }, containerColor = NexusColors.Surface) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Section(tr("Source", "منبع")) {
                ChipRow(listOf<SourceType?>(null) + SourceType.values(), filter.source, { it?.label() ?: tr("All", "همه") }, vm::setSource)
            }
            if (collections.isNotEmpty()) Section(tr("Collection", "کلکسیون")) {
                ChipRow(
                    listOf<CollectionEntity?>(null) + collections, collections.firstOrNull { it.id == filter.collectionId },
                    { it?.name ?: tr("All", "همه") },
                ) { vm.setCollection(it?.id) }
            }
            Section(tr("Sort by", "مرتب‌سازی")) {
                val names = listOf(tr("Name", "نام"), tr("Recently added", "تازه‌ترین"), tr("Rating", "امتیاز"))
                ChipRow(listOf(0, 1, 2), filter.sort, { names[it] }, vm::setSort)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(tr("Favorites only", "فقط موردعلاقه‌ها"), Modifier.weight(1f))
                Switch(filter.favoritesOnly, { vm.setFavorites(it) })
            }
            TextButton(onClick = { vm.filter.value = LibraryFilter(); sheet = false }) { Text(tr("Clear all filters", "پاک‌کردن همه‌ی فیلترها")) }
        }
    }
}

@Composable
private fun GameTile(g: GameEntity, source: String?, onClick: () -> Unit) {
    Box(Modifier.fillMaxWidth().aspectRatio(2f / 3f).clip(RoundedCornerShape(14.dp)).clickable(onClick = onClick)) {
        Cover(g.coverUri, g.title, Modifier.fillMaxSize(), radius = 0)
        Box(Modifier.fillMaxWidth().fillMaxHeight(0.55f).align(Alignment.BottomCenter).background(ScrimBrush))
        Column(Modifier.align(Alignment.BottomStart).padding(10.dp)) {
            Text(g.title, style = MaterialTheme.typography.labelLarge, color = Color.White, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(
                listOfNotNull(g.status.label(), source).joinToString(" · "),
                style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.78f), maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
        }
        if (g.favorite) Icon(Icons.Default.Favorite, tr("Favorite", "موردعلاقه"), tint = NexusColors.Accent, modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).size(20.dp))
    }
}
