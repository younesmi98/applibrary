package com.nexus.app

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.nexus.polish.Localization
import java.util.Locale

enum class Lang { EN, FA }

/** In-app language. null override = follow the phone. Reading `lang` inside a composable recomposes it on change. */
object I18n {
    var override by mutableStateOf<Lang?>(null)
    private val system: Lang get() = if (Locale.getDefault().language == "fa") Lang.FA else Lang.EN
    val lang: Lang get() = override ?: system
    val fa: Boolean get() = lang == Lang.FA

    fun load(ctx: Context) {
        override = ctx.getSharedPreferences("nexus_prefs", Context.MODE_PRIVATE).getString("lang", null)
            ?.let { runCatching { Lang.valueOf(it) }.getOrNull() }
    }

    fun choose(ctx: Context, l: Lang?) {
        override = l
        ctx.getSharedPreferences("nexus_prefs", Context.MODE_PRIVATE).edit().putString("lang", l?.name).apply()
    }
}

fun tr(en: String, fa: String): String = if (I18n.fa) fa else en
fun digits(s: String): String = if (I18n.fa) Localization.persianDigits(s) else s
fun num(n: Int): String = digits(n.toString())
