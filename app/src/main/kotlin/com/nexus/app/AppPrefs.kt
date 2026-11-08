package com.nexus.app

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

/** Small preferences kept on the phone. Every value is Compose state, so screens update the moment it changes. */
object AppPrefs {
    val accents = listOf(Color(0xFF29E69A), Color(0xFF8BD84B), Color(0xFFFFC15A), Color(0xFF58A6FF))   // Emerald, Lime, Amber, Sky
    var profileName by mutableStateOf("")
    var avatarUri by mutableStateOf<String?>(null)
    var defaultGrid by mutableStateOf(true)
    var defaultSort by mutableIntStateOf(0)       // 0 name, 1 added, 2 rating, 3 hours, 4 last played
    var autoSync by mutableStateOf(false)
    var accent by mutableIntStateOf(0)
    var deepBlack by mutableStateOf(false)

    private fun p(ctx: Context) = ctx.getSharedPreferences("nexus_prefs", Context.MODE_PRIVATE)

    fun load(ctx: Context) {
        val s = p(ctx)
        profileName = s.getString("profile_name", "") ?: ""
        avatarUri = s.getString("avatar_uri", null)
        defaultGrid = s.getBoolean("default_grid", true)
        defaultSort = s.getInt("default_sort", 0)
        autoSync = s.getBoolean("auto_sync", false)
        accent = s.getInt("accent", 0).coerceIn(0, accents.lastIndex)
        deepBlack = s.getBoolean("deep_black", false)
    }

    fun setProfile(ctx: Context, name: String, avatar: String?) {
        profileName = name; avatarUri = avatar
        p(ctx).edit().putString("profile_name", name).putString("avatar_uri", avatar).apply()
    }
    fun setDefaultGrid(ctx: Context, v: Boolean) { defaultGrid = v; p(ctx).edit().putBoolean("default_grid", v).apply() }
    fun setDefaultSort(ctx: Context, v: Int) { defaultSort = v; p(ctx).edit().putInt("default_sort", v).apply() }
    fun setAutoSync(ctx: Context, v: Boolean) { autoSync = v; p(ctx).edit().putBoolean("auto_sync", v).apply() }
    fun setAccent(ctx: Context, v: Int) { accent = v; p(ctx).edit().putInt("accent", v).apply() }
    fun setDeepBlack(ctx: Context, v: Boolean) { deepBlack = v; p(ctx).edit().putBoolean("deep_black", v).apply() }
}
