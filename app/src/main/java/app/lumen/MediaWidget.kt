package app.lumen

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.text.TextPaint
import android.text.TextUtils
import android.widget.RemoteViews
import androidx.core.content.edit

/**
 * Now playing on the lock screen, fed by Fluid (its Media.kt) over an explicit broadcast whenever the
 * track changes: greyscale art, the title heavy, the artist in spaced caps — the clock widget's language.
 * No transport controls: Samsung's own chip has those; a tap opens the player. Nothing playing = empty slot.
 */
class MediaWidget : AppWidgetProvider() {
    override fun onUpdate(ctx: Context, mgr: AppWidgetManager, ids: IntArray) = render(ctx)
    override fun onAppWidgetOptionsChanged(ctx: Context, mgr: AppWidgetManager, id: Int, options: android.os.Bundle) = render(ctx)

    override fun onReceive(ctx: Context, intent: Intent) {
        super.onReceive(ctx, intent)
        if (intent.action != MEDIA) return
        // The receiver is exported (any app may broadcast at it), so cap what we accept: short strings,
        // a package name that actually resolves, and a small image — spoofed text is the accepted ceiling.
        val pkg = intent.getStringExtra("pkg").orEmpty().take(100)
        ctx.getSharedPreferences("lumen", Context.MODE_PRIVATE).edit {
            putString("m_title", intent.getStringExtra("title").orEmpty().take(150))
            putString("m_artist", intent.getStringExtra("artist").orEmpty().take(150))
            putString("m_pkg", if (ctx.packageManager.getLaunchIntentForPackage(pkg) != null) pkg else "")
        }
        val art = intent.getByteArrayExtra("art")?.takeIf { it.size <= 512 * 1024 }
        runCatching {
            ctx.openFileOutput(ART, Context.MODE_PRIVATE).use { it.write(art ?: ByteArray(0)) }
        }
        render(ctx)
    }

    companion object {
        const val MEDIA = "app.lumen.MEDIA"
        private const val ART = "media_art.png"

        fun render(ctx: Context) {
            val mgr = AppWidgetManager.getInstance(ctx)
            val ids = mgr.getAppWidgetIds(ComponentName(ctx, MediaWidget::class.java))
            if (ids.isEmpty()) return
            val sp = ctx.getSharedPreferences("lumen", Context.MODE_PRIVATE)
            val title = sp.getString("m_title", "").orEmpty()
            val artist = sp.getString("m_artist", "").orEmpty()
            // Bounds-check before decoding: a hostile broadcast could carry a tiny PNG that inflates huge.
            val art = runCatching {
                val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                ctx.openFileInput(ART).use { BitmapFactory.decodeStream(it, null, opts) }
                if (opts.outWidth !in 1..2048 || opts.outHeight !in 1..2048) null
                else ctx.openFileInput(ART).use { BitmapFactory.decodeStream(it) }
            }.getOrNull()
            val open = sp.getString("m_pkg", "").orEmpty().takeIf { it.isNotEmpty() }
                ?.let { ctx.packageManager.getLaunchIntentForPackage(it) }
                ?.let { PendingIntent.getActivity(ctx, 2, it, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT) }
            val empty = title.isEmpty()
            for (id in ids) mgr.updateAppWidget(id, RemoteViews(ctx.packageName, R.layout.media_widget).apply {
                setImageViewBitmap(R.id.media,
                    if (empty) Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
                    else inSlot(ctx, id, draw(ctx, title, artist, art), bottom = false))
                setContentDescription(R.id.media, if (empty) "Nothing playing" else "$title, $artist")
                setOnClickPendingIntent(R.id.media, if (empty) null else open)
            })
        }

        private fun draw(ctx: Context, title: String, artist: String, art: Bitmap?): Bitmap {
            val d = ctx.resources.displayMetrics.density
            val titleInk = ink(ctx, R.font.montserrat_heavy, 15f)
            val artistInk = ink(ctx, R.font.montserrat, 9.5f, 0.3f)
            val pad = PAD * d
            val side = 44 * d
            val gap = 12 * d
            val maxText = 230 * d
            val line1 = TextUtils.ellipsize(title, titleInk, maxText, TextUtils.TruncateAt.END).toString()
            val line2 = TextUtils.ellipsize(artist.uppercase(), artistInk, maxText, TextUtils.TruncateAt.END).toString()
            val textW = maxOf(titleInk.measureText(line1), artistInk.measureText(line2))
            val artW = if (art != null) side + gap else 0f
            val bmp = Bitmap.createBitmap((pad * 2 + artW + textW).toInt(), (pad * 2 + side).toInt(), Bitmap.Config.ARGB_8888)
            Canvas(bmp).apply {
                if (art != null) {
                    val r = RectF(pad, pad, pad + side, pad + side)
                    // Full colour: the user wants the album's own colours on the lock screen.
                    val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
                    save()
                    clipPath(Path().apply { addRoundRect(r, 10 * d, 10 * d, Path.Direction.CW) })
                    // Centre-crop into the square, like the launcher's thumb.
                    val k = maxOf(r.width() / art.width, r.height() / art.height)
                    val dx = r.left - (art.width * k - r.width()) / 2
                    val dy = r.top - (art.height * k - r.height()) / 2
                    drawBitmap(art, null, RectF(dx, dy, dx + art.width * k, dy + art.height * k), paint)
                    restore()
                }
                val x = pad + artW
                val mid = pad + side / 2
                for (shade in listOf(true, false)) { // the dark cloud under the text first, then the white text
                    fun p(ink: TextPaint) = if (shade) shadeOf(ink, d) else ink
                    drawText(line1, x, mid - 4 * d, p(titleInk))
                    if (line2.isNotEmpty()) drawText(line2, x, mid + artistInk.textSize + 4 * d, p(artistInk))
                }
            }
            return bmp
        }
    }
}
