package com.nexus.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.nexus.core.database.*
import com.nexus.pc.*
import com.nexus.playtime.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

class LibraryViewModel(private val app: NexusApp) : ViewModel() {
    val filter = MutableStateFlow(LibraryFilter())

    @OptIn(ExperimentalCoroutinesApi::class)
    val games: Flow<PagingData<GameEntity>> =
        filter.flatMapLatest { app.repo.library(it) }.cachedIn(viewModelScope)

    fun setQuery(q: String) = filter.update { it.copy(query = q) }
    fun setSource(s: SourceType?) = filter.update { it.copy(source = s) }
    fun setStatus(s: GameStatus?) = filter.update { it.copy(status = s) }
    fun setFavorites(on: Boolean) = filter.update { it.copy(favoritesOnly = on) }
    fun setCollection(id: String?) = filter.update { it.copy(collectionId = id) }
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
    fun setSort(sort: Int) = filter.update { it.copy(sort = sort) }

    val sources: StateFlow<Map<String, List<SourceType>>> = app.db.insights().sourceRowsFlow()
        .map { rows -> rows.groupBy({ it.gameId }, { it.type }) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val manualCount: StateFlow<Int> = app.db.games().manualCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
}

/** A live session plus its pauses, enough to compute elapsed time from stored timestamps. */
data class ActiveSession(val session: GameSessionEntity, val pauses: List<SessionPauseEntity>)

/** Playtime from four origins. They are shown side by side and never added together. */
data class PlaytimeBreakdown(
    val steamMinutes: Int?,       // last store snapshot, null = unknown
    val windowsMillis: Long,      // pc_sessions table
    val phoneSessionMillis: Long, // finished manual sessions
    val adjustmentMillis: Long,   // one-time manual adjustments
)

class DetailViewModel(private val app: NexusApp, val gameId: String) : ViewModel() {
    private val db = app.db

    /** False until the first database answer arrives, so the screen can show a skeleton instead of "not found". */
    val ready = MutableStateFlow(false)
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

    fun toggleFavorite() = viewModelScope.launch { game.value?.let { app.repo.toggleFavorite(it) } }
    fun setStatus(s: GameStatus) = viewModelScope.launch { game.value?.let { app.repo.setStatus(it, s) } }
    fun saveNotes(text: String) = viewModelScope.launch {
        game.value?.let { db.games().update(it.copy(notes = text.ifBlank { null }, updatedAt = System.currentTimeMillis())) }
    }
    fun delete() = viewModelScope.launch { app.repo.deleteEntry(gameId) }
}

class AddGameViewModel(private val app: NexusApp) : ViewModel() {
    suspend fun save(g: NewGame): String = app.repo.addGame(g)
}

class RecoveryViewModel(private val app: NexusApp) : ViewModel() {
    val interrupted = MutableStateFlow<List<Pair<GameSessionEntity, String>>>(emptyList())
    fun check() = viewModelScope.launch {
        if (SessionService.running) return@launch
        interrupted.value = app.sessions.findInterrupted().map { s ->
            s to (app.db.games().observe(s.gameId).firstOrNull()?.title ?: tr("Game", "بازی"))
        }
    }
    fun choose(id: String, c: RecoveryChoice, ctx: android.content.Context) = viewModelScope.launch {
        app.sessions.recover(id, c)
        if (c == RecoveryChoice.RESUME) SessionNotifier.push(ctx, id) else SessionNotifier.stop(ctx)
        interrupted.update { list -> list.filterNot { it.first.id == id } }
    }
}

@Suppress("UNCHECKED_CAST")
class NexusVmFactory(private val app: NexusApp, private val gameId: String? = null) : androidx.lifecycle.ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T = when (modelClass) {
        LibraryViewModel::class.java -> LibraryViewModel(app)
        DetailViewModel::class.java -> DetailViewModel(app, requireNotNull(gameId))
        AddGameViewModel::class.java -> AddGameViewModel(app)
        RecoveryViewModel::class.java -> RecoveryViewModel(app)
        DevicesViewModel::class.java -> DevicesViewModel(app.db.pc(), PairingManager(app.db.pc()), null)
        else -> error("Unknown ViewModel $modelClass")
    } as T
}
