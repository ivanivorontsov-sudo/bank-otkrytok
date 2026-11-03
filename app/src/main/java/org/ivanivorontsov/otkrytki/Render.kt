package org.ivanivorontsov.otkrytki

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint

data class FontOption(val title: String, val face: Typeface)

val fonts = listOf(
    FontOption("Классика", Typeface.create(Typeface.SERIF, Typeface.BOLD_ITALIC)),
    FontOption("Строгий", Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)),
    FontOption("Рукописный", Typeface.create("cursive", Typeface.BOLD)),
    FontOption("Узкий", Typeface.create("sans-serif-condensed", Typeface.BOLD)),
    FontOption("Печатный", Typeface.create(Typeface.MONOSPACE, Typeface.BOLD))
)

val textColors = listOf(0xFFFFFFFF.toInt(), 0xFF222222.toInt(), 0xFFFFD54F.toInt(), 0xFFE53935.toInt(), 0xFF1E88E5.toInt(), 0xFF43A047.toInt(), 0xFF8E24AA.toInt(), 0xFFFF7A59.toInt())

val backgrounds = listOf(
    intArrayOf(0xFFFF9A8B.toInt(), 0xFFFF6A88.toInt(), 0xFFFF99AC.toInt()),
    intArrayOf(0xFFA1C4FD.toInt(), 0xFFC2E9FB.toInt()),
    intArrayOf(0xFFFDDB92.toInt(), 0xFFD1FDFF.toInt()),
    intArrayOf(0xFF84FAB0.toInt(), 0xFF8FD3F4.toInt()),
    intArrayOf(0xFF30CFD0.toInt(), 0xFF330867.toInt()),
    intArrayOf(0xFFF6D365.toInt(), 0xFFFDA085.toInt()),
    intArrayOf(0xFF0F2027.toInt(), 0xFF2C5364.toInt()),
    intArrayOf(0xFFE0C3FC.toInt(), 0xFF8EC5FC.toInt())
)

fun gradientBitmap(colors: IntArray, w: Int = 1024, h: Int = 1280): Bitmap {
    val b = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val p = Paint().apply { shader = LinearGradient(0f, 0f, w.toFloat(), h.toFloat(), colors, null, Shader.TileMode.CLAMP) }
    Canvas(b).drawRect(0f, 0f, w.toFloat(), h.toFloat(), p)
    return b
}

data class CardStyle(
    val text: String = "",
    val font: Int = 0,
    val size: Float = 0.07f,
    val color: Int = 0xFFFFFFFF.toInt(),
    val x: Float = 0.5f,
    val y: Float = 0.75f,
    val align: Int = 1,
    val plate: Boolean = false,
    val aiImage: Boolean = false,
    val aiText: Boolean = false,
    val aiLabel: Boolean = true
)

fun renderCard(src: Bitmap, st: CardStyle, maxSide: Int = 1600): Bitmap {
    val scale = minOf(1f, maxSide.toFloat() / maxOf(src.width, src.height))
    val w = (src.width * scale).toInt().coerceAtLeast(1)
    val h = (src.height * scale).toInt().coerceAtLeast(1)
    val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val c = Canvas(out)
    c.drawBitmap(Bitmap.createScaledBitmap(src, w, h, true), 0f, 0f, null)
    if (st.text.isNotBlank()) {
        val tp = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = fonts[st.font.coerceIn(fonts.indices)].face
            textSize = w * st.size
            color = st.color
            val dark = android.graphics.Color.luminance(st.color) > 0.5f
            setShadowLayer(w * 0.008f, 0f, w * 0.003f, if (dark) 0xAA000000.toInt() else 0x88FFFFFF.toInt())
        }
        val boxW = (w * 0.88f).toInt()
        val al = when (st.align) { 0 -> Layout.Alignment.ALIGN_NORMAL; 2 -> Layout.Alignment.ALIGN_OPPOSITE; else -> Layout.Alignment.ALIGN_CENTER }
        val layout = StaticLayout.Builder.obtain(st.text, 0, st.text.length, tp, boxW).setAlignment(al).setLineSpacing(0f, 1.1f).build()
        val left = (w * st.x - boxW / 2f).coerceIn(0f, (w - boxW).toFloat())
        val top = (h * st.y - layout.height / 2f).coerceIn(0f, (h - layout.height).toFloat().coerceAtLeast(0f))
        if (st.plate) {
            val pad = w * 0.03f
            val bg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = if (android.graphics.Color.luminance(st.color) > 0.5f) 0x88000000.toInt() else 0xBBFFFFFF.toInt() }
            c.drawRoundRect(RectF(left - pad, top - pad, left + boxW + pad, top + layout.height + pad), pad, pad, bg)
        }
        c.save()
        c.translate(left, top)
        layout.draw(c)
        c.restore()
    }
    if (st.aiLabel && (st.aiImage || st.aiText)) {
        val label = if (st.aiImage) "Создано ИИ" else "Текст: ИИ"
        val lp = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = w * 0.026f; color = 0xFFFFFFFF.toInt(); typeface = Typeface.DEFAULT_BOLD }
        val tw = lp.measureText(label)
        val pad = w * 0.012f
        val r = RectF(w - tw - pad * 3, h - lp.textSize - pad * 3, w - pad, h - pad)
        c.drawRoundRect(r, pad, pad, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x99000000.toInt() })
        c.drawText(label, r.left + pad, r.bottom - pad - lp.descent(), lp)
    }
    return out
}
