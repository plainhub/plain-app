package com.ismartcoding.plain.models

import com.ismartcoding.plain.features.downloads.DownloadStatus
import kotlinx.serialization.Serializable

@Serializable
data class Download(
    val id: String,
    val title: String,
    val source: DownloadSource,
    val targetDir: String,
    val zipName: String?,
    val status: DownloadStatus,
    val downloadedBytes: Long,
    val totalBytes: Long,
    val bytesPerSecond: Long,
    val totalFiles: Int,
    val doneFiles: Int,
    val currentFile: String,
    val error: String,
    val failures: List<DownloadFailure>,
) {
    fun fraction(): Float = if (totalBytes > 0) (downloadedBytes.toFloat() / totalBytes).coerceIn(0f, 1f) else 0f
}

@Serializable
data class DownloadFailure(val path: String, val error: String)

@Serializable
data class DownloadInput(val resources: ResourceDownloadInput? = null, val chat: ChatDownloadInput? = null)

@Serializable
data class ResourceDownloadInput(val urls: List<String>, val targetDir: String, val zipName: String? = null)

@Serializable
data class ChatDownloadInput(val messageId: String, val peerId: String, val fileIds: List<String>)
