package com.ismartcoding.plain.httpserver.models

import com.ismartcoding.plain.lib.kgraphql.annotations.GraphQLField
import com.ismartcoding.plain.lib.kgraphql.annotations.GraphQLType

enum class MergeTaskStatus {
    NONE,
    STARTED,
    MERGING,
    DONE,
    FAILED,
}

/** Chunked-upload merge job state; the completion signal is the
 *  upload_merge_result WS event, `mergeStatus` is the polling fallback. */
@GraphQLType
data class MergeTask(
    val status: MergeTaskStatus,
    @GraphQLField(description = "Name of the merged result once status is DONE — the destination file's base name for mergeChunks, the content-store file name for mergeAppFileChunks; not a full path.")
    val value: String? = null,
    @GraphQLField(description = "Total size in bytes of the merged result once status is DONE; null otherwise.")
    val mergedSize: Long? = null,
    @GraphQLField(description = "Failure reason when status is FAILED.")
    val error: String? = null,
)
