package com.nexus.app

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/** Thin line icons from the design sheet (24 grid, 1.6 stroke, round caps). Tint is applied by Icon(). */
object NexusIcons {
    private fun line(name: String, width: Float, vararg d: String): ImageVector =
        ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).apply {
            d.forEach {
                addPath(addPathNodes(it), stroke = SolidColor(Color.Black), strokeLineWidth = width, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
            }
        }.build()

    val Home = line("home", 1.6f, "M4 11l8-7 8 7v9H4z")
    val Library = line("library", 1.6f, "M4 4h6v6H4z", "M14 4h6v6h-6z", "M4 14h6v6H4z", "M14 14h6v6h-6z")
    val Stats = line("stats", 1.6f, "M5 20V10M12 20V4M19 20v-7")
    val More = line("more", 3f, "M5 12h.01M12 12h.01M19 12h.01")
    val Plus = line("plus", 1.8f, "M12 5v14M5 12h14")
    val List = line("list", 1.8f, "M4 7h16M4 12h16M4 17h16")
    val Clock = line("clock", 1.6f, "M12 4a8 8 0 1 0 0.01 0z", "M12 8v4l3 2")
    val Filter = line("filter", 1.6f, "M4 7h9M17 7h3M4 12h3M11 12h9M4 17h11M19 17h1")
    val Sort = line("sort", 1.6f, "M8 5v14M8 19l-3-3M8 19l3-3M16 19V5M16 5l-3 3M16 5l3 3")
    val ChevronDown = line("chevron_down", 1.8f, "M6 9l6 6 6-6")
    val Check = line("check", 1.8f, "M5 12l5 5 9-10")
}
