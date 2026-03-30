package com.ismartcoding.plain.ai

import android.graphics.Bitmap
import com.ismartcoding.lib.channel.sendEvent
import com.ismartcoding.lib.logcat.LogCat
import com.ismartcoding.plain.MainApp
import com.ismartcoding.plain.db.AppDatabase
import com.ismartcoding.plain.db.DImageEmbedding
import com.ismartcoding.plain.features.Permission
import com.ismartcoding.plain.features.file.FileSortBy
import com.ismartcoding.plain.features.media.ImageMediaStoreHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object ImageSearchIndexer {
    private const val BATCH_SIZE = 20
    private const val PRELOAD_BUFFER = 4

    @Volatile var isRunning = false; private set
    @Volatile private var cancelled = false
    var totalImages = 0; private set
    var indexedImages = 0; private set

    suspend fun start(forceReindex: Boolean = false) = withContext(Dispatchers.IO) {
        if (isRunning) return@withContext
        if (ImageSearchManager.status.value != ImageSearchStatus.READY) return@withContext
        isRunning = true
        cancelled = false
        try {
            val context = MainApp.instance
            Permission.WRITE_EXTERNAL_STORAGE.checkAsync(context)
            val allImages = ImageMediaStoreHelper.searchAsync(
                context, "", Int.MAX_VALUE, 0, FileSortBy.DATE_DESC,
            )
            totalImages = allImages.size
            val dao = AppDatabase.instance.imageEmbeddingDao()
            if (forceReindex) dao.deleteAll()
            val existingIds = dao.getAllIds().toSet()

            // Remove stale entries for deleted images
            val currentIds = allImages.map { it.id }.toSet()
            val staleIds = existingIds - currentIds
            if (staleIds.isNotEmpty()) {
                dao.deleteByIds(staleIds.toList())
            }

            val toIndex = allImages.filter { it.id !in existingIds }
            indexedImages = totalImages - toIndex.size
            emitProgress()

            indexWithPipeline(toIndex, dao)
        } catch (e: Exception) {
            LogCat.e("Image indexing failed", e)
        } finally {
            isRunning = false
            emitProgress()
        }
    }

    /**
     * Pipeline approach: preload bitmaps in a background coroutine so disk I/O
     * overlaps with model inference, roughly doubling throughput.
     */
    private suspend fun indexWithPipeline(
        toIndex: List<com.ismartcoding.plain.data.DImage>,
        dao: com.ismartcoding.plain.db.ImageEmbeddingDao,
    ) = coroutineScope {
        val ch = Channel<Triple<String, String, Bitmap>>(PRELOAD_BUFFER)

        // Producer: decode bitmaps ahead of inference
        launch(Dispatchers.IO) {
            for (image in toIndex) {
                if (cancelled) break
                val bmp = ImageEmbedHelper.loadBitmap(image.path) ?: continue
                ch.send(Triple(image.id, image.path, bmp))
            }
            ch.close()
        }

        // Consumer: run inference on pre-loaded bitmaps
        val batch = mutableListOf<DImageEmbedding>()
        for ((id, path, bmp) in ch) {
            if (cancelled) { bmp.recycle(); break }
            val embedding = ImageEmbedHelper.embedBitmap(bmp) ?: continue
            batch.add(DImageEmbedding(id, path, floatsToBytes(embedding)))
            indexedImages++
            if (batch.size >= BATCH_SIZE) {
                dao.insertAll(batch)
                batch.clear()
                emitProgress()
            }
        }
        if (batch.isNotEmpty()) {
            dao.insertAll(batch)
            batch.clear()
        }
        emitProgress()
    }

    fun cancel() { cancelled = true }

    private fun emitProgress() {
        sendEvent(ImageIndexProgressEvent(totalImages, indexedImages, isRunning))
    }
}
