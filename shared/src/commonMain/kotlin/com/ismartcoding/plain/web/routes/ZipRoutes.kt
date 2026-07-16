package com.ismartcoding.plain.web.routes

import com.ismartcoding.plain.data.DownloadFileItem
import com.ismartcoding.plain.enums.DataType
import com.ismartcoding.plain.helpers.JsonHelper.jsonDecode
import com.ismartcoding.plain.helpers.TempHelper
import com.ismartcoding.plain.helpers.UrlHelper
import com.ismartcoding.plain.lib.extensions.urlEncode
import com.ismartcoding.plain.platform.ZipStreamEntry
import com.ismartcoding.plain.platform.fileExists
import com.ismartcoding.plain.platform.searchZipItems
import com.ismartcoding.plain.platform.statFile
import com.ismartcoding.plain.platform.streamZipFolderToSink
import com.ismartcoding.plain.platform.streamZipToSink
import com.ismartcoding.plain.web.FileIdParams
import com.ismartcoding.plain.web.http.HttpCall
import com.ismartcoding.plain.web.http.HttpRouter
import com.ismartcoding.plain.web.http.HttpStatus
import kotlinx.coroutines.sync.Semaphore
import kotlinx.serialization.Serializable

/**
 * Limit concurrent zip operations to 1 to prevent resource exhaustion when the
 * web UI triggers multiple download requests (e.g. double-click). Additional
 * callers receive HTTP 429 Too Many Requests.
 */
private val zipSemaphore = Semaphore(1)

/**
 * `/zip/dir` and `/zip/files` — zip streaming endpoints shared between Android
 * (Ktor) and iOS (SwiftNIO future). Both endpoints acquire [zipSemaphore]
 * before doing any work so that only one zip operation runs at a time on the
 * device.
 *
 * The actual zip encoding is delegated to the platform abstraction
 * (`streamZipFolderToSink` / `streamZipToSink`) so that commonMain does not
 * depend on `java.util.zip`.
 */
fun HttpRouter.addZipRoutes() {
    get("/zip/dir") { call ->
        val id = call.queryParam("id") ?: ""
        if (id.isEmpty()) {
            call.respondNoBody(HttpStatus.BAD_REQUEST)
            return@get
        }
        if (!zipSemaphore.tryAcquire()) {
            call.respondNoBody(HttpStatus.TOO_MANY_REQUESTS)
            return@get
        }
        try {
            val decryptedId = UrlHelper.decrypt(id)
            val dirPath: String
            val jsonName: String
            if (decryptedId.startsWith("{")) {
                val params = jsonDecode<FileIdParams>(decryptedId)
                dirPath = params.path
                jsonName = params.name
            } else {
                dirPath = decryptedId
                jsonName = ""
            }

            val stat = statFile(dirPath)
            if (stat == null || !stat.isDir) {
                call.respondNoBody(HttpStatus.NOT_FOUND)
                return@get
            }

            val folderName = dirPath.substringAfterLast('/')
            val fileName = (jsonName.ifEmpty { "$folderName.zip" }).urlEncode()
            call.respondStream(
                contentType = "application/zip",
                headers = mapOf(
                    "Content-Disposition" to
                        "attachment;filename=\"${fileName}\";filename*=utf-8''\"${fileName}\""
                ),
            ) { sink ->
                streamZipFolderToSink(dirPath, sink)
            }
        } finally {
            zipSemaphore.release()
        }
    }

    get("/zip/files") { call ->
        val id = call.queryParam("id") ?: ""
        if (id.isEmpty()) {
            call.respondNoBody(HttpStatus.BAD_REQUEST)
            return@get
        }
        if (!zipSemaphore.tryAcquire()) {
            call.respondNoBody(HttpStatus.TOO_MANY_REQUESTS)
            return@get
        }
        try {
            val request = jsonDecode<ZipFilesRequest>(UrlHelper.decrypt(id))
            if (request.type.isEmpty()) {
                call.respondNoBody(HttpStatus.BAD_REQUEST)
                return@get
            }

            // For FILE type the item list is stored in TempHelper under
            // request.id. We read and clear it here in commonMain so that an
            // empty/expired temp value yields HTTP 404 (matching the original
            // behavior) instead of producing an empty zip.
            val items: List<ZipStreamEntry> = if (request.type == DataType.FILE.name) {
                val value = TempHelper.getValue(request.id)
                TempHelper.clearValue(request.id)
                if (value.isEmpty()) {
                    call.respondNoBody(HttpStatus.NOT_FOUND)
                    return@get
                }
                jsonDecode<List<DownloadFileItem>>(value)
                    .map { ZipStreamEntry(it.path, it.name) }
                    .filter { fileExists(it.sourcePath) }
            } else {
                searchZipItems(request.type, request.query, request.id)
                    .filter { fileExists(it.sourcePath) }
            }

            val fileName = (request.name.ifEmpty { "download.zip" }).urlEncode()
            call.respondStream(
                contentType = "application/zip",
                headers = mapOf(
                    "Content-Disposition" to
                        "attachment;filename=\"${fileName}\";filename*=utf-8''\"${fileName}\""
                ),
            ) { sink ->
                streamZipToSink(items, sink)
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
            call.respondText(ex.message ?: "", status = HttpStatus.BAD_REQUEST)
        } finally {
            zipSemaphore.release()
        }
    }
}

/** Decrypted `/zip/files` request body. */
@Serializable
private data class ZipFilesRequest(
    val type: String = "",
    val query: String = "",
    val id: String = "",
    val name: String = "",
)
