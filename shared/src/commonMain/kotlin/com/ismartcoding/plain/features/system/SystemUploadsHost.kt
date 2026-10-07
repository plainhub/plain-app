package com.ismartcoding.plain.features.system

import com.ismartcoding.plain.httpserver.MergeClaim
import com.ismartcoding.plain.httpserver.MergeJobs
import com.ismartcoding.plain.lib.JsonHelper
import com.ismartcoding.plain.lib.sendEvent
import kotlinx.coroutines.launch
import kotlinx.serialization.json.*

internal object SystemUploadsHost {
    suspend fun handle(method: String, params: JsonObject): JsonElement = when (method) {
        "uploadTmpDirFacts" -> JsonHelper.jsonEncodeToElement(PathFacts(
            path = com.ismartcoding.plain.platform.getUploadTmpDirPath(),
        ))
        "systemUploadedChunkFacts" -> JsonHelper.jsonEncodeToElement(UploadedChunksFacts(
            chunks = com.ismartcoding.plain.platform.listUploadedChunks(
                params.getValue("fileId").jsonPrimitive.content),
        ))
        "systemMergeStatusFacts" -> mergeStatusFacts(params.getValue("fileId").jsonPrimitive.content)
        "systemDeleteChunks" -> JsonHelper.jsonEncodeToElement(com.ismartcoding.plain.platform.deleteUploadedChunks(
            params.getValue("fileId").jsonPrimitive.content))
        "systemMergeChunks" -> startMerge(params, isAppFile = false)
        "systemMergeAppFileChunks" -> startMerge(params, isAppFile = true)
        else -> error("Unsupported provider operation")
    }

    private suspend fun mergeStatusFacts(fileId: String): JsonElement {
        val task = MergeJobs.status(fileId)
        return JsonHelper.jsonEncodeToElement(MergeStatusFacts(
            status = task.status.name,
            value = task.value,
            mergedSize = task.mergedSize,
            error = task.error,
        ))
    }

    /** Starts the merge and returns immediately. The claim makes a repeated
     * call idempotent, and the job table is what `mergeStatus` polls when the
     * websocket result is lost. */
    private suspend fun startMerge(params: JsonObject, isAppFile: Boolean): JsonElement {
        val fileId = params.getValue("fileId").jsonPrimitive.content
        val totalChunks = params.getValue("totalChunks").jsonPrimitive.int
        val totalSize = params.getValue("totalSize").jsonPrimitive.long
        when (val claim = MergeJobs.claim(fileId)) {
            is MergeClaim.AlreadyDone -> return mergeStatusFacts(fileId)
            MergeClaim.InProgress -> return JsonHelper.jsonEncodeToElement(MergeStartFacts(
                status = "MERGING",
            ))
            MergeClaim.Claimed -> {}
        }
        if (com.ismartcoding.plain.platform.listUploadedChunks(fileId).isEmpty()) {
            MergeJobs.release(fileId)
            error("No chunks found for $fileId")
        }
        com.ismartcoding.plain.lib.ChannelScope().launch {
            val outcome = runCatching {
                if (isAppFile) {
                    com.ismartcoding.plain.platform.mergeUploadedChunks(
                        fileId, totalChunks,
                        params.getValue("fileName").jsonPrimitive.content,
                        replace = true, isAppFile = true, totalSize = totalSize,
                    )
                } else {
                    com.ismartcoding.plain.platform.mergeUploadedChunks(
                        fileId, totalChunks,
                        params.getValue("path").jsonPrimitive.content,
                        params.getValue("replace").jsonPrimitive.boolean,
                        isAppFile = false, totalSize = totalSize,
                    )
                }
            }
            val message = if (outcome.isSuccess) {
                val reply = outcome.getOrThrow()
                val idx = reply.lastIndexOf(':')
                val value = if (idx > 0) reply.substring(0, idx) else reply
                val size = if (idx > 0) reply.substring(idx + 1).toLongOrNull() ?: 0L else 0L
                MergeJobs.finish(fileId, value, size)
                com.ismartcoding.plain.lib.sendEvent(com.ismartcoding.plain.events.WebSocketEvent(
                    com.ismartcoding.plain.events.EventType.UPLOAD_MERGE_RESULT,
                    JsonHelper.jsonEncode(com.ismartcoding.plain.events.UploadMergeResultData(
                        fileId = fileId, ok = true, value = value, mergedSize = size)),
                ))
                null
            } else {
                val text = outcome.exceptionOrNull()?.message ?: "merge failed"
                MergeJobs.fail(fileId, text)
                com.ismartcoding.plain.lib.sendEvent(com.ismartcoding.plain.events.WebSocketEvent(
                    com.ismartcoding.plain.events.EventType.UPLOAD_MERGE_RESULT,
                    JsonHelper.jsonEncode(com.ismartcoding.plain.events.UploadMergeResultData(
                        fileId = fileId, ok = false, error = text)),
                ))
                text
            }
            if (message != null) error(message)
        }
        return JsonHelper.jsonEncodeToElement(MergeStartFacts(
            status = "STARTED",
        ))
    }
}
