package com.ismartcoding.plain.ai

import com.ismartcoding.lib.isQPlus
import com.ismartcoding.lib.logcat.LogCat
import com.ismartcoding.plain.MainApp
import com.ismartcoding.plain.db.AppDatabase
import com.ismartcoding.plain.features.Permission
import com.ismartcoding.plain.features.file.FileSortBy
import com.ismartcoding.plain.features.media.ImageMediaStoreHelper
import android.provider.MediaStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlin.coroutines.coroutineContext

/**
 * Central coordinator for AI image search indexing.
 * All index operations are queued and processed serially to prevent race conditions.
 *
 * Entry points:
 *  - [enqueueAdd]    – after upload, restore from trash
 *  - [enqueueRemove] – after delete, move to trash
 *  - [enqueueSync]   – ContentObserver change, app startup
 *  - [fullScan]      – user-triggered full rescan
 */
object ImageIndexManager {
    private const val SYNC_THRESHOLD = 50

    private sealed class Op {
        data class Add(val ids: Set<String>) : Op()
        data class Remove(val ids: Set<String>) : Op()
        data object Sync : Op()
        data class FullScan(val force: Boolean) : Op()
    }

    private val opChannel = Channel<Op>(Channel.UNLIMITED)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var processorJob: Job? = null
    private var observer: ImageMediaObserver? = null

    /** Start the processor and ContentObserver. Call when model becomes READY. */
    fun startup() {
        if (processorJob?.isActive == true) return
        processorJob = scope.launch { processOps() }
        registerObserver()
        enqueueSync()
    }

    /** Stop the processor and ContentObserver. Call when model is disabled. */
    fun shutdown() {
        unregisterObserver()
        processorJob?.cancel()
        processorJob = null
    }

    fun enqueueAdd(ids: Set<String>) {
        if (ids.isEmpty() || !ImageSearchManager.isModelReady()) return
        opChannel.trySend(Op.Add(ids))
    }

    fun enqueueRemove(ids: Set<String>) {
        if (ids.isEmpty()) return
        opChannel.trySend(Op.Remove(ids))
    }

    fun enqueueSync() {
        ensureProcessorRunning()
        opChannel.trySend(Op.Sync)
    }

    fun fullScan(force: Boolean = false) {
        ensureProcessorRunning()
        LogCat.d("ImageIndexManager: fullScan enqueued (force=$force)")
        opChannel.trySend(Op.FullScan(force))
    }

    private fun ensureProcessorRunning() {
        if (ImageSearchManager.isModelReady() && processorJob?.isActive != true) {
            processorJob = scope.launch { processOps() }
        }
    }

    private suspend fun processOps() {
        for (op in opChannel) {
            if (!ImageSearchManager.isModelReady()) continue
            try {
                LogCat.d("ImageIndexManager: processing $op")
                when (op) {
                    is Op.FullScan -> ImageSearchIndexer.start(op.force)
                    is Op.Add -> doAdd(op.ids)
                    is Op.Remove -> doRemove(op.ids)
                    is Op.Sync -> doSync()
                }
            } catch (e: Exception) {
                // Check if our own processor job was cancelled (by shutdown())
                coroutineContext.ensureActive()
                LogCat.e("ImageIndexManager op failed", e)
            }
        }
    }

    private suspend fun doAdd(ids: Set<String>) {
        val dao = AppDatabase.instance.imageEmbeddingDao()
        val existingIds = dao.getAllIds().toSet()
        val newIds = ids - existingIds
        if (newIds.isEmpty()) return

        val context = MainApp.instance
        val idsQuery = "ids:${newIds.joinToString(",")}"
        val images = ImageMediaStoreHelper.searchAsync(
            context, idsQuery, newIds.size, 0, FileSortBy.DATE_DESC,
        )
        if (images.isNotEmpty()) {
            ImageSearchIndexer.indexImages(images)
        }
    }

    private suspend fun doRemove(ids: Set<String>) {
        AppDatabase.instance.imageEmbeddingDao().deleteByIds(ids.toList())
    }

    private suspend fun doSync() {
        val context = MainApp.instance
        if (!Permission.WRITE_EXTERNAL_STORAGE.can(context)) return
        val allImages = ImageMediaStoreHelper.searchAsync(
            context, "", Int.MAX_VALUE, 0, FileSortBy.DATE_DESC,
        )
        val currentIds = allImages.map { it.id }.toSet()
        val dao = AppDatabase.instance.imageEmbeddingDao()
        val existingIds = dao.getAllIds().toSet()

        // Remove stale embeddings
        val staleIds = existingIds - currentIds
        if (staleIds.isNotEmpty()) dao.deleteByIds(staleIds.toList())

        // Index new images – delegate to FullScan for large batches
        val newImages = allImages.filter { it.id !in existingIds }
        if (newImages.size > SYNC_THRESHOLD) {
            LogCat.d("ImageIndexManager: doSync: ${newImages.size} new images, delegating to FullScan")
            opChannel.trySend(Op.FullScan(false))
        } else if (newImages.isNotEmpty()) {
            ImageSearchIndexer.indexImages(newImages)
        }
    }

    private fun registerObserver() {
        if (observer != null) return
        val obs = ImageMediaObserver { enqueueSync() }
        val uri = if (isQPlus()) {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }
        MainApp.instance.contentResolver.registerContentObserver(uri, true, obs)
        observer = obs
    }

    private fun unregisterObserver() {
        observer?.let {
            MainApp.instance.contentResolver.unregisterContentObserver(it)
        }
        observer = null
    }
}
