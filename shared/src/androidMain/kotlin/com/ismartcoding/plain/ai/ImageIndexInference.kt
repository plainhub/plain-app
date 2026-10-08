package com.ismartcoding.plain.ai

import com.ismartcoding.plain.lib.JsonHelper
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.*
import kotlin.io.encoding.Base64
import java.util.Collections

object ImageIndexInference {
    private val lock = Mutex()
    private var jobId = ""
    private val workers = mutableListOf<ImageIndexWorker>()
    private var factory: ImageIndexWorkerFactory = PlatformImageIndexWorkerFactory
    suspend fun install(current: ImageIndexWorkerFactory): ImageIndexWorkerFactory = lock.withLock {
        check(jobId.isEmpty() && workers.isEmpty()) { "Image index engine busy" }
        val previous = factory
        factory = current
        previous
    }
    suspend fun begin(id: String): JsonObject = lock.withLock {
        check(jobId.isEmpty() || jobId == id) { "Image index engine busy" }
        check(factory.isReady()) { "Image search models are unavailable" }
        jobId = id
        ImageIndexCatalog.snapshot()
    }
    suspend fun embed(id: String, revision: String, items: JsonArray): JsonObject = lock.withLock {
        val images = JsonHelper.jsonDecodeFromElement<List<ImageCatalogItem>>(items)
        require(images.size <= 20)
        check(jobId == id) { "Image index job expired" }
        ImageIndexCatalog.verify(revision)
        if (workers.isEmpty()) {
            try {
                repeat(4) { workers.add(factory.create()) }
            } catch (error: Throwable) {
                runCatching { closeWorkers() }
                throw error
            }
        }
        val encoded = Collections.synchronizedList(mutableListOf<ImageEmbeddedItem>())
        val skipped = Collections.synchronizedList(mutableListOf<String>())
        coroutineScope {
            workers.mapIndexed { workerIndex, worker ->
                launch(Dispatchers.Default) {
                    images.filterIndexed { index, _ -> index % workers.size == workerIndex }.forEach { value ->
                        ensureActive()
                        val imageId = value.id
                        val path = value.path
                        val bitmap = ImageEmbedWorker.loadBitmap(path)
                        if (bitmap == null) {
                            skipped.add(imageId)
                        } else {
                            try {
                                ensureActive()
                                val vector = worker.embedBitmap(bitmap) ?: error("Image inference failed for $imageId")
                                encoded.add(ImageEmbeddedItem(imageId, path, Base64.encode(floatsToBytes(vector))))
                            } finally { if (!bitmap.isRecycled) bitmap.recycle() }
                        }
                    }
                }
            }.joinAll()
        }
        ImageIndexCatalog.verify(revision)
        JsonHelper.jsonEncodeToElement(ImageEmbeddedBatch(encoded.toList(), skipped.toList())).jsonObject
    }
    suspend fun end(id: String): Boolean = lock.withLock {
        if (jobId != id) return@withLock false
        closeWorkers()
        true
    }
    suspend fun disconnect() = lock.withLock { try { closeWorkers() } finally { ImageIndexCatalog.close() } }
    private fun closeWorkers() {
        val closing = workers.toList()
        workers.clear()
        jobId = ""
        var error: Throwable? = null
        closing.forEach { worker -> try { worker.close() } catch (failed: Throwable) { if (error == null) error = failed } }
        error?.let { throw it }
    }
}
