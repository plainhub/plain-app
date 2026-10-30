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
@OptIn(ExperimentalUuidApi::class)
internal class UploadChunksClient(
    private val sendChunk: suspend (String, Long, ByteArray) -> String,
    private val api: GraphqlClient = GraphqlApi,
) {
    private val maxChunkBytes = 1024 * 1024
    suspend fun replace(path: String, chunks: Iterator<ByteArray>): Boolean {
        upload({ sink -> while (chunks.hasNext()) sink.write(chunks.next()) }) { id, count, size -> Mutation.mergeChunks(id, count, path, size) }
        return true
    }

    suspend fun create(path: String, produce: suspend (com.ismartcoding.plain.platform.files.StreamSink) -> Unit): String =
        requireNotNull(upload(produce) { id, count, size -> Mutation.mergeChunks(id, count, path, size, replace = false) }.value) { "Uploaded file name missing" }

    private suspend fun upload(
        produce: suspend (com.ismartcoding.plain.platform.files.StreamSink) -> Unit,
        merge: (String, Long, Long) -> com.ismartcoding.plain.rust.graphql.GraphqlRequest<com.ismartcoding.plain.models.MergeResult>,
    ): com.ismartcoding.plain.models.MergeResult {
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
            val sink = object : com.ismartcoding.plain.platform.files.StreamSink {
                override suspend fun write(bytes: ByteArray) = write(bytes, 0, bytes.size)
                override suspend fun write(bytes: ByteArray, offset: Int, length: Int) {
                    var current = offset
                    while (current < offset + length) {
                        currentCoroutineContext().ensureActive()
                        val count = minOf(maxChunkBytes - buffered, offset + length - current)
                        bytes.copyInto(buffer, buffered, current, current + count)
                        current += count
                        buffered += count
                        if (buffered == maxChunkBytes) { upload(buffer); buffered = 0 }
                    }
                }
                override suspend fun flush() {
                    if (buffered > 0) { upload(buffer.copyOf(buffered)); buffered = 0 }
                }
                override suspend fun close() = flush()
            }
            produce(sink)
            sink.flush()
            if (index == 0L) upload(ByteArray(0))
            var status = api.execute(merge(id, index, size))
            while (status.status in setOf(MergeTaskStatus.STARTED, MergeTaskStatus.MERGING)) {
                delay(100)
                status = api.execute(Query.mergeStatus(id))
            }
            check(status.status == MergeTaskStatus.DONE) {
                status.error ?: "Upload merge did not complete"
            }
            return status
        } finally {
            withContext(NonCancellable) {
                try { api.execute(Mutation.deleteChunks(id)) }
                catch (error: Exception) { LogCat.e("Upload chunk cleanup failed", error) }
            }
        }
    }
}
