package com.ismartcoding.plain.features.files.upload

import com.ismartcoding.plain.graphql.Mutation
import com.ismartcoding.plain.graphql.Query
import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.models.MergeTaskStatus
import com.ismartcoding.plain.rust.graphql.GraphqlClient
import com.ismartcoding.plain.rust.graphql.GraphqlApi
import com.ismartcoding.plain.rust.native.RustNativeBindings
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.coroutines.*

internal object RustUploadChunks {
    private val client = UploadChunksClient(
        sendChunk = { id, index, bytes -> withContext(Dispatchers.IO) { RustNativeBindings.uploadChunk(id, index, bytes) } },
    )

    suspend fun replace(path: String, chunks: Iterator<ByteArray>): Boolean = client.replace(path, chunks)
}
internal class UploadChunksClient(
    private val sendChunk: suspend (String, Long, ByteArray) -> String,
    private val api: GraphqlClient = GraphqlApi,
) {
    private val maxChunkBytes = 1024 * 1024
    @OptIn(ExperimentalUuidApi::class)

    suspend fun replace(path: String, chunks: Iterator<ByteArray>): Boolean {
        val id = Uuid.random().toString()
        var index = 0L
        var size = 0L
        try {
            suspend fun upload(bytes: ByteArray) {
                currentCoroutineContext().ensureActive()
                val error = sendChunk(id, index, bytes)
                check(error.isEmpty()) { error }
                index++
                size += bytes.size
            }
            val buffer = ByteArray(maxChunkBytes)
            var buffered = 0
            while (chunks.hasNext()) {
                val bytes = chunks.next()
                var offset = 0
                while (offset < bytes.size) {
                    currentCoroutineContext().ensureActive()
                    val length = minOf(maxChunkBytes - buffered, bytes.size - offset)
                    bytes.copyInto(buffer, buffered, offset, offset + length)
                    offset += length
                    buffered += length
                    if (buffered == maxChunkBytes) { upload(buffer); buffered = 0 }
                }
            }
            if (buffered > 0) upload(buffer.copyOf(buffered))
            if (index == 0L) upload(ByteArray(0))
            var status = api.execute(Mutation.mergeChunks(id, index, path, size))
            while (status.status in setOf(MergeTaskStatus.STARTED, MergeTaskStatus.MERGING)) {
                delay(100)
                status = api.execute(Query.mergeStatus(id))
            }
            check(status.status == MergeTaskStatus.DONE) {
                status.error ?: "Upload merge did not complete"
            }
            return true
        } finally {
            withContext(NonCancellable) {
                try { api.execute(Mutation.deleteChunks(id)) }
                catch (error: Exception) { LogCat.e("Upload chunk cleanup failed", error) }
            }
        }
    }
}
