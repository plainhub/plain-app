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
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.atomic.AtomicInteger

object ImageSearchIndexer {
    private const val BATCH_SIZE = 20
    private const val PRELOAD_BUFFER = 16
    private const val NUM_WORKERS = 4
    private const val NUM_LOADERS = 3

    @Volatile var isRunning = false; private set
    @Volatile private var cancelled = false
    var totalImages = 0; private set
    var indexedImages = 0; private set

    /** Index specific images incrementally (single worker, no progress UI). */
    suspend fun indexImages(images: List<com.ismartcoding.plain.data.DImage>) = withContext(Dispatchers.IO) {
        if (images.isEmpty()) return@withContext
        val dao = AppDatabase.instance.imageEmbeddingDao()
        val modelFile = File(ImageSearchManager.getModelDir(), "mobileclip_s2_image.tflite")
        val worker = ImageEmbedWorker(modelFile)
        try {
            val batch = mutableListOf<DImageEmbedding>()
            for (image in images) {
                val bmp = ImageEmbedWorker.loadBitmap(image.path) ?: continue
                val embedding = worker.embedBitmap(bmp) ?: continue
                batch.add(DImageEmbedding(image.id, image.path, floatsToBytes(embedding)))
                if (batch.size >= BATCH_SIZE) {
                    dao.insertAll(batch)
                    batch.clear()
                }
            }
            if (batch.isNotEmpty()) dao.insertAll(batch)
        } finally {
            worker.close()
        }
    }

    /** Full scan with progress tracking (parallel workers). */
    suspend fun start(forceReindex: Boolean = false) = withContext(Dispatchers.IO) {
        if (isRunning) return@withContext
        if (ImageSearchManager.status.value != ImageSearchStatus.READY) return@withContext
        isRunning = true
        cancelled = false
        try {
            val context = MainApp.instance
            if (!Permission.WRITE_EXTERNAL_STORAGE.can(context)) return@withContext
            val allImages = ImageMediaStoreHelper.searchAsync(
                context, "", Int.MAX_VALUE, 0, FileSortBy.DATE_DESC,
            )
            totalImages = allImages.size
            val dao = AppDatabase.instance.imageEmbeddingDao()
            if (forceReindex) dao.deleteAll()
            val existingIds = dao.getAllIds().toSet()

            val currentIds = allImages.map { it.id }.toSet()
            val staleIds = existingIds - currentIds
            if (staleIds.isNotEmpty()) dao.deleteByIds(staleIds.toList())

            val toIndex = allImages.filter { it.id !in existingIds }
            indexedImages = totalImages - toIndex.size
            emitProgress()

            indexWithParallelWorkers(toIndex, dao)
        } catch (e: Exception) {
            LogCat.e("Image indexing failed", e)
        } finally {
            isRunning = false
            emitProgress()
        }
    }

    private suspend fun indexWithParallelWorkers(
        toIndex: List<com.ismartcoding.plain.data.DImage>,
        dao: com.ismartcoding.plain.db.ImageEmbeddingDao,
    ) = coroutineScope {
        val modelFile = File(ImageSearchManager.getModelDir(), "mobileclip_s2_image.tflite")
        val imageCh = Channel<com.ismartcoding.plain.data.DImage>(PRELOAD_BUFFER)
        val bitmapCh = Channel<Triple<String, String, Bitmap>>(PRELOAD_BUFFER)
        val resultCh = Channel<DImageEmbedding>(BATCH_SIZE * 2)
        val indexed = AtomicInteger(indexedImages)

        // Dispatcher: feed images into work queue
        launch(Dispatchers.IO) {
            for (image in toIndex) {
                if (cancelled) break
                imageCh.send(image)
            }
            imageCh.close()
        }

        // Parallel bitmap loaders
        val loaderJobs = (0 until NUM_LOADERS).map {
            launch(Dispatchers.IO) {
                for (image in imageCh) {
                    if (cancelled) break
                    val bmp = ImageEmbedWorker.loadBitmap(image.path) ?: continue
                    bitmapCh.send(Triple(image.id, image.path, bmp))
                }
            }
        }
        launch { loaderJobs.joinAll(); bitmapCh.close() }

        // Parallel inference workers, each with own model + buffers
        val workers = (0 until NUM_WORKERS).map { ImageEmbedWorker(modelFile) }
        val workerJobs = workers.map { worker ->
            launch(Dispatchers.Default) {
                for ((id, path, bmp) in bitmapCh) {
                    if (cancelled) { bmp.recycle(); break }
                    val embedding = worker.embedBitmap(bmp) ?: continue
                    resultCh.send(DImageEmbedding(id, path, floatsToBytes(embedding)))
                }
            }
        }
        launch { workerJobs.joinAll(); resultCh.close() }

        // Batch writer
        val batch = mutableListOf<DImageEmbedding>()
        for (item in resultCh) {
            batch.add(item)
            indexedImages = indexed.incrementAndGet()
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

        workers.forEach { it.close() }
    }

    fun cancel() { cancelled = true }

    private fun emitProgress() {
        sendEvent(ImageIndexProgressEvent(totalImages, indexedImages, isRunning))
    }
}
