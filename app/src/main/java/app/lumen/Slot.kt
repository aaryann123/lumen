package app.lumen

import android.appwidget.AppWidgetManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.text.TextPaint
import androidx.core.content.res.ResourcesCompat

/** Room around each widget's text for its glow and shade (dp); the text also starts this far in from the slot's edge. */
const val PAD = 20f

/**
 * Places [art] (drawn with [PAD] of room on every side) in a bitmap exactly the size of widget [id]'s slot,
 * so the host shows it 1:1. Every widget then starts its text the same distance from the slot's left edge, and the
 * clock can sit at the bottom of its tall slot right above the quote. Never scaled up, so text sizes stay put.
 */
fun inSlot(ctx: Context, id: Int, art: Bitmap, bottom: Boolean): Bitmap {
    val o = AppWidgetManager.getInstance(ctx).getAppWidgetOptions(id)
    val d = ctx.resources.displayMetrics.density
    val w = o.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH) * d
    val h = o.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT) * d
    if (w < 1 || h < 1) return art
    val pad = PAD * d
    val k = minOf(1f, (w - pad * 2) / (art.width - pad * 2), h / art.height)
    val left = pad - pad * k // the text still starts PAD in when the art has to shrink
    val top = if (bottom) h - art.height * k else 0f // the art's own pad keeps its glow and shade inside the slot
    return Bitmap.createBitmap(w.toInt(), h.toInt(), Bitmap.Config.ARGB_8888).also {
        Canvas(it).drawBitmap(art, null, RectF(left, top, left + art.width * k, top + art.height * k), Paint(Paint.FILTER_BITMAP_FLAG))
    }
}

/** Every widget's white text, with one soft dark glow for crisp edges; [shadeOf] does the heavy lifting on light walls. */
fun ink(ctx: Context, font: Int, size: Float, spacing: Float = 0f) = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
    val d = ctx.resources.displayMetrics.density
    typeface = ResourcesCompat.getFont(ctx, font)
    textSize = size * d
    letterSpacing = spacing
    color = Color.WHITE
    setShadowLayer(6 * d, 0f, 1 * d, 0xE6000000.toInt())
}

/**
 * [ink] as a soft dark cloud: drawn under the white text, it lets the words read on the light, busy lock wallpapers
 * and melts into dark ones. Thickened and blurred, so it hugs the words; a rectangle read as a grey box. It spreads
 * about 15dp, well inside [PAD], so the art's edge never cuts it.
 */
fun shadeOf(ink: TextPaint, d: Float) = TextPaint(ink).apply {
    clearShadowLayer()
    color = 0x8C000000.toInt()
    style = Paint.Style.FILL_AND_STROKE
    strokeWidth = 6 * d
    strokeJoin = Paint.Join.ROUND
    maskFilter = BlurMaskFilter(12 * d, BlurMaskFilter.Blur.NORMAL)
}
