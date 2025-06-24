package com.ismartcoding.plain.web

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Build
import com.ismartcoding.lib.channel.sendEvent
import com.ismartcoding.lib.extensions.compress
import com.ismartcoding.lib.extensions.getFinalPath
import com.ismartcoding.lib.extensions.isImageFast
import com.ismartcoding.lib.extensions.isUrl
import com.ismartcoding.lib.extensions.scanFileByConnection
import com.ismartcoding.lib.extensions.urlEncode
import com.ismartcoding.lib.helpers.CoroutinesHelper.coIO
import com.ismartcoding.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.lib.helpers.CryptoHelper
import com.ismartcoding.lib.helpers.JsonHelper
import com.ismartcoding.lib.helpers.JsonHelper.jsonDecode
import com.ismartcoding.lib.helpers.ZipHelper
import com.ismartcoding.lib.logcat.LogCat
import com.ismartcoding.lib.upnp.UPnPController
import com.ismartcoding.plain.BuildConfig
import com.ismartcoding.plain.MainApp
import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.api.HttpClientManager
import com.ismartcoding.plain.data.DownloadFileItem
import com.ismartcoding.plain.data.DownloadFileItemWrap
import com.ismartcoding.plain.data.UploadChunkInfo
import com.ismartcoding.plain.data.UploadInfo
import com.ismartcoding.plain.enums.DataType
import com.ismartcoding.plain.enums.ImageType
import com.ismartcoding.plain.enums.PasswordType
import com.ismartcoding.plain.extensions.newFile
import com.ismartcoding.plain.extensions.toThumbBytesAsync
import com.ismartcoding.plain.events.ConfirmToAcceptLoginEvent
import com.ismartcoding.plain.features.PackageHelper
import com.ismartcoding.plain.features.file.FileSortBy
import com.ismartcoding.plain.features.media.AudioMediaStoreHelper
import com.ismartcoding.plain.features.media.CastPlayer
import com.ismartcoding.plain.features.media.ImageMediaStoreHelper
import com.ismartcoding.plain.features.media.VideoMediaStoreHelper
import com.ismartcoding.plain.helpers.ImageHelper
import com.ismartcoding.plain.helpers.TempHelper
import com.ismartcoding.plain.helpers.UrlHelper
import com.ismartcoding.plain.preferences.AuthTwoFactorPreference
import com.ismartcoding.plain.preferences.PasswordPreference
import com.ismartcoding.plain.preferences.PasswordTypePreference
import com.ismartcoding.plain.web.websocket.WebSocketSession
import io.ktor.client.request.get
import io.ktor.client.statement.readRawBytes
import io.ktor.http.CacheControl
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.CachingOptions
import io.ktor.http.content.EntityTagVersion
import io.ktor.http.content.LastModifiedVersion
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCallPipeline
import io.ktor.server.application.ApplicationStopPreparing
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.http.content.SPAConfig
import io.ktor.server.http.content.staticResources
import io.ktor.server.plugins.autohead.AutoHeadResponse
import io.ktor.server.plugins.cachingheaders.CachingHeaders
import io.ktor.server.plugins.conditionalheaders.ConditionalHeaders
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.plugins.forwardedheaders.ForwardedHeaders
import io.ktor.server.plugins.origin
import io.ktor.server.plugins.partialcontent.PartialContent
import io.ktor.server.request.header
import io.ktor.server.request.receiveMultipart
import io.ktor.server.request.receiveText
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.response.respondBytes
import io.ktor.server.response.respondFile
import io.ktor.server.response.respondOutputStream
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.webSocket
import io.ktor.utils.io.jvm.javaio.copyTo
import io.ktor.utils.io.toByteArray
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readBytes
import io.ktor.websocket.send
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.Date
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object HttpModule {
    @SuppressLint("SuspiciousIndentation")
    val module: Application.() -> Unit = {
        install(CachingHeaders) {
            options { _, outgoingContent ->
                when (outgoingContent.contentType?.withoutParameters()) {
                    ContentType.Text.CSS, ContentType.Application.JavaScript ->
                        CachingOptions(
                            CacheControl.MaxAge(maxAgeSeconds = 3600 * 24 * 30),
                        )

                    else -> null
                }
            }
        }

        install(CORS) {
            if (BuildConfig.DEBUG) {
                allowHost("*")
            } else {
                allowHost("localhost:3000")
                allowHost("127.0.0.1:3000")
            }
            allowHeadersPrefixed("c-")
            allowHeader("x-box-api")
        }

        install(ConditionalHeaders)
        install(WebSockets)
//        install(Compression) // this will slow down the download speed
        install(ForwardedHeaders)
        install(PartialContent)
        install(AutoHeadResponse)
        install(ContentNegotiation) {
            json(
                Json {
                    prettyPrint = true
                    isLenient = true
                },
            )
        }

        intercept(ApplicationCallPipeline.Plugins) {
            if (!TempData.webEnabled) {
                call.respond(HttpStatusCode.NotFound)
                return@intercept finish()
            }
        }

        routing {
            val config = SPAConfig()
            config.filesPath = "web"
            staticResources(config.applicationRoute, config.filesPath, index = config.defaultPage) {
                cacheControl {
                    arrayListOf(
                        CacheControl.NoCache(CacheControl.Visibility.Public),
                        CacheControl.NoStore(CacheControl.Visibility.Public),
                    )
                }
                default(config.defaultPage)
            }

            get("/health_check") {
                call.respond(HttpStatusCode.OK, BuildConfig.APPLICATION_ID)
            }

            get("/shutdown") {
                val ip = call.request.origin.remoteHost
                LogCat.d("$ip is shutting down the server")
                if (ip != "localhost") {
                    call.respond(HttpStatusCode.Forbidden)
                    return@get
                }

                HttpServerManager.wsSessions.forEach {
                    it.session.close()
                }
                HttpServerManager.wsSessions.clear()
                val latch = CompletableDeferred<Nothing>()
                val application = call.application
                val environment = application.environment
                application.launch {
                    latch.join()
                    application.monitor.raise(ApplicationStopPreparing, environment)
                    application.dispose()
                }

                try {
                    call.respond(HttpStatusCode.Gone)
                } finally {
                    latch.cancel()
                }
            }

            get("/media/{id}") {
                val id = call.parameters["id"]?.split(".")?.get(0) ?: ""
                if (id.isEmpty()) {
                    call.respond(HttpStatusCode.BadRequest)
                    return@get
                }
                try {
                    val path = UrlHelper.getMediaPath(id)
                    if (path.isEmpty()) {
                        call.respond(HttpStatusCode.BadRequest)
                        return@get
                    }

                    if (path.isUrl()) {
                        try {
                            val client = HttpClientManager.browserClient()
                            val r = client.get(path)
                            call.respondBytes(r.readRawBytes(), r.contentType() ?: ContentType.Application.OctetStream)
                        } catch (e: IOException) {
                            call.respondText("Failed to fetch data from URL: $path", status = HttpStatusCode.InternalServerError)
                        }
                    } else if (path.startsWith("content://")) {
                        val bytes = MainApp.instance.contentResolver.openInputStream(Uri.parse(path))?.buffered()?.use { it.readBytes() }
                        call.respondBytes(bytes!!)
                    } else if (path.isImageFast()) {
                        call.respondFile(File(path))
                    } else {
                        val file = File(path)
                        call.response.run {
                            header("realTimeInfo.dlna.org", "DLNA.ORG_TLAG=*")
                            header("contentFeatures.dlna.org", "")
                            header("transferMode.dlna.org", "Streaming")
                            header("Connection", "keep-alive")
                            header(
                                "Server",
                                "DLNADOC/1.50 UPnP/1.0 Plain/1.0 Android/" + Build.VERSION.RELEASE,
                            )

                            EntityTagVersion(file.lastModified().hashCode().toString())
                            LastModifiedVersion(Date(file.lastModified()))
                            status(HttpStatusCode.PartialContent) // some TV os only accepts 206
                        }
                        call.respondFile(file)
                    }
                } catch (ex: Exception) {
                    // ex.printStackTrace()
                    call.respondText("File is expired or does not exist. $ex", status = HttpStatusCode.Forbidden)
                }
            }

            get("/zip/dir") {
                val q = call.request.queryParameters
                val id = q["id"] ?: ""
                if (id.isEmpty()) {
                    call.respond(HttpStatusCode.BadRequest)
                    return@get
                }

                val path = UrlHelper.decrypt(id)
                val folder = File(path)
                if (!folder.exists() || !folder.isDirectory) {
                    call.respond(HttpStatusCode.NotFound)
                    return@get
                }

                val fileName = (q["name"] ?: "${folder.name}.zip").urlEncode().replace("+", "%20")
                call.response.header("Content-Disposition", "attachment;filename=\"${fileName}\";filename*=utf-8''\"${fileName}\"")
                call.response.header(HttpHeaders.ContentType, ContentType.Application.Zip.toString())
                call.respondOutputStream(ContentType.Application.Zip) {
                    ZipOutputStream(this).use { zip ->
                        ZipHelper.zipFolderToStreamAsync(folder, zip)
                    }
                }
            }

            get("/zip/files") {
                val query = call.request.queryParameters
                val id = query["id"] ?: ""
                if (id.isEmpty()) {
                    call.respond(HttpStatusCode.BadRequest)
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
                }
            }

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
                    if (decryptedId.startsWith("{")) {
                        val json = JSONObject(decryptedId)
                        path = json.optString("path")
                        mediaId = json.optString("mediaId")
                    } else {
                        path = decryptedId
                    }
                    if (path.startsWith("content://")) {
                        val bytes = withIO { context.contentResolver.openInputStream(Uri.parse(path))?.buffered()?.use { it.readBytes() } }
                        if (bytes != null) {
                            call.respondBytes(bytes)
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

                        val fileName = (q["name"] ?: file.name).urlEncode().replace("+", "%20")
                        if (q["dl"] == "1") {
                            call.response.header("Content-Disposition", "attachment;filename=\"${fileName}\";filename*=utf-8''\"${fileName}\"")
                        } else {
                            call.response.header("Content-Disposition", "inline;filename=\"${fileName}\";filename*=utf-8''\"${fileName}\"")
                        }

                        call.response.header("Access-Control-Expose-Headers", "Content-Disposition") 

                        if (path.isImageFast()) {
                            val imageType = ImageHelper.getImageType(path)
                            if (imageType.isApplicableAnimated() || imageType == ImageType.SVG) {
                                call.respondFile(file)
                                return@get
                            }
                        }

                        val w = q["w"]?.toIntOrNull()
                        val h = q["h"]?.toIntOrNull()
                        val centerCrop = q["cc"]?.toBooleanStrictOrNull() != false
                        // get video/image thumbnail
                        if (w != null && h != null) {
                            val bytes = withIO { file.toThumbBytesAsync(MainApp.instance, w, h, centerCrop, mediaId) }
                            if (bytes != null) {
                                call.respondBytes(bytes)
                            }
                            return@get
                        }

                        call.respondFile(file)
                    }
                } catch (ex: Exception) {
                    ex.printStackTrace()
                    call.respondText("File is expired or does not exist. $ex", status = HttpStatusCode.Forbidden)
                }
            }

            route("/callback/cast", HttpMethod("NOTIFY")) {
                handle {
                    val xml = call.receiveText()
                    LogCat.d(xml)
                    // the TV could send the callback twice in short time, the second one should be ignore if it has AVTransportURIMetaData field.
                    if (xml.contains("TransportState val=\"STOPPED\"") && !xml.contains("AVTransportURIMetaData")) {
                        withIO {
                            CastPlayer.isPlaying.value = false
                            val castItems = CastPlayer.items.value
                            if (castItems.isNotEmpty()) {
                                CastPlayer.currentDevice?.let { device ->
                                    val currentUri = CastPlayer.currentUri.value
                                    var index = castItems.indexOfFirst { it.path == currentUri }
                                    index++
                                    if (index > castItems.size - 1) {
                                        index = 0
                                    }
                                    val current = castItems[index]
                                    if (current.path != currentUri) {
                                        LogCat.d(current.path)
                                        UPnPController.setAVTransportURIAsync(device, UrlHelper.getMediaHttpUrl(current.path))
                                        CastPlayer.setCurrentUri(current.path)
                                        CastPlayer.isPlaying.value = true
                                    }
                                }
                            }
                        }
                    } else if (xml.contains("TransportState val=\"PLAYING\"")) {
                        withIO {
                            CastPlayer.isPlaying.value = true
                        }
                    } else if (xml.contains("TransportState val=\"PAUSED_PLAYBACK\"")) {
                        withIO {
                            CastPlayer.isPlaying.value = false
                        }
                    }
                    
                    // 尝试解析播放位置信息
                    if (xml.contains("RelTime val=") && xml.contains("TrackDuration val=")) {
                        withIO {
                            try {
                                val relTimeMatch = Regex("RelTime val=\"([^\"]+)\"").find(xml)
                                val durationMatch = Regex("TrackDuration val=\"([^\"]+)\"").find(xml)
                                
                                if (relTimeMatch != null && durationMatch != null) {
                                    val relTime = relTimeMatch.groupValues[1]
                                    val trackDuration = durationMatch.groupValues[1]
                                    CastPlayer.updatePositionInfo(relTime, trackDuration)
                                }
                            } catch (e: Exception) {
                                // 解析失败，忽略
                            }
                        }
                    }
                    call.respond(HttpStatusCode.OK)
                }
            }

            post("/upload") {
                val clientId = call.request.header("c-id") ?: ""
                if (clientId.isEmpty()) {
                    call.respond(HttpStatusCode.BadRequest, "c-id header is missing")
                    return@post
                }

                val token = HttpServerManager.tokenCache[clientId]
                if (token == null) {
                    call.respond(HttpStatusCode.Unauthorized)
                    return@post
                }
                try {
                    lateinit var info: UploadInfo
                    var fileName = ""
                    call.receiveMultipart(formFieldLimit = Long.MAX_VALUE).forEachPart { part ->
                        when (part) {
                            is PartData.FileItem -> {
                                when (part.name) {
                                    "info" -> {
                                        var requestStr = ""
                                        val decryptedBytes = CryptoHelper.chaCha20Decrypt(token, part.provider().toByteArray())
                                        if (decryptedBytes != null) {
                                            requestStr = decryptedBytes.decodeToString()
                                        }
                                        if (requestStr.isEmpty()) {
                                            call.respond(HttpStatusCode.Unauthorized)
                                            return@forEachPart
                                        }

                                        info = jsonDecode<UploadInfo>(requestStr)
                                    }

                                    "file" -> {
                                        fileName = part.originalFileName as String
                                        if (info.dir.isEmpty() || fileName.isEmpty()) {
                                            call.respond(HttpStatusCode.BadRequest)
                                            return@forEachPart
                                        }
                                        var destFile = File("${info.dir}/$fileName")
                                        if (destFile.exists()) {
                                            if (info.replace) {
                                                destFile.delete()
                                            } else {
                                                destFile = destFile.newFile()
                                                fileName = destFile.name
                                            }
                                        }
                                        LogCat.d("Upload: ${info.dir}, ${destFile.absolutePath}")
                                        destFile.parentFile?.mkdirs()

                                        part.provider().let { input ->
                                            val outputStream = FileOutputStream(destFile)
                                            input.copyTo(outputStream)
                                            outputStream.close()
                                        }
                                        MainApp.instance.scanFileByConnection(destFile, null)
                                    }

                                    else -> {}
                                }
                            }

                            else -> {
                            }
                        }
                        part.dispose()
                    }
                    call.respond(HttpStatusCode.Created, fileName)
                } catch (ex: Exception) {
                    ex.printStackTrace()
                    call.respond(HttpStatusCode.BadRequest, ex.message ?: "")
                }
            }

            post("/upload_chunk") {
                val clientId = call.request.header("c-id") ?: ""
                if (clientId.isEmpty()) {
                    call.respond(HttpStatusCode.BadRequest, "c-id header is missing")
                    return@post
                }

                val token = HttpServerManager.tokenCache[clientId]
                if (token == null) {
                    call.respond(HttpStatusCode.Unauthorized)
                    return@post
                }

                try {
                    lateinit var chunkInfo: UploadChunkInfo
                    var chunkSaved = false

                    call.receiveMultipart(formFieldLimit = Long.MAX_VALUE).forEachPart { part ->
                        when (part) {
                            is PartData.FileItem -> {
                                when (part.name) {
                                    "info" -> {
                                        var requestStr = ""
                                        val decryptedBytes = CryptoHelper.chaCha20Decrypt(token, part.provider().toByteArray())
                                        if (decryptedBytes != null) {
                                            requestStr = decryptedBytes.decodeToString()
                                        }
                                        if (requestStr.isEmpty()) {
                                            call.respond(HttpStatusCode.Unauthorized)
                                            return@forEachPart
                                        }

                                        chunkInfo = jsonDecode<UploadChunkInfo>(requestStr)
                                    }

                                    "file" -> {
                                        if (chunkInfo.fileId.isEmpty() || chunkInfo.index < 0) {
                                            call.respond(HttpStatusCode.BadRequest, "fileId or index is missing or invalid")
                                            return@forEachPart
                                        }

                                        // Create directory in cache dir using file_id as directory name
                                        val chunkDir = File(MainApp.instance.filesDir, "upload_tmp/${chunkInfo.fileId}")
                                        chunkDir.mkdirs()

                                        // Save chunk file with name chunk_$index
                                        val chunkFile = File(chunkDir, "chunk_${chunkInfo.index}")
                                        part.provider().let { input ->
                                            val outputStream = FileOutputStream(chunkFile)
                                            input.copyTo(outputStream)
                                            outputStream.close()
                                        }
                                        chunkSaved = true
                                    }

                                    else -> {}
                                }
                            }

                            else -> {}
                        }
                        part.dispose()
                    }

                    if (chunkSaved) {
                        call.respond(HttpStatusCode.Created, "chunk_${chunkInfo.index}")
                    } else {
                        call.respond(HttpStatusCode.BadRequest, "chunk upload failed")
                    }
                } catch (ex: Exception) {
                    ex.printStackTrace()
                    call.respond(HttpStatusCode.BadRequest, ex.message ?: "")
                }
            }

            // this api is to fix the websocket takes 10s to get remoteAddress on some phones.
            post("/init") {
                val clientId = call.request.headers["c-id"] ?: ""
                if (clientId.isEmpty()) {
                    call.respond(HttpStatusCode.BadRequest, "`c-id` is missing in the headers")
                    return@post
                }
                if (!TempData.webEnabled) {
                    call.respond(HttpStatusCode.Forbidden, "web_access_disabled")
                    return@post
                }
                HttpServerManager.clientIpCache[clientId] = call.request.origin.remoteAddress
                if (PasswordTypePreference.getValueAsync(MainApp.instance) == PasswordType.NONE) {
                    call.respondText(HttpServerManager.resetPasswordAsync())
                } else {
                    call.respond(HttpStatusCode.NoContent)
                }
            }

            webSocket("/") {
                val q = call.request.queryParameters
                if (q["test"] == "1") {
                    close(CloseReason(CloseReason.Codes.NORMAL, BuildConfig.APPLICATION_ID))
                    return@webSocket
                }
                val clientId = q["cid"] ?: ""
                if (clientId.isEmpty()) {
                    LogCat.e("ws: `cid` is missing")
                    close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "`cid` is missing"))
                    return@webSocket
                }

                val session = WebSocketSession(System.currentTimeMillis(), clientId, this)
                try {
                    for (frame in incoming) {
                        when (frame) {
                            is Frame.Binary -> {
                                if (q["auth"] == "1") {
                                    var r: AuthRequest? = null
                                    val hash = CryptoHelper.sha512(PasswordPreference.getAsync(MainApp.instance).toByteArray())
                                    val token = HttpServerManager.hashToToken(hash)
                                    val decryptedBytes = CryptoHelper.chaCha20Decrypt(token, frame.readBytes())
                                    if (decryptedBytes != null) {
                                        r = jsonDecode<AuthRequest>(decryptedBytes.decodeToString())
                                    }
                                    if (r?.password == hash) {
                                        val event = ConfirmToAcceptLoginEvent(this, clientId, r)
                                        if (AuthTwoFactorPreference.getAsync(MainApp.instance)) {
                                            send(CryptoHelper.chaCha20Encrypt(token, JsonHelper.jsonEncode(AuthResponse(AuthStatus.PENDING))))
                                            sendEvent(event)
                                        } else {
                                            coIO {
                                                val clientIp = HttpServerManager.clientIpCache[event.clientId] ?: ""
                                                HttpServerManager.respondTokenAsync(event, clientIp)
                                            }
                                        }
                                    } else {
                                        LogCat.e("ws: invalid_password")
                                        close(CloseReason(CloseReason.Codes.TRY_AGAIN_LATER, "invalid_password"))
                                    }
                                } else {
                                    val token = HttpServerManager.tokenCache[clientId]
                                    if (token != null) {
                                        val decryptedBytes = CryptoHelper.chaCha20Decrypt(token, frame.readBytes())
                                        if (decryptedBytes != null) {
                                            LogCat.d("ws: add session ${session.id}, ts: ${decryptedBytes.decodeToString()}")
                                            HttpServerManager.wsSessions.add(session)
                                        } else {
                                            LogCat.d("ws: invalid_request")
                                            close(CloseReason(CloseReason.Codes.TRY_AGAIN_LATER, "invalid_request"))
                                        }
                                    } else {
                                        LogCat.d("ws: invalid_request")
                                        close(CloseReason(CloseReason.Codes.TRY_AGAIN_LATER, "invalid_request"))
                                    }
                                }
                            }

                            else -> {}
                        }
                    }
                } catch (ex: Exception) {
                    LogCat.e("ws: $ex")
                } finally {
                    LogCat.d("ws: remove session ${session.id}")
                    HttpServerManager.wsSessions.removeIf { it.id == session.id }
                }
            }
        }
        install(MainGraphQL) {
            init()
        }
        install(PeerGraphQL) {
            init()
        }
    }
}
