package com.ismartcoding.plain.chat.download

import com.ismartcoding.plain.api.RustContentApi
import com.ismartcoding.plain.chat.peer.transport.PeerTransportRouter
import com.ismartcoding.plain.features.download.DownloadStatus
import com.ismartcoding.plain.lib.TimeHelper
import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.platform.DownloadTempFileHandle
import com.ismartcoding.plain.platform.createFileWriteHandle
import io.ktor.utils.io.readAvailable
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import kotlinx.serialization.json.*

object PeerFileDownloader {
    suspend fun downloadAsync(task: DownloadTask): String? {
        var token: String? = null
        var handle: DownloadTempFileHandle? = null
        var closed = false
        try {
            val ticket = withContext(NonCancellable) {
                call(buildJsonObject {
                    put("action", "begin"); put("message_id", task.messageId)
                    put("id", task.messageFile.id); put("uri", task.messageFile.uri)
                }).jsonObject.also { token = it.getValue("token").jsonPrimitive.content }
            }
            currentCoroutineContext().ensureActive()
            val temp = createFileWriteHandle(ticket.getValue("path").jsonPrimitive.content)
            handle = temp
            task.status = DownloadStatus.DOWNLOADING
            task.downloadedSize = 0
            task.downloadSpeed = 0
            PeerTransportRouter.downloadFile(task.peer, task.messageFile.parseFileId()).use { response ->
                check(response.status in 200..299) { "HTTP ${response.status}" }
                val buffer = ByteArray(8192)
                var lastProgress = TimeHelper.nowMillis()
                var lastBytes = 0L
                while (true) {
                    currentCoroutineContext().ensureActive()
                    val read = response.channel.readAvailable(buffer)
                    if (read == -1) break
                    temp.write(buffer, 0, read)
                    task.downloadedSize += read
                    val now = TimeHelper.nowMillis()
                    if (now - lastProgress >= 1000) {
                        task.downloadSpeed = (task.downloadedSize - lastBytes) * 1000 / (now - lastProgress)
                        lastBytes = task.downloadedSize
                        lastProgress = now
                        DownloadQueue.notifyProgressUpdate()
                    }
                    yield()
                }
            }
            temp.close()
            closed = true
            currentCoroutineContext().ensureActive()
            val receipt = withContext(NonCancellable) {
                call(buildJsonObject { put("action", "finish"); put("token", checkNotNull(token)) }).jsonObject
            }
            token = null
            currentCoroutineContext().ensureActive()
            task.status = DownloadStatus.COMPLETED
            task.downloadSpeed = 0
            return receipt.getValue("path").jsonPrimitive.content
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Exception) {
            LogCat.e("Download failed: ${failure.message}")
            if (!task.aborted) {
                task.status = DownloadStatus.FAILED
                task.error = failure.message ?: "Download failed"
                task.downloadSpeed = 0
            }
            return null
        } finally {
            if (!closed) runCatching { handle?.close() }
            withContext(NonCancellable) {
                token?.let { value -> runCatching { call(buildJsonObject { put("action", "abort"); put("token", value) }) } }
            }
        }
    }
    private suspend fun call(body: JsonObject): JsonElement =
        RustContentApi.postJson("chat/attachment", body, longRunning = true).getValue("result")
}
