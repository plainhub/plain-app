package com.ismartcoding.plain.features.share

import com.ismartcoding.plain.api.RustContentApi
import com.ismartcoding.plain.features.download.*
import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.platform.getDownloadsDirPath
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.*

object SharedFolderDownloadEngine {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val intents = Channel<JsonObject>(128)
    private val stateLock = Mutex()
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private var revision = -1L

    init {
        scope.launch {
            for (body in intents) {
                try { call(body); refresh() }
                catch (cancelled: CancellationException) { throw cancelled }
                catch (error: Exception) { LogCat.e("Shared download request", error) }
            }
        }
    }
    fun enqueueFile(messageId: String, link: SharedLink, urlToken: String, entry: SharedFileDto, targetDir: String) =
        enqueue(messageId, ShareBatchType.FILE, link, urlToken, listOf(entry), targetDir)
    fun enqueueDirSync(messageId: String, link: SharedLink, urlToken: String, entry: SharedFileDto, targetDir: String) =
        enqueue(messageId, ShareBatchType.SYNC, link, urlToken, listOf(entry), targetDir)
    fun enqueueMulti(messageId: String, link: SharedLink, urlToken: String, entries: List<SharedFileDto>, targetDir: String) =
        enqueue(messageId, ShareBatchType.MULTI, link, urlToken, entries, targetDir)
    fun enqueueZip(messageId: String, link: SharedLink, urlToken: String, entries: List<SharedFileDto>, zipName: String) =
        enqueue(messageId, ShareBatchType.ZIP, link, urlToken, entries, "", zipName)
    private fun enqueue(messageId: String, type: ShareBatchType, link: SharedLink, urlToken: String, entries: List<SharedFileDto>, targetDir: String, zipName: String = "") =
        submit(buildJsonObject {
            put("action", "enqueue")
            put("intent", buildJsonObject {
                put("message_id", messageId); put("kind", type.name); put("link", json.encodeToJsonElement(link))
                put("url_token", urlToken); put("entries", json.encodeToJsonElement(entries)); put("target_dir", targetDir)
                put("downloads_base", "${getDownloadsDirPath().trimEnd('/')}/PlainApp"); put("zip_name", zipName)
            })
        })
    fun retryFailed(taskId: String) { control(taskId, "retry") }
    fun control(id: String, command: String): Boolean = submit(buildJsonObject {
        put("action", "control"); put("id", id); put("command", command)
    })
    private fun submit(body: JsonObject): Boolean = intents.trySend(body).isSuccess.also {
        if (!it) LogCat.e("Shared download request capacity exceeded")
    }
    suspend fun refresh() = stateLock.withLock {
        val snapshot = call(buildJsonObject { put("action", "snapshot") }).jsonObject
        val current = snapshot.getValue("revision").jsonPrimitive.long
        if (current <= revision) return@withLock
        revision = current
        val tasks = snapshot.getValue("tasks").jsonArray.map { value ->
            val row = value.jsonObject
            fun str(key: String) = row.getValue(key).jsonPrimitive.content
            fun number(key: String) = row.getValue(key).jsonPrimitive.long
            SharedFolderBatchTask(
                str("id"), str("messageId"), ShareBatchType.valueOf(str("type")), str("title"), str("targetDir"),
                json.decodeFromJsonElement(row.getValue("link")), str("urlToken"), json.decodeFromJsonElement(row.getValue("entries")), str("zipName"),
            ).apply {
                status = DownloadStatus.valueOf(str("status")); error = str("error")
                downloadedSize = number("downloadedSize"); totalSize = number("totalSize"); downloadSpeed = number("downloadSpeed")
                totalFiles = number("totalFiles").toInt(); doneFiles = number("doneFiles").toInt(); failedFiles = number("failedFiles").toInt()
                currentFile = str("currentFile"); packing = row.getValue("packing").jsonPrimitive.boolean
                failures = row.getValue("failures").jsonArray.map { item ->
                    DownloadFailure(item.jsonObject.getValue("path").jsonPrimitive.content, item.jsonObject.getValue("error").jsonPrimitive.content)
                }.toMutableList()
            }
        }
        DownloadCenter.replaceExternal(DOWNLOAD_KIND_SHARE, tasks)
    }
    private suspend fun call(body: JsonObject): JsonElement = RustContentApi.postJson("shares/batch", body, longRunning = true).getValue("result")
}
