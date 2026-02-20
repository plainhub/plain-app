package com.ismartcoding.plain.chat

import android.content.Context
import android.webkit.MimeTypeMap
import com.ismartcoding.lib.extensions.getFilenameExtension
import com.ismartcoding.lib.logcat.LogCat
import com.ismartcoding.plain.api.HttpClientManager
import com.ismartcoding.plain.db.AppDatabase
import com.ismartcoding.plain.db.ChatItemDataUpdate
import com.ismartcoding.plain.db.DMessageFiles
import com.ismartcoding.plain.db.DMessageImages
import com.ismartcoding.plain.helpers.ChatFileSaveHelper
import com.ismartcoding.plain.helpers.AppFileStore
import okhttp3.Request
import java.io.File

object PeerFileDownloader {
    suspend fun downloadAsync(
        context: Context,
        task: DownloadTask
    ): String? {
        val messageFile = task.messageFile
        val fileName = messageFile.fileName
        val downloadUrl = task.peer.getFileUrl(messageFile.parseFileId())

        // Stage download into a temp file; imported into the content-addressable
        // store only after a successful, complete download.
        val tempFile = File(context.cacheDir, "dl_${task.messageFile.id}_${System.currentTimeMillis()}")
        tempFile.parentFile?.mkdirs()

        try {
            task.status = DownloadStatus.DOWNLOADING
            task.downloadedSize = 0

            tempFile.createNewFile()

            var downloadedBytes = 0L
            val client = HttpClientManager.createUnsafeOkHttpClient()
            task.httpClient = client

            val response = client.newCall(Request.Builder().url(downloadUrl).build()).execute()
            if (!response.isSuccessful) {
                val error = "HTTP ${response.code}"
                LogCat.e("HTTP request failed: $downloadUrl $error")
                task.status = DownloadStatus.FAILED
                task.error = error
                return null
            }

            response.body.byteStream().use { inputStream ->
                tempFile.outputStream().use { outputStream ->
                    val buffer = ByteArray(8192)
                    var lastProgressUpdate = System.currentTimeMillis()
                    var lastDownloadedSize = 0L

                    while (task.status == DownloadStatus.DOWNLOADING) {
                        val bytesRead = inputStream.read(buffer)
                        if (bytesRead == -1) break

                        outputStream.write(buffer, 0, bytesRead)
                        downloadedBytes += bytesRead
                        val now = System.currentTimeMillis()

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
                    }
                }
            }

            if (downloadedBytes == messageFile.size) {
                task.status = DownloadStatus.COMPLETED
                task.downloadedSize = downloadedBytes
                task.downloadSpeed = 0

                // Infer MIME type from file name extension
                val mimeType = MimeTypeMap.getSingleton()
                    .getMimeTypeFromExtension(fileName.getFilenameExtension()) ?: ""

                // Import into content-addressable store (dedup, deleteSrc=true moves the temp file)
                val fidUri = ChatFileSaveHelper.importDownloadedFile(context, tempFile, mimeType)
                val realPath = AppFileStore.resolveUri(context, fidUri)

                updateMessageFileUri(task.messageId, messageFile.uri, fidUri)
                return realPath
            } else {
                if (!task.aborted) {
                    task.status = DownloadStatus.FAILED
                    task.error = "Incomplete download"
                }
                tempFile.takeIf { it.exists() }?.delete()
                return null
            }
        } catch (ex: Exception) {
            LogCat.e("Download failed: ${ex.message}")
            task.status = DownloadStatus.FAILED
            task.error = ex.message ?: "Download failed"
            tempFile.takeIf { it.exists() }?.delete()
            return null
        }
    }

    private fun updateMessageFileUri(messageId: String, originalUri: String, newUri: String) {
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
