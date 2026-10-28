package com.ismartcoding.plain.features.media

import com.ismartcoding.plain.graphql.Mutation
import com.ismartcoding.plain.graphql.Query
import com.ismartcoding.plain.lib.JsonHelper
import com.ismartcoding.plain.lib.coIO
import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.models.MediaPlayHistory
import com.ismartcoding.plain.platform.system.DataType
import com.ismartcoding.plain.rust.graphql.GraphqlRequest
import com.ismartcoding.plain.rust.graphql.GraphqlApi
import kotlin.time.Instant
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job

object MediaPlayHistoryClient {
    private val lock = SynchronizedObject()
    private var pendingWrite: Job? = null

    suspend fun find(mediaId: String, type: DataType, since: Instant): MediaPlayHistory? {
        synchronized(lock) { pendingWrite }?.join()
        val query = "type:${type.name} ids:${JsonHelper.jsonEncode(mediaId)} played_at:${JsonHelper.jsonEncode(">=$since")}"
        return GraphqlApi.execute(Query.mediaPlayHistory(query, 0, 1)).singleOrNull()
    }

    fun recordPlay(mediaId: String, type: DataType, positionMs: Long): Job =
        write(Mutation.recordMediaPlay(mediaId, type, positionMs))

    fun saveProgress(mediaId: String, type: DataType, positionMs: Long): Job =
        write(Mutation.saveMediaPlayProgress(mediaId, type, positionMs))

    private fun write(request: GraphqlRequest<Unit>): Job = synchronized(lock) {
        val previousWrite = pendingWrite
        coIO {
            previousWrite?.join()
            try { GraphqlApi.execute(request) }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) { LogCat.e("Write media playback history", error) }
        }.also { pendingWrite = it }
    }
}
