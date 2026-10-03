package com.ismartcoding.plain.tests

import android.content.ContentValues
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ismartcoding.plain.ai.*
import com.ismartcoding.plain.appContext
import com.ismartcoding.plain.features.ImageEmbeddingHelper
import com.ismartcoding.plain.features.imageindex.ImageIndexHelper
import com.ismartcoding.plain.platform.AppDatabase
import com.ismartcoding.plain.preferences.UserPrefs
import kotlinx.coroutines.*
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger

@RunWith(AndroidJUnit4::class)
class ImageIndexRustHttpTest {
    @Test
    fun selectedNativeImagesAreDecodedAndWorkersCloseBeforeRustJobCompletes() = runBlocking {
        val restart = ImageSearchManager.isModelReady() && UserPrefs.aiImageSearchEnabled.value
        ImageIndexManager.shutdown()
        val created = AtomicInteger()
        val decoded = AtomicInteger()
        val closed = AtomicInteger()
        var failAllocation = false
        val factory = object : ImageIndexWorkerFactory {
            override fun isReady() = true
            override fun create(): ImageIndexWorker {
                val number = created.incrementAndGet()
                if (failAllocation && number == 3) error("Synthetic worker allocation failure")
                return object : ImageIndexWorker {
                    override fun embedBitmap(bitmap: Bitmap): FloatArray {
                        assertEquals(256,bitmap.width)
                        assertEquals(256,bitmap.height)
                        decoded.incrementAndGet()
                        return FloatArray(512).apply { this[0] = 1f }
                    }
                    override fun close() { closed.incrementAndGet() }
                }
            }
        }
        val previous = ImageIndexInference.install(factory)
        val images = mutableListOf<Uri>()
        fun createImage(): String {
            val resolver = appContext.contentResolver
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME,"plain-index-${UUID.randomUUID()}.png")
                put(MediaStore.Images.Media.MIME_TYPE,"image/png")
                put(MediaStore.Images.Media.RELATIVE_PATH,"Pictures/PlainSyntheticTests")
                put(MediaStore.Images.Media.IS_PENDING,1)
            }
            val uri = checkNotNull(resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,values))
            images.add(uri)
            val bitmap = Bitmap.createBitmap(256,256,Bitmap.Config.ARGB_8888)
            try {
                bitmap.eraseColor(0xff336699.toInt())
                checkNotNull(resolver.openOutputStream(uri)).use { check(bitmap.compress(Bitmap.CompressFormat.PNG,100,it)) }
            } finally { bitmap.recycle() }
            values.clear()
            values.put(MediaStore.Images.Media.IS_PENDING,0)
            assertEquals(1,resolver.update(uri,values,null,null))
            return checkNotNull(uri.lastPathSegment)
        }
        suspend fun waitForJob(): JsonObject = withTimeout(15_000) {
            while (true) {
                val status = ImageIndexHelper.status()
                if (!status.getValue("isRunning").jsonPrimitive.boolean) return@withTimeout status
                delay(50)
            }
            error("Unreachable")
        }
        try {
            val first = createImage()
            delay(150)
            ImageIndexHelper.selected(listOf(first,first))
            val done = waitForJob()
            assertEquals("",done.getValue("errorMessage").jsonPrimitive.content)
            assertEquals(4,created.get())
            assertEquals(1,decoded.get())
            assertEquals(4,closed.get())
            assertTrue(ImageEmbeddingHelper.getAllIds().contains(first))
            assertFalse(AppDatabase.instance.imageEmbeddingDao().getAllIds().contains(first))
            created.set(0); closed.set(0); failAllocation = true
            val second = createImage()
            delay(150)
            ImageIndexHelper.selected(listOf(second))
            val failed = waitForJob()
            assertTrue(failed.getValue("errorMessage").jsonPrimitive.content.contains("Synthetic worker allocation failure"))
            assertEquals(3,created.get())
            assertEquals(2,closed.get())
            assertFalse(ImageEmbeddingHelper.getAllIds().contains(second))
            assertTrue(ImageEmbeddingHelper.getAllIds().contains(first))
        } finally {
            try {
                ImageIndexHelper.cancel()
                waitForJob()
                ImageEmbeddingHelper.deleteByIds(images.map { checkNotNull(it.lastPathSegment) })
            } finally {
                try { images.forEach { appContext.contentResolver.delete(it,null,null) } }
                finally {
                    ImageIndexInference.disconnect()
                    ImageIndexInference.install(previous)
                    ImageSearchManager.setIndexError("")
                    if (restart) ImageIndexManager.startup()
                }
            }
        }
    }
}
