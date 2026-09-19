package com.ismartcoding.plain.features.download

import kotlinx.coroutines.Job

/** One failed file inside a batch: remote path plus the transfer error. */
data class DownloadFailure(val path: String, val error: String)

/** Engine kind for chat peer file transfers (see chat/download package). */
const val DOWNLOAD_KIND_CHAT = "chat"

/** Engine kind for shared-folder batch downloads (see features/share). */
const val DOWNLOAD_KIND_SHARE = "share"

/**
 * Minimal contract every download task fulfills. Concrete task classes own
 * their payload and progress fields; the center only drives the lifecycle.
 */
interface DownloadTaskHandle {
    val id: String
    val kind: String
    var status: DownloadStatus
    var error: String
    var aborted: Boolean
    var job: Job?

    /**
     * Immutable copy for state-flow emission. Implementations must copy every
     * mutable field a collector reads, because the executing coroutine keeps
     * mutating the original.
     */
    fun flowSnapshot(): DownloadTaskHandle
}

/**
 * Executes one task kind for [DownloadCenter]. Implementations stream data,
 * update progress fields on the task (calling [DownloadCenter.notifyProgressUpdate]
 * when collectors should refresh) and set a terminal status before returning.
 */
interface DownloadEngine {
    suspend fun execute(task: DownloadTaskHandle)

    /** Called once [execute] returned with a terminal status. */
    fun onFinished(task: DownloadTaskHandle) {}
}
