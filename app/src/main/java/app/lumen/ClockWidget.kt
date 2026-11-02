package app.lumen

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.text.TextPaint
import android.widget.RemoteViews
import java.util.Calendar

private val HOURS = listOf("TWELVE", "ONE", "TWO", "THREE", "FOUR", "FIVE", "SIX", "SEVEN", "EIGHT", "NINE", "TEN", "ELEVEN")

/** Desaturate's "TWELVE 56 PM / Have a beautiful Monday": the hour as a heavy word, minutes thin, a greeting under it. */
fun clockLines(now: Calendar): List<String> = listOf(
    HOURS[now.get(Calendar.HOUR)],
    "%02d".format(now.get(Calendar.MINUTE)),
    if (now.get(Calendar.AM_PM) == Calendar.AM) "AM" else "PM",
    now.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, java.util.Locale.ENGLISH)!!,
)

class ClockWidget : AppWidgetProvider() {
    override fun onUpdate(ctx: Context, mgr: AppWidgetManager, ids: IntArray) = render(ctx)
    override fun onAppWidgetOptionsChanged(ctx: Context, mgr: AppWidgetManager, id: Int, options: android.os.Bundle) = render(ctx)

    override fun onReceive(ctx: Context, intent: Intent) {
        super.onReceive(ctx, intent)
        // An app update cancels our alarms and isn't always followed by an APPWIDGET_UPDATE, so restart the ticking here.
        if (intent.action == TICK || intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) render(ctx)
    }

    companion object {
        private const val TICK = "app.lumen.TICK"
        private const val GREETING = "Have a beautiful"

        fun render(ctx: Context) {
            val mgr = AppWidgetManager.getInstance(ctx)
            val ids = mgr.getAppWidgetIds(ComponentName(ctx, ClockWidget::class.java))
            if (ids.isEmpty()) return
            val now = Calendar.getInstance()
            val art = draw(ctx, clockLines(now))
            for (id in ids) mgr.updateAppWidget(id, RemoteViews(ctx.packageName, R.layout.clock_widget).apply {
                setImageViewBitmap(R.id.clock, inSlot(ctx, id, art, bottom = true))
                setContentDescription(R.id.clock, android.text.format.DateFormat.getTimeFormat(ctx).format(now.time))
            })
            // Next minute. RTC (not _WAKEUP): while the screen is off it waits and fires as soon as the phone wakes,
            // so the lock screen is right the moment you look and the phone is never woken just for this.
            val next = (now.clone() as Calendar).apply { add(Calendar.MINUTE, 1); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }
            val tick = PendingIntent.getBroadcast(ctx, 1, Intent(ctx, ClockWidget::class.java).setAction(TICK), PendingIntent.FLAG_IMMUTABLE)
            ctx.getSystemService(AlarmManager::class.java).setExact(AlarmManager.RTC, next.timeInMillis, tick)
        }

        // Regular, not Light, for the minutes and greeting: thin white strokes vanished on the light lock wallpapers.
        private fun draw(ctx: Context, l: List<String>): Bitmap {
            val d = ctx.resources.displayMetrics.density
            val hour = ink(ctx, R.font.montserrat_heavy, 34f)
            val min = ink(ctx, R.font.montserrat_regular, 34f)
            val ampm = ink(ctx, R.font.montserrat, 9f, 0.2f)
            val greet = ink(ctx, R.font.montserrat_regular, 17f)
            val day = ink(ctx, R.font.montserrat_heavy, 17f)
            val gap = 8 * d
            val top = hour.measureText(l[0]) + gap + min.measureText(l[1]) + 4 * d + ampm.measureText(l[2])
            val pad = PAD * d
            val text = maxOf(top, greet.measureText(GREETING), day.measureText(l[3]))
            val base = pad + hour.textSize * 0.8f
            val bottom = base + greet.textSize * 2.5f + day.descent()
            val bmp = Bitmap.createBitmap((text + pad * 2).toInt(), (bottom + pad).toInt(), Bitmap.Config.ARGB_8888)
            Canvas(bmp).apply {
                for (shade in listOf(true, false)) { // the dark cloud under everything first, then the white text
                    fun p(ink: TextPaint) = if (shade) shadeOf(ink, d) else ink
                    var x = pad
                    drawText(l[0], x, base, p(hour)); x += hour.measureText(l[0]) + gap
                    drawText(l[1], x, base, p(min)); x += min.measureText(l[1]) + 4 * d
                    drawText(l[2], x, base - hour.textSize * 0.45f, p(ampm))
                    drawText(GREETING, pad, base + greet.textSize * 1.3f, p(greet))
                    drawText(l[3], pad, base + greet.textSize * 2.5f, p(day))
                }
            }
            return bmp
        }
    }
}
