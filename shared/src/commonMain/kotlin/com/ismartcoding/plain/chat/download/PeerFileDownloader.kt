package com.ismartcoding.plain.chat.download
import com.ismartcoding.plain.platform.resolveAppFilePath
import com.ismartcoding.plain.platform.importDownloadedFile
import com.ismartcoding.plain.platform.getMimeTypeFromExtension
import com.ismartcoding.plain.platform.createDownloadTempFile
import com.ismartcoding.plain.platform.DownloadTempFileHandle

import com.ismartcoding.plain.lib.extensions.getFilenameExtension
import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.chat.peer.transport.PeerTransportRouter
import com.ismartcoding.plain.platform.AppDatabase
import com.ismartcoding.plain.db.ChatItemDataUpdate
import com.ismartcoding.plain.db.DMessageFiles
import com.ismartcoding.plain.db.DMessageImages
import com.ismartcoding.plain.helpers.TimeHelper
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.isSuccess
import io.ktor.utils.io.readAvailable
import kotlinx.coroutines.yield

object PeerFileDownloader {
    suspend fun downloadAsync(
        task: DownloadTask,
    ): String? {
        val messageFile = task.messageFile
        val fileName = messageFile.fileName
        val fileId = messageFile.parseFileId()

        val tempFile = createDownloadTempFile(task.messageFile.id)

        try {
            task.status = DownloadStatus.DOWNLOADING
            task.downloadedSize = 0

            var downloadedBytes = 0L
            val downloaded = PeerTransportRouter.downloadFile(task.peer, fileId)

            downloaded.use { d ->
                val response = d.response
                if (!response.status.isSuccess()) {
                    val error = "HTTP ${response.status.value}"
                    LogCat.e("HTTP request failed: $error")
                    task.status = DownloadStatus.FAILED
                    task.error = error
                    return null
                }

                val channel = response.bodyAsChannel()
                val buffer = ByteArray(8192)
                var lastProgressUpdate = TimeHelper.nowMillis()
                var lastDownloadedSize = 0L

                while (task.status == DownloadStatus.DOWNLOADING) {
                    val bytesRead = channel.readAvailable(buffer)
                    if (bytesRead == -1) break

                    tempFile.write(buffer, 0, bytesRead)
                    downloadedBytes += bytesRead
                    val now = TimeHelper.nowMillis()

                    task.downloadedSize = downloadedBytes
                    if (now - lastProgressUpdate > 1000) {
                        val downloadedSinceLast = downloadedBytes - lastDownloadedSize
                        val timeElapsed = (now - lastProgressUpdate) / 1000.0
                        task.downloadSpeed = (downloadedSinceLast / timeElapsed).toLong()
                        task.lastDownloadedSize = downloadedBytes
                        task.lastUpdateTime = now
                        lastProgressUpdate = now
                        lastDownloadedSize = downloadedBytes
                        DownloadQueue.notifyProgressUpdate()
                    }
                    yield()
                }
            }

            tempFile.close()

            if (downloadedBytes == messageFile.size) {
                task.status = DownloadStatus.COMPLETED
                task.downloadedSize = downloadedBytes
                task.downloadSpeed = 0

                val mimeType = getMimeTypeFromExtension(fileName.getFilenameExtension())

                val fidUri = importDownloadedFile(tempFile, mimeType)
                val realPath = resolveAppFilePath(fidUri)

                updateMessageFileUri(task.messageId, messageFile.uri, fidUri)
                return realPath
            } else {
                if (!task.aborted) {
                    task.status = DownloadStatus.FAILED
                    task.error = "Incomplete download"
                }
                tempFile.delete()
                return null
            }
        } catch (ex: Exception) {
            LogCat.e("Download failed: ${ex.message}")
            task.status = DownloadStatus.FAILED
            task.error = ex.message ?: "Download failed"
            tempFile.delete()
            return null
        }
    }

    private suspend fun updateMessageFileUri(messageId: String, originalUri: String, newUri: String) {
        val message = AppDatabase.instance.chatDao().getById(messageId) ?: return
        val content = message.content

        when (content.value) {
            is DMessageFiles -> {
                val files = content.value as DMessageFiles
                val updatedFiles = files.items.map { file ->
                    if (file.uri == originalUri) file.copy(uri = newUri) else file
                }
                content.value = DMessageFiles(updatedFiles)
            }

            is DMessageImages -> {
                val images = content.value as DMessageImages
                val updatedImages = images.items.map { image ->
                    if (image.uri == originalUri) image.copy(uri = newUri) else image
                }
                content.value = DMessageImages(updatedImages)
            }
        }

        AppDatabase.instance.chatDao().updateData(ChatItemDataUpdate(messageId, content))
        LogCat.d("PeerFileDownloader: updated URI $originalUri -> $newUri")
    }
}
