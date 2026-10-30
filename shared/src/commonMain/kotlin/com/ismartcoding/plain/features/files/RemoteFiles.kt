package com.ismartcoding.plain.features.files

import com.ismartcoding.plain.platform.files.downloadHttpFile
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

internal data class ResourceDownload(val id: String, val name: String, val downloadedBytes: Long, val totalBytes: Long?, val cancel: () -> Unit)

object RemoteFiles : ResourceDownloadClient(::downloadHttpFile)

open class ResourceDownloadClient internal constructor(
    private val transfer: suspend (String, String, String?, (Long, Long?) -> Unit) -> String,
) {
    private val downloads = MutableStateFlow<List<ResourceDownload>>(emptyList())
    internal val progress = downloads.asStateFlow()

    @OptIn(ExperimentalUuidApi::class)
    suspend fun download(url: String, name: String = "", tlsCertificateSha256: String? = null): String {
        val id = Uuid.random().toString()
        val job = currentCoroutineContext()[Job]
        downloads.update { it + ResourceDownload(id, name, 0, null) { job?.cancel() } }
        try {
            return transfer(url, name, tlsCertificateSha256) { bytes, total ->
                downloads.update { values -> values.map { if (it.id == id) it.copy(downloadedBytes = bytes, totalBytes = total) else it } }
            }
        } finally {
            downloads.update { it.filterNot { it.id == id } }
        }
    }
}
