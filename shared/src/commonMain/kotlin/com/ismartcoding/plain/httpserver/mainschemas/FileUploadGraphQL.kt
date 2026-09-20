package com.ismartcoding.plain.httpserver.mainschemas

import com.ismartcoding.plain.events.EventType
import com.ismartcoding.plain.events.UploadMergeResultData
import com.ismartcoding.plain.events.WebSocketEvent
import com.ismartcoding.plain.lib.ChannelScope
import com.ismartcoding.plain.lib.JsonHelper
import com.ismartcoding.plain.lib.kgraphql.GraphQLError
import com.ismartcoding.plain.lib.kgraphql.annotations.GraphQLMutation
import com.ismartcoding.plain.lib.kgraphql.annotations.GraphQLQuery
import com.ismartcoding.plain.lib.kgraphql.schema.dsl.SchemaBuilder
import com.ismartcoding.plain.lib.sendEvent
import com.ismartcoding.plain.httpserver.models.ID
import com.ismartcoding.plain.httpserver.models.MergeTask
import com.ismartcoding.plain.httpserver.models.MergeTaskStatus
import com.ismartcoding.plain.platform.deleteUploadedChunks
import com.ismartcoding.plain.platform.listUploadedChunks
import com.ismartcoding.plain.platform.mergeUploadedChunks
import kotlinx.coroutines.launch

@GraphQLQuery
suspend fun uploadedChunks(fileId: ID): List<String> {
    return listUploadedChunks(fileId.value)
}

@GraphQLMutation
suspend fun deleteChunks(fileId: ID): Boolean {
    return deleteUploadedChunks(fileId.value)
}

@GraphQLQuery
suspend fun mergeStatus(fileId: ID): MergeTask {
    return MergeJobs.status(fileId.value)
}

/**
 * Start merging in the background and return immediately with "started",
 * "merging" (already in flight) or "done:{value}:{size}" (already finished).
 * Completion is signalled by WS event 38; `mergeStatus` is the polling
 * fallback for lost events.
 */
@GraphQLMutation(description = "Start a background merge of the uploaded chunks into the file at `path`; completion arrives via the upload_merge_result WS event, `mergeStatus` is the polling fallback. `replace=false` keeps the existing file and writes to a new sibling path instead; the summed chunk sizes must match `totalSize`.")
suspend fun mergeChunks(fileId: ID, totalChunks: Int, path: String, replace: Boolean, totalSize: Long): MergeTask =
    mergeChunksAsyncImpl(fileId.value) { mergeUploadedChunks(fileId.value, totalChunks, path, replace, isAppFile = false, totalSize) }

@GraphQLMutation(description = "Background merge into the app-private content store; `fileName` is a name hint (no directory), always overwrites. Returns a MergeTask — poll mergeStatus or wait for the upload_merge_result WS event.")
suspend fun mergeAppFileChunks(fileId: ID, totalChunks: Int, fileName: String, totalSize: Long): MergeTask =
    mergeChunksAsyncImpl(fileId.value) { mergeUploadedChunks(fileId.value, totalChunks, fileName, replace = true, isAppFile = true, totalSize) }

private suspend fun mergeChunksAsyncImpl(fileId: String, merge: suspend () -> String): MergeTask {
    when (val claim = MergeJobs.claim(fileId)) {
        is MergeClaim.AlreadyDone -> return claim.task
        MergeClaim.InProgress -> return MergeTask(MergeTaskStatus.MERGING)
        MergeClaim.Claimed -> {}
    }
    if (listUploadedChunks(fileId).isEmpty()) {
        MergeJobs.release(fileId)
        throw GraphQLError("No chunks found for $fileId")
    }
    ChannelScope().launch {
        val outcome = runCatching { merge() }
        if (outcome.isSuccess) {
            val reply = outcome.getOrThrow()
            val idx = reply.lastIndexOf(':')
            val value = if (idx > 0) reply.substring(0, idx) else reply
            val size = if (idx > 0) reply.substring(idx + 1).toLongOrNull() ?: 0L else 0L
            MergeJobs.finish(fileId, value, size)
            sendEvent(WebSocketEvent(EventType.UPLOAD_MERGE_RESULT, JsonHelper.jsonEncode(UploadMergeResultData(fileId = fileId, ok = true, value = value, mergedSize = size))))
        } else {
            val message = outcome.exceptionOrNull()?.message ?: "merge failed"
            MergeJobs.fail(fileId, message)
            sendEvent(WebSocketEvent(EventType.UPLOAD_MERGE_RESULT, JsonHelper.jsonEncode(UploadMergeResultData(fileId = fileId, ok = false, error = message))))
        }
    }
    return MergeTask(MergeTaskStatus.STARTED)
}

fun SchemaBuilder.addFileUploadSchema() {
}
