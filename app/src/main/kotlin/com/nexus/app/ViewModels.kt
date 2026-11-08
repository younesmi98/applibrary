package com.nexus.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.nexus.core.database.*
import com.nexus.steam.SteamPlaytimeSnapshotEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

/** Emits the current time every minute, so "playing now" expires on its own without a new database change. */
private val minuteTicker: Flow<Long> = flow { while (true) { emit(System.currentTimeMillis()); delay(60_000) } }

class LibraryViewModel(private val app: NexusApp) : ViewModel() {
    val filter = MutableStateFlow(LibraryFilter(sort = AppPrefs.defaultSort))

    @OptIn(ExperimentalCoroutinesApi::class)
    val games: Flow<PagingData<GameEntity>> = filter.flatMapLatest { app.repo.library(it) }.cachedIn(viewModelScope)

    private val allSources = app.db.insights().allSourcesFlow()
    val sources: StateFlow<Map<String, List<SourceType>>> = allSources
        .map { rows -> rows.groupBy({ it.gameId }, { it.type }) }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())
    val stats: StateFlow<Map<String, GameStats>> = allSources
        .map { rows -> rows.groupBy { it.gameId }.mapValues { it.value.toStats() } }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())
    val playingIds: StateFlow<Set<String>> = combine(allSources, app.db.insights().account("STEAM"), minuteTicker) { src, acc, now -> playingGameIds(acc, src, now) }
        .flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())
    val collections = app.db.collections().all().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setQuery(q: String) = filter.update { it.copy(query = q) }
    fun setSource(s: SourceType?) = filter.update { it.copy(source = s) }
    fun setStatus(s: GameStatus?) = filter.update { it.copy(status = s) }
    fun setCollection(id: String?) = filter.update { it.copy(collectionId = id) }
<<<<<<< HEAD
    val collections = app.db.collections().all().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val playtime = MutableStateFlow<Map<String, Long>>(emptyMap())
    /** Minutes recorded by NEXUS (phone and PC sessions). Store-reported hours are never mixed in here. */
    fun refreshPlaytime() = viewModelScope.launch {
        playtime.value = InsightsLoader.load(app.db).games.associate { it.id to (it.pcMinutes + it.manualMinutes).toLong() }
    }
    fun toggleFavorite(g: GameEntity) = viewModelScope.launch { app.repo.toggleFavorite(g) }
    fun setSegment(i: Int) = filter.update {
        it.copy(status = when (i) { 1 -> GameStatus.PLAYING; 2 -> GameStatus.COMPLETED; else -> null }, favoritesOnly = i == 3)
    }
=======
>>>>>>> 7d946ca (NEXUS 0.3.0 redesign and release preparation)
    fun setSort(sort: Int) = filter.update { it.copy(sort = sort) }
    fun toggleFavorite(g: GameEntity) = viewModelScope.launch { app.repo.toggleFavorite(g) }

    /** 0 all, 1 recently played, 2 backlog, 3 completed, 4 favorites. */
    fun setSegment(i: Int) = filter.update {
        it.copy(
            status = when (i) { 2 -> GameStatus.BACKLOG; 3 -> GameStatus.COMPLETED; else -> null },
            favoritesOnly = i == 4, recentOnly = i == 1,
        )
    }
}

data class NowPlaying(val appId: String, val title: String, val card: GameCard?, val checkedAt: Long)
data class HomeOverview(val total: Int, val perPlatform: Map<SourceType, Int>, val storeMinutes: Long?, val favorites: Int)
data class HomeState(
    val nowPlaying: NowPlaying?, val recent: List<GameCard>, val overview: HomeOverview,
    val favorites: List<GameCard>, val explore: List<GameCard>, val account: PlatformAccountEntity?,
)
sealed interface HomeUi { data object Loading : HomeUi; data class Ready(val s: HomeState) : HomeUi }

class HomeViewModel(private val app: NexusApp) : ViewModel() {
    val ui: StateFlow<HomeUi> = combine(
        app.db.insights().gamesFlow(), app.db.insights().allSourcesFlow(), app.db.insights().account("STEAM"), minuteTicker,
    ) { games, sources, account, now ->
        val by = sources.groupBy { it.gameId }
        val cards = games.map { g -> GameCard(g, by[g.id].orEmpty(), by[g.id].orEmpty().toStats()) }
        val playing = playingGameIds(account, sources, now)
        val fresh = account?.takeIf { it.currentAppId != null && it.currentCheckedAt != null && now - it.currentCheckedAt <= PRESENCE_FRESH_MS }
        val nowPlaying = fresh?.let { a ->
            val card = cards.firstOrNull { it.game.id in playing }
            NowPlaying(a.currentAppId!!, card?.game?.title ?: a.currentGameName ?: "Steam", card, a.currentCheckedAt!!)
        }
        val known = cards.filter { it.stats.forever != null }
        HomeUi.Ready(HomeState(
            nowPlaying = nowPlaying,
            recent = cards.filter { it.stats.lastPlayed != null }.sortedByDescending { it.stats.lastPlayed }.take(10),
            overview = HomeOverview(
                total = cards.size,
                perPlatform = cards.flatMap { c -> c.sources.map { it.type }.distinct() }.groupingBy { it }.eachCount(),
                storeMinutes = if (known.isEmpty()) null else known.sumOf { it.stats.forever!!.toLong() },
                favorites = cards.count { it.game.favorite },
            ),
            favorites = cards.filter { it.game.favorite }.take(10),
            explore = known.filter { it.stats.forever == 0 || (it.stats.lastPlayed != null && now - it.stats.lastPlayed > 180L * 86_400_000L) }
                .sortedBy { it.game.title.lowercase() }.take(10),
            account = account,
        ))
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeUi.Loading)
}

class DetailViewModel(private val app: NexusApp, val gameId: String) : ViewModel() {
    private val db = app.db
    private fun <T> Flow<T>.hold(initial: T) = stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), initial)

    /** False until the first database answer arrives, so the screen can show a skeleton instead of "not found". */
    val ready = MutableStateFlow(false)
<<<<<<< HEAD
    val game = db.games().observe(gameId).onEach { ready.value = true }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    val images = db.insights().images(gameId).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val sources = db.details().sources(gameId).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val tags = db.details().tags(gameId).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val adjustments = db.sessions().adjustments(gameId).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeSession = MutableStateFlow<ActiveSession?>(null)
    val otherGameRunning = MutableStateFlow(false)
    private val steamMinutes = MutableStateFlow<Int?>(null)

    /** Finished sessions with their active duration (pauses removed). */
    val history: StateFlow<List<Pair<GameSessionEntity, Long>>> = db.sessions().history(gameId)
        .map { list ->
            val now = System.currentTimeMillis()
            list.filter { it.state == SessionState.FINISHED }
                .map { it to PlaytimeCalc.activeMillis(it, db.sessions().pauses(it.id), now) }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val playtime: StateFlow<PlaytimeBreakdown> = combine(
        steamMinutes, db.pc().trackedMillis(gameId), history, adjustments,
    ) { steam, pc, hist, adj ->
        PlaytimeBreakdown(
            steamMinutes = steam,
            windowsMillis = pc,
            phoneSessionMillis = hist.filter { it.first.method == SessionMethod.MANUAL }.sumOf { it.second },
            adjustmentMillis = adj.sumOf { it.deltaMillis },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PlaytimeBreakdown(null, 0, 0, 0))


    // ---- journal, collections, Windows rules
    val journal = db.insights().journal().map { l -> l.filter { it.gameId == gameId } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allCollections = db.collections().all().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val myCollectionIds = db.insights().collectionsOf(gameId).map { l -> l.map { it.collectionId }.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())
    val rules = db.pc().rulesFor(gameId).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addJournal(title: String, body: String) = viewModelScope.launch {
        db.insights().upsertJournal(JournalEntity(UUID.randomUUID().toString(), gameId, System.currentTimeMillis(), title, body, null))
    }
    fun deleteJournal(id: String) = viewModelScope.launch { db.insights().deleteJournal(id) }
    fun toggleCollection(collectionId: String, isIn: Boolean) = viewModelScope.launch {
        if (isIn) db.collections().removeItem(collectionId, gameId) else db.collections().addItem(CollectionItemEntity(collectionId, gameId))
    }
    fun saveRule(exe: String, mode: TrackingMode, idleMinutes: Int?) = viewModelScope.launch {
        val name = exe.trim()
        if (name.isEmpty() || '/' in name || '\\' in name) return@launch   // executable name only, never a path
        db.pc().upsertRule(ProcessRuleEntity(UUID.randomUUID().toString(), gameId, null, name, mode, idleMinutes, true))
    }
    fun setCover(uri: String?) = viewModelScope.launch { game.value?.let { db.games().update(it.copy(coverUri = uri, updatedAt = System.currentTimeMillis())) } }
    fun setBackground(uri: String?) = viewModelScope.launch { game.value?.let { db.games().update(it.copy(backgroundUri = uri, updatedAt = System.currentTimeMillis())) } }
    fun addImages(uris: List<String>) = viewModelScope.launch {
        val now = System.currentTimeMillis()
        db.insights().addImages(uris.mapIndexed { i, u -> GameImageEntity(UUID.randomUUID().toString(), gameId, u, now + i) })
    }
    fun removeImage(id: String) = viewModelScope.launch { db.insights().deleteImage(id) }
    fun deleteRule(id: String) = viewModelScope.launch { db.pc().deleteRule(id) }

    init { refresh() }

    fun refresh() = viewModelScope.launch {
        steamMinutes.value = db.steam().lastMinutes(gameId)
        val live = db.sessions().unfinished()
        val mine = live.firstOrNull { it.gameId == gameId }
        activeSession.value = mine?.let { ActiveSession(it, db.sessions().pauses(it.id)) }
        otherGameRunning.value = mine == null && live.isNotEmpty()
    }

    /** Returns the new session id, or null if another session is already running. */
    suspend fun start(): String? {
        val id = runCatching { app.sessions.start(gameId) }.getOrNull()
        refresh().join()
        return id
    }
    suspend fun pause(id: String) { app.sessions.pause(id); refresh().join() }
    suspend fun resume(id: String) { app.sessions.resume(id); refresh().join() }
    suspend fun finish(id: String) { app.sessions.finish(id); refresh().join() }

    fun addAdjustment(minutes: Long, reason: String?) = viewModelScope.launch {
        if (minutes != 0L) app.sessions.addAdjustment(gameId, minutes * 60_000L, reason)
    }
    fun undoAdjustment(id: String) = viewModelScope.launch { app.sessions.undoAdjustment(id) }
    fun deleteSession(id: String) = viewModelScope.launch { db.sessions().delete(id) }
=======
    val game = db.games().observe(gameId).onEach { ready.value = true }.hold(null)
    val sources = db.details().sources(gameId).hold(emptyList())
    val tags = db.details().tags(gameId).hold(emptyList())
    val images = db.insights().images(gameId).hold(emptyList())
    val journal = db.insights().journal().map { l -> l.filter { it.gameId == gameId } }.hold(emptyList())
    val allCollections = db.collections().all().hold(emptyList())
    val myCollectionIds = db.insights().collectionsOf(gameId).map { l -> l.map { it.collectionId }.toSet() }.hold(emptySet())
    val account = db.insights().account("STEAM").hold(null)
    val snapshots: StateFlow<List<SteamPlaytimeSnapshotEntity>> = db.steam().history(gameId).hold(emptyList())
    val stats: StateFlow<GameStats> = sources.map { it.toStats() }.hold(GameStats(null, null, null, null, null))
    val playingNow: StateFlow<Boolean> = combine(sources, account, minuteTicker) { s, a, now -> playingGameIds(a, s, now).contains(gameId) }.hold(false)
>>>>>>> 7d946ca (NEXUS 0.3.0 redesign and release preparation)

    fun toggleFavorite() = viewModelScope.launch { game.value?.let { app.repo.toggleFavorite(it) } }
    fun setStatus(s: GameStatus) = viewModelScope.launch { game.value?.let { app.repo.setStatus(it, s) } }
    fun saveNotes(text: String) = viewModelScope.launch {
        game.value?.let { db.games().update(it.copy(notes = text.ifBlank { null }, updatedAt = System.currentTimeMillis())) }
    }
    fun delete() = viewModelScope.launch { app.repo.deleteEntry(gameId) }
    fun setCover(uri: String?) = viewModelScope.launch { game.value?.let { db.games().update(it.copy(coverUri = uri, updatedAt = System.currentTimeMillis())) } }
    fun setBackground(uri: String?) = viewModelScope.launch { game.value?.let { db.games().update(it.copy(backgroundUri = uri, updatedAt = System.currentTimeMillis())) } }
    fun addImages(uris: List<String>) = viewModelScope.launch {
        val now = System.currentTimeMillis()
        db.insights().addImages(uris.mapIndexed { i, u -> GameImageEntity(UUID.randomUUID().toString(), gameId, u, now + i) })
    }
    fun removeImage(id: String) = viewModelScope.launch { db.insights().deleteImage(id) }
    fun addJournal(title: String, body: String) = viewModelScope.launch {
        db.insights().upsertJournal(JournalEntity(UUID.randomUUID().toString(), gameId, System.currentTimeMillis(), title, body, null))
    }
    fun deleteJournal(id: String) = viewModelScope.launch { db.insights().deleteJournal(id) }
    fun toggleCollection(collectionId: String, isIn: Boolean) = viewModelScope.launch {
        if (isIn) db.collections().removeItem(collectionId, gameId) else db.collections().addItem(CollectionItemEntity(collectionId, gameId))
    }
}

class AddGameViewModel(private val app: NexusApp) : ViewModel() {
    suspend fun save(g: NewGame): String = app.repo.addGame(g)
}

class ConnectionsViewModel(private val app: NexusApp) : ViewModel() {
    val account = app.db.insights().account("STEAM").stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    val syncing = app.platform.syncing
    val outcome = MutableStateFlow<SyncOutcome?>(null)

    fun connect(idOrName: String, key: String) = viewModelScope.launch { outcome.value = app.platform.connect(idOrName, key) }
    fun sync() = viewModelScope.launch { outcome.value = app.platform.sync() }
    fun disconnect() = viewModelScope.launch { app.platform.disconnect(); outcome.value = null }
    fun clearOutcome() { outcome.value = null }
}

@Suppress("UNCHECKED_CAST")
class NexusVmFactory(private val app: NexusApp, private val gameId: String? = null) : androidx.lifecycle.ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T = when (modelClass) {
        LibraryViewModel::class.java -> LibraryViewModel(app)
        HomeViewModel::class.java -> HomeViewModel(app)
        DetailViewModel::class.java -> DetailViewModel(app, requireNotNull(gameId))
        AddGameViewModel::class.java -> AddGameViewModel(app)
        ConnectionsViewModel::class.java -> ConnectionsViewModel(app)
        else -> error("Unknown ViewModel $modelClass")
    } as T
}
