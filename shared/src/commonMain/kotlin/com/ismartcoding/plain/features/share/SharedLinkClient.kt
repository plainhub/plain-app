package com.ismartcoding.plain.features.share

import com.ismartcoding.plain.api.RustContentApi
import com.ismartcoding.plain.db.DMessageShare
import com.ismartcoding.plain.lib.TimeHelper
import io.ktor.utils.io.readAvailable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.serialization.json.*

object SharedLinkClient {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    fun pageUrl(link: SharedLink): String = link.pageUrl

    suspend fun ownLink(id: String, host: String? = null): SharedLink = json.decodeFromJsonElement(call(buildJsonObject {
        put("action", "ownLink"); put("id", id); put("host", host)
    }))

    suspend fun initialLink(messageId: String, expected: DMessageShare): SharedLink = json.decodeFromJsonElement(call(buildJsonObject {
        put("action", "initialLink"); put("message_id", messageId); put("expected", json.encodeToJsonElement(expected))
    }))

    suspend fun browse(messageId: String, expected: DMessageShare, virtualPath: String?): SharedBrowseResult? = call(buildJsonObject {
        put("action", "browse"); put("message_id", messageId); put("expected", json.encodeToJsonElement(expected)); put("virtual_path", virtualPath)
    }).takeUnless { it is JsonNull }?.let { json.decodeFromJsonElement(it) }

    suspend fun fetchSharedInfo(link: SharedLink, virtualPath: String?): SharedInfoDto = json.decodeFromJsonElement(call(buildJsonObject {
        put("action", "fetch"); put("link", json.encodeToJsonElement(link)); put("virtual_path", virtualPath)
    }))

    suspend fun plan(kind: ShareBatchType, link: SharedLink, entries: List<SharedFileDto>, targetDir: String, downloadsBase: String): SharedBatchPlan =
        json.decodeFromJsonElement(call(buildJsonObject {
            put("action", "plan"); put("kind", kind.name); put("link", json.encodeToJsonElement(link))
            put("entries", json.encodeToJsonElement(entries)); put("target_dir", targetDir); put("downloads_base", downloadsBase)
        }))

    suspend fun fileUrl(link: SharedLink, urlToken: String, virtualPath: String): String = call(buildJsonObject {
        put("action", "fileUrl"); put("link", json.encodeToJsonElement(link)); put("url_token", urlToken); put("virtual_path", virtualPath); put("zip", false)
    }).jsonPrimitive.content

    suspend fun downloadTo(
        url: String,
        write: (buffer: ByteArray, length: Int) -> Unit,
        onProgress: (downloaded: Long, total: Long) -> Unit = { _, _ -> },
    ) {
        RustContentApi.postStream("shares/client/file", buildJsonObject { put("url", url) }).use { response ->
            check(response.isSuccess()) { "Shared download HTTP ${response.status.value}" }
            val total = response.header("Content-Length")?.toLongOrNull() ?: -1L
            val buffer = ByteArray(64 * 1024)
            var downloaded = 0L
            var lastReport = 0L
            while (true) {
                currentCoroutineContext().ensureActive()
                val read = response.channel.readAvailable(buffer)
                if (read == -1) break
                if (read > 0) {
                    write(buffer, read)
                    downloaded += read
                }
                val now = TimeHelper.nowMillis()
                if (now - lastReport > 300) { onProgress(downloaded, total); lastReport = now }
            }
            currentCoroutineContext().ensureActive()
            onProgress(downloaded, total)
        }
    }

    suspend fun packZip(items: List<com.ismartcoding.plain.platform.ZipStreamEntry>, write: (ByteArray, Int) -> Unit) {
        val body = buildJsonObject { put("items", JsonArray(items.map { item -> buildJsonObject {
            put("sourcePath", item.sourcePath); put("entryName", item.entryName)
        } })) }
        RustContentApi.postStream("shares/client/zip", body).use { response ->
            check(response.isSuccess()) { "Shared ZIP HTTP ${response.status.value}" }
            val buffer = ByteArray(64 * 1024)
            while (true) {
                currentCoroutineContext().ensureActive()
                val read = response.channel.readAvailable(buffer)
                if (read == -1) break
                if (read > 0) write(buffer, read)
            }
            currentCoroutineContext().ensureActive()
        }
    }

    private suspend fun call(body: JsonObject): JsonElement = RustContentApi.postJson("shares/client", body, longRunning = true).getValue("result")
}
