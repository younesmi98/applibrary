package com.nexus.app

import android.content.Context
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight

/**
 * Loads the bundled TTF files from assets/fonts (no network at run time).
 * A family is only built from files that really exist, so a missing file can never crash text rendering.
 */
object Fonts {
    var inter: FontFamily = FontFamily.Default; private set
    var vazirmatn: FontFamily = FontFamily.Default; private set
    var interBundled = false; private set
    var vazirmatnBundled = false; private set

    private val weights = listOf("Regular" to FontWeight.Normal, "Medium" to FontWeight.Medium, "SemiBold" to FontWeight.SemiBold, "Bold" to FontWeight.Bold)

    fun load(ctx: Context) {
        val present = runCatching { ctx.assets.list("fonts")?.toSet() }.getOrNull().orEmpty()
        fun family(prefix: String): FontFamily? {
            val fonts = weights.mapNotNull { (name, w) ->
                val file = "$prefix-$name.ttf"
                if (file in present) Font("fonts/$file", ctx.assets, w) else null
            }
            return if (fonts.isEmpty()) null else FontFamily(fonts)
        }
        family("Inter")?.let { inter = it; interBundled = true }
        family("Vazirmatn")?.let { vazirmatn = it; vazirmatnBundled = true }
    }
}
