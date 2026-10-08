package com.ismartcoding.plain.ai

import android.provider.MediaStore
import com.ismartcoding.plain.appContext
import com.ismartcoding.plain.features.ImageEmbeddingHelper
import com.ismartcoding.plain.features.imageindex.ImageIndexHelper
import com.ismartcoding.plain.platform.isQPlus
import com.ismartcoding.plain.lib.logcat.LogCat
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonPrimitive

object ImageIndexManager {
    private sealed class Action {
        data class Add(val ids: Set<String>) : Action()
        data class Remove(val ids: Set<String>) : Action()
        data class Scan(val force: Boolean) : Action()
    }
    private val requests = Channel<Action>(32)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val sending = Mutex()
    private var observer: ImageMediaObserver? = null
    @Volatile private var active = false
    init {
        scope.launch {
            for (request in requests) {
                sending.withLock {
                    try {
                        when (request) {
                            is Action.Add -> if (active) ImageSearchIndexer.applyStatus(ImageIndexHelper.selected(request.ids))
                            is Action.Remove -> ImageEmbeddingHelper.deleteByIds(request.ids.toList())
                            is Action.Scan -> if (active) ImageSearchIndexer.start(request.force)
                        }
                    } catch (cancelled: CancellationException) { throw cancelled }
                    catch (error: Exception) { ImageSearchManager.setIndexError(error.message ?: "Image index request failed");LogCat.e("Image index",error) }
                }
            }
        }
    }
    @Synchronized
    fun startup() {
        if (active) return
        active = true
        val uri = if (isQPlus()) MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL) else MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val next = ImageMediaObserver { enqueueSync() }
        appContext.contentResolver.registerContentObserver(uri,true,next)
        observer = next
    }
    suspend fun shutdown() {
        active = false
        observer?.let { appContext.contentResolver.unregisterContentObserver(it) }
        observer = null
        sending.withLock { while (requests.tryReceive().isSuccess) { } }
        ImageIndexCatalog.close()
    }
    fun enqueueAdd(ids: Set<String>) { if (ids.isNotEmpty() && active) send(Action.Add(ids)) }
    fun enqueueRemove(ids: Set<String>) { if (ids.isNotEmpty()) send(Action.Remove(ids)) }
    fun enqueueSync() { if (active) send(Action.Scan(false)) }
    fun fullScan(force: Boolean = false) {
        if (!active) { ImageSearchManager.setIndexError("Image search models are unavailable");return }
        send(Action.Scan(force))
    }
    private fun send(action: Action) {
        if (requests.trySend(action).isFailure) ImageSearchManager.setIndexError("Image index request capacity exceeded")
    }
}
