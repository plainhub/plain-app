package com.ismartcoding.plain.features.shares

import com.ismartcoding.plain.features.downloads.*
import com.ismartcoding.plain.graphql.Mutation
import com.ismartcoding.plain.graphql.Query
import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.rust.graphql.GraphqlApi
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable

object SharedFolderDownloadEngine : SharedFolderDownloadClient(
    publish = { tasks -> DownloadCenter.replaceExternal(DOWNLOAD_KIND_SHARE, tasks.values.toList()) },
)

open class SharedFolderDownloadClient internal constructor(
    private val api: com.ismartcoding.plain.rust.graphql.GraphqlClient = GraphqlApi,
    scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
    private val publish: (Map<String, SharedFolderBatchTask>) -> Unit = {},
) {
    private val _progress = MutableStateFlow<Map<String, SharedFolderBatchTask>>(emptyMap())
    val progress = _progress.asStateFlow()
    private val _errors = MutableStateFlow<Exception?>(null)
    val errors = _errors.asStateFlow()
    private val intents = Channel<Request>(128)
    private val stateLock = Mutex()
    private val version = DownloadQueueVersion()

    init {
        scope.launch {
            for (body in intents) {
                try { call(body); refresh() }
                catch (cancelled: CancellationException) { throw cancelled }
                catch (error: Exception) {
                    _errors.value = error
                    LogCat.e("Shared download request", error)
                }
            }
        }.invokeOnCompletion { intents.close() }
    }

    fun enqueueFile(messageId: String, link: SharedLink, urlToken: String, entry: SharedFileDto, targetDir: String) =
        enqueue(messageId, ShareBatchType.FILE, link, urlToken, listOf(entry), targetDir)

    fun enqueueDirSync(messageId: String, link: SharedLink, urlToken: String, entry: SharedFileDto, targetDir: String) =
        enqueue(messageId, ShareBatchType.SYNC, link, urlToken, listOf(entry), targetDir)

    fun enqueueMulti(
        messageId: String,
        link: SharedLink,
        urlToken: String,
        entries: List<SharedFileDto>,
        targetDir: String,
    ) =
        enqueue(messageId, ShareBatchType.MULTI, link, urlToken, entries, targetDir)

    fun enqueueZip(
        messageId: String,
        link: SharedLink,
        urlToken: String,
        entries: List<SharedFileDto>,
        zipName: String,
    ) =
        enqueue(messageId, ShareBatchType.ZIP, link, urlToken, entries, "", zipName)

    private fun enqueue(
        messageId: String,
        type: ShareBatchType,
        link: SharedLink,
        urlToken: String,
        entries: List<SharedFileDto>,
        targetDir: String,
        zipName: String = "",
    ) =
        submit(Request.Enqueue(SharedDownloadInput(messageId, type, link, urlToken, entries, targetDir, zipName)))

    fun retryFailed(taskId: String) {
        control(taskId, SharedDownloadCommand.RETRY)
    }

    fun control(id: String, command: SharedDownloadCommand): Boolean = submit(Request.Control(id, command))

    private fun submit(request: Request): Boolean = intents.trySend(request).isSuccess.also {
        if (!it) {
            val error = IllegalStateException("Shared download request queue unavailable")
            _errors.value = error
            LogCat.e(error.message.orEmpty())
        }
    }

    suspend fun refresh() = stateLock.withLock {
        val snapshot = api.execute(Query.sharedDownloadQueue())
        if (!version.accept(snapshot.runtimeId, snapshot.revision)) return@withLock
        val tasks = snapshot.tasks.map { row ->
            SharedFolderBatchTask(
                row.id, row.messageId, row.type, row.title, row.targetDir,
                row.link, row.urlToken, row.entries, row.zipName,
            ).apply {
                status = row.status
                error = row.error
                downloadedSize = row.downloadedSize
                totalSize = row.totalSize
                downloadSpeed = row.downloadSpeed
                totalFiles = row.totalFiles.toInt()
                doneFiles = row.doneFiles.toInt()
                failedFiles = row.failedFiles.toInt()
                currentFile = row.currentFile
                packing = row.packing
                failures = row.failures.map { DownloadFailure(it.path, it.error) }.toMutableList()
            }
        }
        val next = tasks.associateBy { it.id }
        _progress.value = next
        publish(next)
    }

    private suspend fun call(request: Request) {
        val operation = when (request) {
            is Request.Enqueue -> Mutation.enqueueSharedDownload(request.input)
            is Request.Control -> Mutation.sharedDownloadControl(request.command, request.id)
        }
        api.execute(operation)
    }

    private sealed interface Request {
        data class Enqueue(val input: SharedDownloadInput) : Request
        data class Control(
            val id: String,
            val command: SharedDownloadCommand,
        ) : Request
    }
}

@Serializable
internal data class SharedDownloadInput(
    val messageId: String,
    val kind: ShareBatchType,
    val link: SharedLink,
    val urlToken: String,
    val entries: List<SharedFileDto>,
    val targetDir: String,
    val zipName: String,
)
