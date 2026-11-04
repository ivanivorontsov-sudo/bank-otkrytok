package org.ivanivorontsov.otkrytki

import android.graphics.BitmapFactory
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class ExportTest {
    private val ctx get() = ApplicationProvider.getApplicationContext<android.content.Context>()

    @Test fun poemsAndStockLoaded() {
        val all = Quotes.all(ctx)
        assertTrue("poems ${all.size}", all.size >= 150)
        occasions.forEach { o -> assertTrue(o.id, poemsFor(all, o).isNotEmpty()) }
        val stock = stockList(ctx)
        assertTrue("stock ${stock.size}", stock.size >= 80)
        stock.forEach { assertTrue(it.file, it.license.isNotBlank()) }
    }

    @Test fun composeCompressPipeline() {
        val stock = stockList(ctx)
        val outDir = File("build/card-samples").apply { mkdirs() }
        listOf("birthday", "newyear", "wedding", "victory").forEach { cat ->
            val s = stock.first { it.category == cat }
            val base = loadAsset(ctx, "stock/" + s.file)
            val q = poemsFor(Quotes.all(ctx), occasions.first { it.id == if (cat == "wedding") "wedding" else cat }).first()
            val card = renderCard(base, CardStyle(text = q.text.lines().take(4).joinToString("\n"), size = 0.05f, plate = true, y = 0.78f))
            val big = renderCard(base, CardStyle(text = "x"), 4000)
            val bytes = Export.jpeg(card)
            assertTrue("size ${bytes.size}", bytes.size in 10_000..Export.MAX_BYTES)
            assertTrue(bytes[0] == 0xFF.toByte() && bytes[1] == 0xD8.toByte())
            val back = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            assertTrue(maxOf(back.width, back.height) <= Export.MAX_SIDE)
            assertTrue(Export.jpeg(big).size <= Export.MAX_BYTES)
            File(outDir, "$cat.jpg").writeBytes(bytes)
        }
    }
}
