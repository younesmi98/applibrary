package com.nexus.app

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.core.content.FileProvider
import com.nexus.insights.Wrapped
import java.io.File

/** Draws the Wrapped numbers onto a PNG. Only values that were really recorded are drawn. */
object WrappedImage {
    fun lines(w: Wrapped): List<Pair<String, String>> = buildList {
        add(tr("Recorded time", "زمان ثبت‌شده") to formatDuration(w.recordedMinutes * 60_000L))
        w.topGame?.let { add(tr("Most played", "بیشترین بازی") to "${it.first} (${formatDuration(it.second * 60_000L)})") }
        w.mostActiveMonth?.let { add(tr("Most active month", "فعال‌ترین ماه") to "${digits(it.first.toString())} (${formatDuration(it.second * 60_000L)})") }
        add(tr("Games played", "بازی‌های انجام‌شده") to num(w.gamesPlayed))
        add(tr("Longest session", "طولانی‌ترین جلسه") to formatDuration(w.longestSessionMinutes * 60_000L))
        add(tr("Games added", "بازی‌های اضافه‌شده") to num(w.newGames))
    }

    fun render(w: Wrapped): Bitmap {
        val bmp = Bitmap.createBitmap(1080, 1350, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.drawColor(0xFF0E1012.toInt())
        val accent = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF3DD6C6.toInt(); textSize = 84f; isFakeBoldText = true }
        val label = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF9AA4AC.toInt(); textSize = 36f }
        val value = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFE8EBED.toInt(); textSize = 52f }
        val fa = I18n.fa
        val x = if (fa) 1000f else 80f
        val align = if (fa) Paint.Align.RIGHT else Paint.Align.LEFT
        listOf(accent, label, value).forEach { it.textAlign = align }
        c.drawText("NEXUS ${num(w.year)}", x, 170f, accent)
        var y = 330f
        for ((l, v) in lines(w)) {
            c.drawText(l, x, y, label)
            c.drawText(v.take(32), x, y + 62f, value)
            y += 160f
        }
        c.drawText(tr("Only sessions recorded by NEXUS. Store hours are not included.", "فقط جلسه‌های ثبت‌شده توسط NEXUS. ساعت‌های استور حساب نشده."), x, 1290f, label.apply { textSize = 28f })
        return bmp
    }

    fun share(ctx: Context, w: Wrapped) {
        val dir = File(ctx.cacheDir, "shared").apply { mkdirs() }
        val file = File(dir, "nexus-wrapped-${w.year}.png")
        file.outputStream().use { render(w).compress(Bitmap.CompressFormat.PNG, 100, it) }
        val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.files", file)
        val send = Intent(Intent.ACTION_SEND).setType("image/png")
            .putExtra(Intent.EXTRA_STREAM, uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        ctx.startActivity(Intent.createChooser(send, tr("Share Wrapped", "اشتراک مرور سال")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}
