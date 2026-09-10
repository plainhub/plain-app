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
import com.ismartcoding.plain.platform.deleteUploadedChunks
import com.ismartcoding.plain.platform.listUploadedChunks
import com.ismartcoding.plain.platform.mergeUploadedChunks
import kotlinx.coroutines.launch

@GraphQLQuery
suspend fun uploadedChunks(fileId: String): List<String> {
    return listUploadedChunks(fileId)
}

@GraphQLMutation
suspend fun deleteChunks(fileId: String): Boolean {
    return deleteUploadedChunks(fileId)
}

@GraphQLMutation
suspend fun mergeChunks(fileId: String, totalChunks: Int, path: String, replace: Boolean, isAppFile: Boolean, totalSize: Long): String {
    return mergeUploadedChunks(fileId, totalChunks, path, replace, isAppFile, totalSize)
}

@GraphQLQuery
suspend fun mergeStatus(fileId: String): String {
    return MergeJobs.statusString(fileId)
}

/**
 * Start merging in the background and return immediately with "started",
 * "merging" (already in flight) or "done:{value}:{size}" (already finished).
 * Completion is signalled by WS event 38; `mergeStatus` is the polling
 * fallback for lost events.
 */
@GraphQLMutation
suspend fun mergeChunksAsync(fileId: String, totalChunks: Int, path: String, replace: Boolean, isAppFile: Boolean, totalSize: Long): String {
    when (val claim = MergeJobs.claim(fileId)) {
        is MergeClaim.AlreadyDone -> return claim.reply
        MergeClaim.InProgress -> return "merging"
        MergeClaim.Claimed -> {}
    }
    if (listUploadedChunks(fileId).isEmpty()) {
        MergeJobs.release(fileId)
        throw GraphQLError("No chunks found for $fileId")
    }
    ChannelScope().launch {
        val outcome = runCatching { mergeUploadedChunks(fileId, totalChunks, path, replace, isAppFile, totalSize) }
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
    return "started"
}

fun SchemaBuilder.addFileUploadSchema() {
}
