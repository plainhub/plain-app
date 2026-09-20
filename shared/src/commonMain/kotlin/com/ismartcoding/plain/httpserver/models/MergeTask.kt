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
    @GraphQLField(description = "Path of the merged file once status is DONE.")
    val value: String? = null,
    val mergedSize: Long? = null,
    @GraphQLField(description = "Failure reason when status is FAILED.")
    val error: String? = null,
)
