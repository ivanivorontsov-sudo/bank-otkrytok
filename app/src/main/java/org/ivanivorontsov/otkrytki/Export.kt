package org.ivanivorontsov.otkrytki

import android.graphics.Bitmap
import java.io.ByteArrayOutputStream

/** «Съедание» открытки: уменьшение и сжатие в JPEG, удобный для мессенджеров. */
object Export {
    const val MAX_SIDE = 1440
    const val MAX_BYTES = 450_000

    fun shrink(b: Bitmap, maxSide: Int = MAX_SIDE): Bitmap {
        val s = maxSide.toFloat() / maxOf(b.width, b.height)
        if (s >= 1f) return b
        return Bitmap.createScaledBitmap(b, (b.width * s).toInt().coerceAtLeast(1), (b.height * s).toInt().coerceAtLeast(1), true)
    }

    fun jpeg(b: Bitmap, maxBytes: Int = MAX_BYTES, maxSide: Int = MAX_SIDE): ByteArray {
        var img = shrink(b, maxSide)
        var q = 90
        while (true) {
            val out = ByteArrayOutputStream()
            img.compress(Bitmap.CompressFormat.JPEG, q, out)
            val bytes = out.toByteArray()
            if (bytes.size <= maxBytes) return bytes
            if (q > 60) q -= 8
            else if (img.width > 640) { img = shrink(img, (maxOf(img.width, img.height) * 0.8f).toInt()); q = 82 }
            else return bytes
        }
    }
}
