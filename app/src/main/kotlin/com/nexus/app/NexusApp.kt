package com.nexus.app

import android.app.Application
import android.content.Context
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.nexus.core.database.GameRepository
import com.nexus.core.database.NexusDatabase

<<<<<<< HEAD
class NexusApp : Application() {
    override fun onCreate() { super.onCreate(); I18n.load(this); Fonts.load(this); FxState.load(this) }
=======
class NexusApp : Application(), ImageLoaderFactory {
    override fun onCreate() { super.onCreate(); I18n.load(this); Fonts.load(this); FxState.load(this); AppPrefs.load(this) }

>>>>>>> 7d946ca (NEXUS 0.3.0 redesign and release preparation)
    val db: NexusDatabase by lazy { NexusDatabase.build(this) }
    val repo: GameRepository by lazy { GameRepository(db) }
    val platform: PlatformSync by lazy { PlatformSync(this) }

    /** Covers are cached on disk and in memory, so scrolling and offline use never download the same image twice. */
    override fun newImageLoader(): ImageLoader = ImageLoader.Builder(this)
        .memoryCache { MemoryCache.Builder(this).maxSizePercent(0.15).build() }
        .diskCache { DiskCache.Builder().directory(cacheDir.resolve("covers")).maxSizeBytes(200L * 1024 * 1024).build() }
        .respectCacheHeaders(false)
        .crossfade(180)
        .build()
}

val Context.nexus: NexusApp get() = applicationContext as NexusApp
