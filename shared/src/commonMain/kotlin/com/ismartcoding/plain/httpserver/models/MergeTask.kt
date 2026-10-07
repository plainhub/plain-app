package com.ismartcoding.plain.httpserver.models

enum class MergeTaskStatus {
    NONE,
    STARTED,
    MERGING,
    DONE,
    FAILED,
}

/** Chunked-upload merge job state; the completion signal is the
 *  upload_merge_result WS event, `mergeStatus` is the polling fallback. */
data class MergeTask(
    val status: MergeTaskStatus,
    val value: String? = null,
    val mergedSize: Long? = null,
    val error: String? = null,
)
