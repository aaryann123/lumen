package app.lumen

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.text.LineBreaker
import android.text.StaticLayout
import android.text.TextPaint
import android.widget.RemoteViews
import androidx.core.content.edit

/** The one current quote, shared by the app and every widget. */
object Lumen {
    private fun prefs(ctx: Context) = ctx.getSharedPreferences("lumen", Context.MODE_PRIVATE)

    fun show(ctx: Context) = prefs(ctx).getString("show", "all") ?: "all"
    fun setShow(ctx: Context, show: String) = prefs(ctx).edit { putString("show", show) }

    fun currentIndex(ctx: Context) = prefs(ctx).getInt("current", 0).coerceIn(QUOTES.indices)
    fun current(ctx: Context) = QUOTES[currentIndex(ctx)]

    /** Moves on to a new random quote and puts it on the widgets. */
    fun advance(ctx: Context): Quote {
        val bag = prefs(ctx).getString("bag", "").orEmpty().split(',').mapNotNull { it.toIntOrNull() }
        val (i, rest) = deal(bag, pool(show(ctx)), prefs(ctx).getInt("current", -1))
        prefs(ctx).edit { putInt("current", i); putString("bag", rest.joinToString(",")) }
        QuoteWidget.render(ctx)
        return QUOTES[i]
    }
}

const val VILLAIN_TINT = 0xFFFFB4A8.toInt()
const val HERO_TINT = 0xFFA8CCFF.toInt()

/** See-through quote widget: a new line every 30 minutes, or on tap. */
class QuoteWidget : AppWidgetProvider() {
    override fun onUpdate(ctx: Context, mgr: AppWidgetManager, ids: IntArray) {
        Lumen.advance(ctx)
    }

    override fun onAppWidgetOptionsChanged(ctx: Context, mgr: AppWidgetManager, id: Int, options: android.os.Bundle) = render(ctx)

    override fun onReceive(ctx: Context, intent: Intent) {
        super.onReceive(ctx, intent)
        if (intent.action == NEXT) Lumen.advance(ctx)
    }

    companion object {
        private const val NEXT = "app.lumen.NEXT"

        fun render(ctx: Context) {
            val mgr = AppWidgetManager.getInstance(ctx)
            val ids = mgr.getAppWidgetIds(ComponentName(ctx, QuoteWidget::class.java))
            if (ids.isEmpty()) return
            val current = Lumen.currentIndex(ctx)
            val tap = PendingIntent.getBroadcast(ctx, 0, Intent(ctx, QuoteWidget::class.java).setAction(NEXT), PendingIntent.FLAG_IMMUTABLE)
            for (id in ids) {
                val h = mgr.getAppWidgetOptions(id).getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT)
                // As wide as the slot allows (less the PAD margins): wide lines mean fewer of them, so a short slot
                // doesn't have to shrink the text.
                val w = (mgr.getAppWidgetOptions(id).getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH) - PAD * 2).toInt().coerceIn(170, 360)
                // ponytail: short slots (one row, ~99dp tall) skip the very longest lines, which would have to shrink.
                val q = QUOTES[if (h in 1..119) fitting(current, 90) else current]
                mgr.updateAppWidget(id, RemoteViews(ctx.packageName, R.layout.quote_widget).apply {
                    // Always white over its own shade, like the clock: Samsung reports no colours for the rotating lock
                    // wallpaper, and guessing from the home wallpaper could leave the quote black under a white clock.
                    setImageViewBitmap(R.id.art, inSlot(ctx, id, draw(ctx, q, w), bottom = false))
                    setContentDescription(R.id.art, "${q.line} ${q.who}, ${q.film}")
                    setOnClickPendingIntent(R.id.art, tap)
                })
            }
        }

        /**
         * Desaturate Pro style: the first word in a handwritten script, the rest in heavy caps, then a hairline and the
         * date in spaced small caps. Left-aligned white text over a soft dark cloud ([shadeOf]), so it reads on any
         * wallpaper; no speaker. Always the same size.
         */
        private fun draw(ctx: Context, q: Quote, widthDp: Int): Bitmap {
            val d = ctx.resources.displayMetrics.density
            val width = (widthDp * d).toInt()
            fun layout(text: String, p: TextPaint, spacing: Float = 1f) =
                StaticLayout.Builder.obtain(text, 0, text.length, p, width).setLineSpacing(0f, spacing)
                    .setIncludePad(false).setBreakStrategy(LineBreaker.BREAK_STRATEGY_BALANCED).build()
            // ponytail: one- or two-letter openers ("I", "A") look lost in script, so they take the next word along.
            val words = q.line.split(' ')
            val take = if (words[0].length <= 2 && words.size > 2) 2 else 1
            val first = words.take(take).joinToString(" ")
            val rest = words.drop(take).joinToString(" ").uppercase()
            val scriptInk = ink(ctx, R.font.great_vibes, 30f)
            val capsInk = ink(ctx, R.font.montserrat_heavy, 12.5f, 0.04f)
            val date = ink(ctx, R.font.montserrat, 9.5f, 0.3f)
            val script = layout(first, scriptInk)
            val caps = layout(rest, capsInk, 1.05f)
            // No weekday: the clock's "Have a beautiful Wednesday" already says it.
            val stamp = java.text.SimpleDateFormat("d MMMM", java.util.Locale.getDefault()).format(java.util.Date()).uppercase()
            val tuck = 9 * d // the script's tail dips into the caps, like the reference
            val gap = 7 * d
            val rule = script.height - tuck + caps.height + gap
            val body = rule + gap + date.textSize
            val pad = (PAD * d).toInt()
            val bmp = Bitmap.createBitmap(width + pad * 2, pad * 2 + body.toInt(), Bitmap.Config.ARGB_8888)
            Canvas(bmp).apply {
                translate(pad.toFloat(), pad.toFloat())
                for (shade in listOf(true, false)) { // the dark cloud under everything first, then the white text
                    fun p(ink: TextPaint) = if (shade) shadeOf(ink, d) else ink
                    layout(first, p(scriptInk)).draw(this) // a stroke doesn't change widths, so the lines break the same
                    save(); translate(0f, script.height - tuck); layout(rest, p(capsInk), 1.05f).draw(this); restore()
                    drawRect(0f, rule, width * 0.62f, rule + 0.8f * d, p(date))
                    drawText(stamp, 0f, rule + gap + date.textSize * 0.8f, p(date))
                }
            }
            return bmp
        }
    }
}
