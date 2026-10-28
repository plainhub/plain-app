package com.ismartcoding.plain.features.chat.download

import com.ismartcoding.plain.features.chat.models.MessageFile
import com.ismartcoding.plain.features.downloads.DOWNLOAD_KIND_CHAT
import com.ismartcoding.plain.features.downloads.DownloadCenter
import com.ismartcoding.plain.features.downloads.DownloadQueueVersion
import com.ismartcoding.plain.graphql.Mutation
import com.ismartcoding.plain.graphql.Query
import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.models.Peer
import com.ismartcoding.plain.rust.graphql.GraphqlApi
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

object DownloadQueue {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val intents = Channel<Request>(128)
    private val stateLock = Mutex()
    private val version = DownloadQueueVersion()
    val downloadProgress = MutableStateFlow<Map<String, DownloadTask>>(emptyMap())

    init {
        scope.launch {
            for (body in intents) {
                try {
                    call(body)
                    refresh()
                } catch (cancelled: CancellationException) { throw cancelled }
                catch (error: Exception) { LogCat.e("Rust download request", error) }
            }
        }
    }

    fun addDownloadTask(messageFile: MessageFile, peer: Peer, messageId: String): String {
        submit(Request.Enqueue(messageId, messageFile.id, peer.id))
        return messageFile.id
    }

    fun pauseDownload(taskId: String): Boolean = control(taskId, ChatDownloadCommand.PAUSE)

    fun resumeDownload(taskId: String): Boolean = control(taskId, ChatDownloadCommand.RESUME)

    fun retryDownload(taskId: String): Boolean = control(taskId, ChatDownloadCommand.RETRY)

    fun removeDownload(taskId: String): Boolean = control(taskId, ChatDownloadCommand.REMOVE)

    private fun control(id: String, command: ChatDownloadCommand) = submit(Request.Control(id, command))

    private fun submit(body: Request): Boolean = intents.trySend(body).isSuccess.also { accepted ->
        if (!accepted) LogCat.e("Rust download request capacity exceeded")
    }

    suspend fun refresh() = stateLock.withLock {
        val snapshot = GraphqlApi.execute(Query.chatDownloadQueue())
        if (!version.accept(snapshot.runtimeId, snapshot.revision)) return@withLock
        val tasks = snapshot.tasks.map { row ->
            DownloadTask(
                messageFile = row.file,
                peer = row.peer,
                messageId = row.messageId,
                status = row.status,
                error = row.error,
                downloadedSize = row.downloaded,
                downloadSpeed = row.speed,
            )
        }.associateBy { it.id }
        downloadProgress.value = tasks
        DownloadCenter.replaceExternal(DOWNLOAD_KIND_CHAT, tasks.values.toList())
    }

    private suspend fun call(request: Request) {
        val operation = when (request) {
            is Request.Enqueue -> Mutation.enqueueChatDownload(request.messageId, request.fileId, request.peerId)
            is Request.Control -> Mutation.chatDownloadControl(request.command, request.id)
        }
        GraphqlApi.execute(operation)
    }

    private sealed interface Request {
        data class Enqueue(
            val messageId: String,
            val fileId: String,
            val peerId: String,
        ) : Request
        data class Control(
            val id: String,
            val command: ChatDownloadCommand,
        ) : Request
    }
}
