package com.ismartcoding.plain.platform

import android.media.MediaScannerConnection
import android.net.Uri
import com.ismartcoding.plain.appContext
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

actual suspend fun scanFileTaskPaths(paths: List<String>) {
    val unique = paths.distinct()
    if (unique.isEmpty()) return
    check(Permission.WRITE_EXTERNAL_STORAGE.isGranted()) { "File scan permission denied" }
    withTimeout(20_000) {
        suspendCancellableCoroutine<Unit> { continuation ->
            val remaining = AtomicInteger(unique.size)
            val completed = ConcurrentHashMap.newKeySet<String>()
            lateinit var connection: MediaScannerConnection
            connection = MediaScannerConnection(appContext,object : MediaScannerConnection.MediaScannerConnectionClient {
                override fun onMediaScannerConnected() {
                    try { unique.forEach { connection.scanFile(it,null) } }
                    catch (error: Exception) {
                        connection.disconnect()
                        if (continuation.isActive) continuation.resumeWithException(error)
                    }
                }
                override fun onScanCompleted(path: String?, uri: Uri?) {
                    if (path != null && path in unique && completed.add(path) && remaining.decrementAndGet() == 0) {
                        connection.disconnect()
                        if (continuation.isActive) continuation.resume(Unit)
                    }
                }
            })
            continuation.invokeOnCancellation { connection.disconnect() }
            connection.connect()
        }
    }
}
