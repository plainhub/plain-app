package com.ismartcoding.plain.tests

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ismartcoding.plain.appContext
import com.ismartcoding.plain.platform.getThumbnailBytes
import com.ismartcoding.plain.platform.getThumbnailResponse
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class ThumbnailRustHttpTest {
    @Test
    fun actualOsDecodeUsesRootCacheCoalescingCropAndFreshSourceValidators() = runBlocking {
        val source = File(appContext.cacheDir, "synthetic-thumbnail-${UUID.randomUUID()}.png")
        val cache = File(appContext.cacheDir, "thumbs/rust")
        val existing = cache.listFiles().orEmpty().map { it.name }.toSet()
        fun write(color: Int) {
            val bitmap = Bitmap.createBitmap(40, 20, Bitmap.Config.ARGB_8888)
            bitmap.eraseColor(color)
            source.outputStream().use { assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }
            bitmap.recycle()
        }
        suspend fun thumbnail(crop: Boolean) = getThumbnailBytes(source.absolutePath, 16, 16, crop, "", "")!!
        try {
            write(android.graphics.Color.RED)
            val first = withTimeout(30_000) { coroutineScope { (1..8).map { async(Dispatchers.Default) { thumbnail(true) } }.awaitAll() } }
            first.forEach { assertArrayEquals(first.first(), it) }
            assertEquals(1, cache.listFiles().orEmpty().count { it.name !in existing })
            BitmapFactory.decodeByteArray(first.first(), 0, first.first().size).let { bitmap ->
                assertEquals(16, bitmap.width); assertEquals(16, bitmap.height); bitmap.recycle()
            }
            val fit = thumbnail(false)
            BitmapFactory.decodeByteArray(fit, 0, fit.size).let { bitmap ->
                assertEquals(16, bitmap.width); assertEquals(8, bitmap.height); bitmap.recycle()
            }
            val old = getThumbnailResponse(source.absolutePath, 16, 16, true, "", "").use { assertEquals("image/jpeg", it.header("Content-Type")); it.header("ETag")!! }
            write(android.graphics.Color.BLUE)
            assertTrue(source.setLastModified(System.currentTimeMillis() + 2000))
            val newer = thumbnail(true)
            assertFalse(first.first().contentEquals(newer))
            val changed = getThumbnailResponse(source.absolutePath, 16, 16, true, "", "").use { it.header("ETag")!! }
            assertNotEquals(old, changed)
            val invalid = File(appContext.cacheDir, "synthetic-thumbnail-invalid-${UUID.randomUUID()}.bin")
            try {
                invalid.writeText("not an image")
                assertNull(getThumbnailBytes(invalid.absolutePath, 16, 16, true, "", ""))
            } finally { invalid.delete() }
            source.delete()
            assertNull(getThumbnailBytes(source.absolutePath, 16, 16, true, "", ""))
            assertTrue(cache.listFiles().orEmpty().none { it.extension == "tmp" })
        } finally {
            source.delete()
            cache.listFiles().orEmpty().filter { it.name !in existing }.forEach { it.delete() }
        }
    }
    @Test
    fun publicEncryptedFidThumbnailAndConditionalRequestUseRootBytesAndValidators() = runBlocking {
        val prefs = com.ismartcoding.plain.preferences.UserPrefs
        val oldService = prefs.service.value
        val oldHttp = prefs.httpPort.value
        val oldHttps = prefs.httpsPort.value
        val cache = File(appContext.cacheDir, "thumbs/rust")
        val existing = cache.listFiles().orEmpty().map { it.name }.toSet()
        var id: String? = null
        try {
            val bitmap = Bitmap.createBitmap(40, 20, Bitmap.Config.ARGB_8888)
            bitmap.eraseColor(android.graphics.Color.GREEN)
            val bytes = java.io.ByteArrayOutputStream()
            assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, bytes))
            bitmap.recycle()
            val file = com.ismartcoding.plain.helpers.AppFileStore.importBytes(bytes.toByteArray(), "image/png")
            id = file.id
            prefs.service.value = true
            com.ismartcoding.plain.platform.stopHttpEngineAsync()
            prefs.httpPort.value = 0
            prefs.httpsPort.value = 0
            com.ismartcoding.plain.platform.startHttpEngineAsync()
            assertTrue(com.ismartcoding.plain.platform.checkHttpServerAsync())
            val encoded = com.ismartcoding.plain.helpers.UrlHelper.encrypt("fid:${file.realPath.substringAfterLast('/')}")
            val url = "http://127.0.0.1:${prefs.httpPort.value}/fs?id=${android.net.Uri.encode(encoded)}&w=16&h=16"
            val first = java.net.URL(url).openConnection() as java.net.HttpURLConnection
            first.connectTimeout = 10000; first.readTimeout = 30000
            assertEquals(200, first.responseCode)
            val etag = first.getHeaderField("ETag")!!
            assertEquals("image/jpeg", first.getHeaderField("Content-Type"))
            val body = first.inputStream.use { it.readBytes() }
            first.disconnect()
            BitmapFactory.decodeByteArray(body, 0, body.size).let { image -> assertEquals(16, image.width); assertEquals(16, image.height); image.recycle() }
            val repeat = java.net.URL(url).openConnection() as java.net.HttpURLConnection
            repeat.setRequestProperty("If-None-Match", etag)
            assertEquals(304, repeat.responseCode)
            assertEquals(etag, repeat.getHeaderField("ETag"))
            repeat.disconnect()
            val fit = java.net.URL("$url&cc=false").openConnection() as java.net.HttpURLConnection
            fit.setRequestProperty("If-None-Match", etag)
            assertEquals(200, fit.responseCode)
            assertNotEquals(etag, fit.getHeaderField("ETag"))
            fit.inputStream.close(); fit.disconnect()
        } finally {
            com.ismartcoding.plain.platform.stopHttpEngineAsync()
            id?.let { com.ismartcoding.plain.helpers.AppFileStore.release(it) }
            prefs.httpPort.value = oldHttp
            prefs.httpsPort.value = oldHttps
            prefs.service.value = oldService
            cache.listFiles().orEmpty().filter { it.name !in existing }.forEach { it.delete() }
        }
    }

}
