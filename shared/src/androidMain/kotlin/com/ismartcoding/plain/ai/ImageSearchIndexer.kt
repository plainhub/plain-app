package com.ismartcoding.plain.ai

import com.ismartcoding.plain.api.string
import com.ismartcoding.plain.data.DImage
import com.ismartcoding.plain.features.imageindex.ImageIndexHelper
import com.ismartcoding.plain.lib.coIO
import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.lib.sendEvent
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.*

object ImageSearchIndexer {
    @Volatile var isRunning = false; private set
    @Volatile var totalImages = 0; private set
    @Volatile var indexedImages = 0; private set
    private var version = -1L

    suspend fun start(forceReindex: Boolean = false) { applyStatus(ImageIndexHelper.start(forceReindex)) }
    suspend fun indexImages(images: List<DImage>) { applyStatus(ImageIndexHelper.selected(images.map { it.id })) }
    fun cancel() {
        coIO {
            try { applyStatus(ImageIndexHelper.cancel()) }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) { ImageSearchManager.setIndexError(error.message ?: "Image index cancellation failed");LogCat.e("Image index",error) }
        }
    }
    @Synchronized
    fun applyStatus(status: JsonObject) {
        val next = status.getValue("version").jsonPrimitive.long
        if (next < version) return
        version = next
        isRunning = status.getValue("isRunning").jsonPrimitive.boolean
        totalImages = status.getValue("totalImages").jsonPrimitive.int
        indexedImages = status.getValue("indexedImages").jsonPrimitive.int
        sendEvent(ImageIndexProgressEvent(totalImages,indexedImages,isRunning))
    }
}
