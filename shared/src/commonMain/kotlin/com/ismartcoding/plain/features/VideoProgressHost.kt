package com.ismartcoding.plain.features

import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.lib.logcat.LogCat
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days

object VideoProgressHost {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val lock = Mutex()

    suspend fun restore() {
        val rows = VideoProgressHelper.recentAsync(Clock.System.now() - 30.days)
        withContext(Dispatchers.Main) {
            rows.forEach { TempData.videoPlayProgressMap.getOrPut(it.mediaId) { it.positionMs } }
        }
    }

    fun save(mediaId: String, positionMs: Long) {
        if (mediaId.isEmpty() || positionMs < 0) return
        TempData.videoPlayProgressMap[mediaId] = positionMs
        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            lock.withLock {
                try { VideoProgressHelper.saveAsync(mediaId, positionMs) }
                catch (cancelled: CancellationException) { throw cancelled }
                catch (e: Exception) { LogCat.e("Video progress: ${e.message}") }
            }
        }
    }
}
