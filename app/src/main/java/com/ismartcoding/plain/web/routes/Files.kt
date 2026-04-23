package com.ismartcoding.plain.web.routes

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.core.net.toUri
import com.ismartcoding.lib.extensions.compress
import com.ismartcoding.lib.extensions.getContentType
import com.ismartcoding.lib.extensions.getFinalPath
import com.ismartcoding.lib.extensions.isImageFast
import com.ismartcoding.lib.extensions.urlEncode
import com.ismartcoding.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.lib.helpers.JsonHelper.jsonDecode
import com.ismartcoding.plain.MainApp
import com.ismartcoding.plain.api.HttpClientManager
import com.ismartcoding.plain.enums.ImageType
import com.ismartcoding.plain.features.PackageHelper
import com.ismartcoding.plain.helpers.ImageHelper
import com.ismartcoding.plain.helpers.Mp4Helper
import com.ismartcoding.plain.helpers.UrlHelper
import com.ismartcoding.plain.thumbnail.ThumbnailGenerator
import com.ismartcoding.plain.web.FileIdParams
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.http.content.LocalFileContent
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.response.respondBytes
import io.ktor.server.response.respondFile
import io.ktor.server.response.respondOutputStream
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import okhttp3.Request
import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.text.isEmpty
import kotlin.text.toBooleanStrictOrNull
import kotlin.text.toIntOrNull

fun Route.addFiles() {
    get("/fs") {
        val q = call.request.queryParameters
        val id = q["id"] ?: ""
        if (id.isEmpty()) {
            call.respond(HttpStatusCode.BadRequest)
            return@get
        }
        try {
            val context = MainApp.instance
            val decryptedId = UrlHelper.decrypt(id).getFinalPath(context)
            var path: String
            var mediaId = ""
            var jsonName = ""
            if (decryptedId.startsWith("{")) {
                val params = jsonDecode<FileIdParams>(decryptedId)
                path = params.path.getFinalPath(context)
                mediaId = params.mediaId
                jsonName = params.name
            } else {
                path = decryptedId
            }

            if (path.startsWith("content://")) {
                val uri = path.toUri()
                val mimeType = context.contentResolver.getType(uri).orEmpty()
                if (mimeType.equals("video/3gpp", true) || mimeType.equals("video/3gp", true) || path.endsWith(".3gp", true)) {
                    val mp4Bytes = withIO { Mp4Helper.convert3gpToMp4(context, uri) }
                    if (mp4Bytes != null) {
                        call.respondBytes(mp4Bytes, ContentType.parse("video/mp4"))
                        return@get
                    }
                }

                val bytes = withIO { context.contentResolver.openInputStream(uri)?.buffered()?.use { it.readBytes() } }
                if (bytes != null) {
                    if (mimeType.isNotEmpty()) {
                        call.respondBytes(bytes, ContentType.parse(mimeType))
                    } else {
                        call.respondBytes(bytes, ContentType.Application.OctetStream)
                    }
                } else {
                    call.respond(HttpStatusCode.NotFound)
                }
            } else if (path.startsWith("pkgicon://")) {
                val packageName = path.substring(10)
                val bitmap = PackageHelper.getIcon(packageName)
                val bytes = withIO {
                    ByteArrayOutputStream().use {
                        bitmap.compress(80, it)
                        it.toByteArray()
                    }
                }
                call.respond(bytes)
            } else {
                val file = File(path)
                if (!file.exists()) {
                    call.respond(HttpStatusCode.NotFound)
                    return@get
                }
                if (file.isDirectory) {
                    call.respond(HttpStatusCode.BadRequest)
                    return@get
                }

                call.response.header("Access-Control-Expose-Headers", "Content-Disposition")
                val fileName = (jsonName.ifEmpty { file.name }).urlEncode().replace("+", "%20")
                if (q["dl"] == "1") {
                    call.response.header(
                        "Content-Disposition",
                        "attachment; filename=\"${fileName}\"; filename*=utf-8''${fileName}"
                    )
                    call.respondFile(file)
                    return@get
                } else {
                    call.response.header(
                        "Content-Disposition",
                        "inline; filename=\"${fileName}\"; filename*=utf-8''${fileName}"
                    )
                }

                if (fileName.isImageFast()) {
                    val imageType = ImageHelper.getImageType(path, fileName)
                    if (imageType.isApplicableAnimated() || imageType == ImageType.SVG) {
                        call.respond(LocalFileContent(file, fileName.getContentType()))
                        return@get
                    }
                }

                val w = q["w"]?.toIntOrNull()
                val h = q["h"]?.toIntOrNull()
                val centerCrop = q["cc"]?.toBooleanStrictOrNull() != false
                // get video/image thumbnail
                if (w != null && h != null) {
                    val bytes = withIO { ThumbnailGenerator.toThumbBytesAsync(MainApp.instance, file, w, h, centerCrop, mediaId, fileName) }
                    if (bytes != null) {
                        call.respondBytes(bytes)
                    }
                    return@get
                }
                val header = ByteArray(12)
                val headerSize = file.inputStream().use { it.read(header) }
                val isHeif = headerSize >= 12 &&
                        header[4] == 0x66.toByte() && // 'f'
                        header[5] == 0x74.toByte() && // 't'
                        header[6] == 0x79.toByte() && // 'y'
                        header[7] == 0x70.toByte() && // 'p'
                        String(header.copyOfRange(8, 12)) in listOf("heic", "heix", "hevc", "hevx", "avif")

                if (isHeif) {
                    val bytes = file.readBytes()
                    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    val output = ByteArrayOutputStream()
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
                    call.respondBytes(output.toByteArray(), ContentType.Image.PNG)
                } else {
                    call.respond(LocalFileContent(file, fileName.getContentType()))
                }
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
            call.respondText("File is expired or does not exist. $ex", status = HttpStatusCode.Forbidden)
        }
    }

    get("/proxyfs") {
        val q = call.request.queryParameters
        val id = q["id"] ?: ""
        if (id.isEmpty()) {
            call.respond(HttpStatusCode.BadRequest)
            return@get
        }

        try {
            val peerUrl = UrlHelper.decrypt(id)
            if (peerUrl.isEmpty() || !peerUrl.startsWith("http")) {
                call.respond(HttpStatusCode.BadRequest, "Invalid peer URL")
                return@get
            }

            val client = HttpClientManager.createUnsafeOkHttpClient()
            val request = Request.Builder().url(peerUrl).build()

            val response = withIO { client.newCall(request).execute() }

            call.response.status(HttpStatusCode.fromValue(response.code))

            for ((name, value) in response.headers) {
                if (!name.equals("Transfer-Encoding", true) &&
                    !name.equals("Connection", true)
                ) {
                    call.response.headers.append(name, value)
                }
            }

            val body = response.body ?: run {
                call.respond(HttpStatusCode.NotFound)
                return@get
            }

            call.respondOutputStream {
                body.byteStream().use { input ->
                    input.copyTo(this)
                }
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
            call.respond(HttpStatusCode.InternalServerError, ex.message ?: "")
        }
    }
}