package com.ismartcoding.plain.web.routes

import com.ismartcoding.lib.extensions.getFinalPath
import com.ismartcoding.lib.extensions.urlEncode
import com.ismartcoding.lib.helpers.JsonHelper.jsonDecode
import com.ismartcoding.lib.helpers.ZipHelper
import com.ismartcoding.plain.MainApp
import com.ismartcoding.plain.data.DownloadFileItem
import com.ismartcoding.plain.data.DownloadFileItemWrap
import com.ismartcoding.plain.db.AppDatabase
import com.ismartcoding.plain.enums.DataType
import com.ismartcoding.plain.features.PackageHelper
import com.ismartcoding.plain.features.file.FileSortBy
import com.ismartcoding.plain.features.media.AudioMediaStoreHelper
import com.ismartcoding.plain.features.media.ImageMediaStoreHelper
import com.ismartcoding.plain.features.media.VideoMediaStoreHelper
import com.ismartcoding.plain.helpers.TempHelper
import com.ismartcoding.plain.helpers.UrlHelper
import com.ismartcoding.plain.ui.page.appfiles.AppFileDisplayNameHelper
import com.ismartcoding.plain.web.FileIdParams
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.response.respondOutputStream
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import kotlinx.coroutines.sync.Semaphore
import org.json.JSONObject
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.text.ifEmpty
import kotlin.text.isEmpty
private val zipSemaphore = Semaphore(1)

fun Route.addZip() {

    get("/zip/dir") {
        val q = call.request.queryParameters
        val id = q["id"] ?: ""
        if (id.isEmpty()) {
            call.respond(HttpStatusCode.BadRequest)
            return@get
        }

        if (!zipSemaphore.tryAcquire()) {
            call.respond(HttpStatusCode.TooManyRequests)
            return@get
        }

        try {
            val decryptedId = UrlHelper.decrypt(id)
            var dirPath: String
            var jsonName = ""
            if (decryptedId.startsWith("{")) {
                val params = jsonDecode<FileIdParams>(decryptedId)
                dirPath = params.path
                jsonName = params.name
            } else {
                dirPath = decryptedId
            }
            val folder = File(dirPath)
            if (!folder.exists() || !folder.isDirectory) {
                call.respond(HttpStatusCode.NotFound)
                return@get
            }

            val fileName = (jsonName.ifEmpty { "${folder.name}.zip" }).urlEncode().replace("+", "%20")
            call.response.header("Content-Disposition", "attachment;filename=\"${fileName}\";filename*=utf-8''\"${fileName}\"")
            call.response.header(HttpHeaders.ContentType, ContentType.Application.Zip.toString())
            call.respondOutputStream(ContentType.Application.Zip) {
                ZipOutputStream(this).use { zip ->
                    ZipHelper.zipFolderToStreamAsync(folder, zip)
                }
            }
        } finally {
            zipSemaphore.release()
        }
    }

    get("/zip/files") {
        val query = call.request.queryParameters
        val id = query["id"] ?: ""
        if (id.isEmpty()) {
            call.respond(HttpStatusCode.BadRequest)
            return@get
        }

        if (!zipSemaphore.tryAcquire()) {
            call.respond(HttpStatusCode.TooManyRequests)
            return@get
        }

        try {
            val json = JSONObject(UrlHelper.decrypt(id))
            var paths: List<DownloadFileItem> = arrayListOf()
            val type = json.optString("type")
            if (type.isEmpty()) {
                call.respond(HttpStatusCode.BadRequest)
                return@get
            }

            val q = json.optString("query")
            val context = MainApp.instance
            when (type) {
                DataType.PACKAGE.name -> {
                    paths = PackageHelper.searchAsync(q, Int.MAX_VALUE, 0, FileSortBy.NAME_ASC).map { DownloadFileItem(it.path, "${it.name.replace(" ", "")}-${it.id}.apk") }
                }

                DataType.VIDEO.name -> {
                    paths = VideoMediaStoreHelper.searchAsync(context, q, Int.MAX_VALUE, 0, FileSortBy.DATE_DESC).map { DownloadFileItem(it.path, "") }
                }

                DataType.AUDIO.name -> {
                    paths = AudioMediaStoreHelper.searchAsync(context, q, Int.MAX_VALUE, 0, FileSortBy.DATE_DESC).map { DownloadFileItem(it.path, "") }
                }

                DataType.IMAGE.name -> {
                    paths = ImageMediaStoreHelper.searchAsync(context, q, Int.MAX_VALUE, 0, FileSortBy.DATE_DESC).map { DownloadFileItem(it.path, "") }
                }

                DataType.APP_FILE.name -> {
                    val appFileDao = AppDatabase.instance.appFileDao()
                    val chatDao = AppDatabase.instance.chatDao()
                    val ids = q.removePrefix("ids:").split(",").filter { it.isNotEmpty() }
                    val appFiles = if (ids.isNotEmpty()) appFileDao.getByIds(ids) else appFileDao.getAll()
                    val nameMap = AppFileDisplayNameHelper.buildNameMap(chatDao.getAll())
                    paths = appFiles.map { file ->
                        val displayName = AppFileDisplayNameHelper.resolveDisplayName(file, nameMap)
                        DownloadFileItem("fid:${file.id}".getFinalPath(context), displayName)
                    }
                }

                DataType.FILE.name -> {
                    val tmpId = json.optString("id")
                    val value = TempHelper.getValue(tmpId)
                    TempHelper.clearValue(tmpId)
                    if (value.isEmpty()) {
                        call.respond(HttpStatusCode.NotFound)
                        return@get
                    }

                    paths = jsonDecode<List<DownloadFileItem>>(value)
                }
            }

            val items = paths.map { DownloadFileItemWrap(File(it.path), it.name) }.filter { it.file.exists() }
            val dirs = items.filter { it.file.isDirectory }
            val fileName = (json.optString("name").ifEmpty { "download.zip" }).urlEncode().replace("+", "%20")
            call.response.header("Content-Disposition", "attachment;filename=\"${fileName}\";filename*=utf-8''\"${fileName}\"")
            call.response.header(HttpHeaders.ContentType, ContentType.Application.Zip.toString())
            call.respondOutputStream(ContentType.Application.Zip) {
                ZipOutputStream(this).use { zip ->
                    items.forEach { item ->
                        if (dirs.any { item.file.absolutePath != it.file.absolutePath && item.file.absolutePath.startsWith(it.file.absolutePath) }) {
                        } else {
                            val filePath = item.name.ifEmpty { item.file.name }
                            if (item.file.isDirectory) {
                                zip.putNextEntry(ZipEntry("$filePath/"))
                                ZipHelper.zipFolderToStreamAsync(item.file, zip, filePath)
                            } else {
                                zip.putNextEntry(ZipEntry(filePath))
                                item.file.inputStream().copyTo(zip)
                            }
                            zip.closeEntry()
                        }
                    }
                }
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
            call.respond(HttpStatusCode.BadRequest, ex.message ?: "")
        } finally {
            zipSemaphore.release()
        }
    }

}