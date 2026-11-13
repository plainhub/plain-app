package com.ismartcoding.plain.features.downloads

import com.ismartcoding.plain.graphql.Mutation
import com.ismartcoding.plain.graphql.Query
import com.ismartcoding.plain.models.Download
import com.ismartcoding.plain.models.DownloadInput
import com.ismartcoding.plain.rust.graphql.GraphqlApi
import com.ismartcoding.plain.rust.graphql.GraphqlClient
import com.ismartcoding.plain.lib.logcat.LogCat
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

open class DownloadsClient internal constructor(
    private val api: GraphqlClient = GraphqlApi,
    scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
) {
    private val state = MutableStateFlow<Map<String, Download>>(emptyMap())
    val progress = state.asStateFlow()
    private val failures = MutableStateFlow<Exception?>(null)
    val errors = failures.asStateFlow()
    private val refreshLock = Mutex()
    private val requests = Channel<Request>(128)

    init {
        scope.launch {
            for (request in requests) {
                try {
                    when (request) {
                        is Request.Create -> api.execute(Mutation.createDownload(request.input))
                        is Request.Control -> api.execute(Mutation.downloadControl(request.id, request.command))
                    }
                    refresh()
                } catch (cancelled: CancellationException) { throw cancelled }
                catch (error: Exception) {
                    failures.value = error
                    LogCat.e("Download request", error)
                }
            }
        }.invokeOnCompletion { requests.close() }
    }

    fun create(input: DownloadInput): Boolean = submit(Request.Create(input))
    fun control(id: String, command: DownloadCommand): Boolean = submit(Request.Control(id, command))
    fun cancel(id: String): Boolean = control(id, DownloadCommand.CANCEL)
    fun remove(id: String): Boolean = control(id, DownloadCommand.REMOVE)

    suspend fun refresh() = refreshLock.withLock {
        state.value = api.execute(Query.downloads()).associateBy { it.id }
    }

    private fun submit(request: Request): Boolean = requests.trySend(request).isSuccess.also { accepted ->
        if (!accepted) {
            val error = IllegalStateException("Download request queue unavailable")
            failures.value = error
            LogCat.e(error.message.orEmpty())
        }
    }

    private sealed interface Request {
        data class Create(val input: DownloadInput) : Request
        data class Control(val id: String, val command: DownloadCommand) : Request
    }
}

enum class DownloadCommand(val field: String) {
    PAUSE("pauseDownload"), RESUME("resumeDownload"), RETRY("retryDownload"),
    CANCEL("cancelDownload"), REMOVE("removeDownload"),
}
