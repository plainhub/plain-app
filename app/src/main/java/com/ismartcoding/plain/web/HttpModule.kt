package com.ismartcoding.plain.web

import android.net.Uri
import android.os.Build
import com.ismartcoding.lib.channel.sendEvent
import com.ismartcoding.lib.extensions.isImageFast
import com.ismartcoding.lib.extensions.scanFileByConnection
import com.ismartcoding.lib.extensions.toThumbBytes
import com.ismartcoding.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.lib.helpers.CryptoHelper
import com.ismartcoding.lib.helpers.JsonHelper
import com.ismartcoding.lib.logcat.LogCat
import com.ismartcoding.lib.upnp.UPnPController
import com.ismartcoding.plain.LocalStorage
import com.ismartcoding.plain.MainApp
import com.ismartcoding.plain.features.ConfirmToAcceptLoginEvent
import com.ismartcoding.plain.features.media.CastPlayer
import com.ismartcoding.plain.helpers.FileHelper
import com.ismartcoding.plain.helpers.UrlHelper
import com.ismartcoding.plain.web.websocket.WebSocketSession
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.http.content.*
import io.ktor.server.plugins.*
import io.ktor.server.plugins.autohead.*
import io.ktor.server.plugins.cachingheaders.*
import io.ktor.server.plugins.compression.*
import io.ktor.server.plugins.conditionalheaders.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.forwardedheaders.*
import io.ktor.server.plugins.partialcontent.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.utils.io.core.*
import io.ktor.websocket.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.json.JSONObject
import java.io.File
import java.net.URLEncoder
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.*
import kotlin.text.toByteArray


fun Application.module() {
    install(CachingHeaders) {
        options { _, outgoingContent ->
            when (outgoingContent.contentType?.withoutParameters()) {
                ContentType.Text.CSS, ContentType.Application.JavaScript -> CachingOptions(CacheControl.MaxAge(maxAgeSeconds = 3600 * 24 * 30))
                else -> null
            }
        }
    }

    install(CORS) {
        allowHost("localhost:3000")
        allowHost("127.0.0.1:3000")
        allowHeadersPrefixed("c-")
        allowHeader("x-box-api")
    }

    install(ConditionalHeaders)
    install(WebSockets)
    install(Compression)
    install(ForwardedHeaders)
    install(PartialContent)
    install(AutoHeadResponse)
    install(ContentNegotiation) {
        json(Json {
            prettyPrint = true
            isLenient = true
        })
    }

    routing {
        singlePageApplication {
            useResources = true
            ignoreFiles {
                !LocalStorage.webConsoleEnabled
            }
            vue("web")
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

                if (path.startsWith("content://")) {
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
                            "DLNADOC/1.50 UPnP/1.0 Plain/1.0 Android/" + Build.VERSION.RELEASE
                        )

                        EntityTagVersion(file.lastModified().hashCode().toString())
                        LastModifiedVersion(Date(file.lastModified()))
                        status(HttpStatusCode.PartialContent) // some TV os only accepts 206
                    }
                    call.respondFile(file)
                }
            } catch (ex: Exception) {
                //ex.printStackTrace()
                call.respondText("File is expired or does not exist. $ex", status = HttpStatusCode.Forbidden)
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
                val path = FileHelper.getFilePath(id)
                if (path.startsWith("content://")) {
                    val bytes = MainApp.instance.contentResolver.openInputStream(Uri.parse(path))?.buffered()?.use { it.readBytes() }
                    call.respondBytes(bytes!!)
                } else {
                    val file = File(path)
                    val fileName = URLEncoder.encode(q["name"] ?:file.name, "UTF-8")
                    if (q["dl"] == "1") {
                        call.response.header("Content-Disposition", "attachment;filename=\"${fileName}\";filename*=utf-8''\"${fileName}\"")
                    } else {
                        call.response.header("Content-Disposition", "inline;filename=\"${fileName}\";filename*=utf-8''\"${fileName}\"")
                    }

                    val w = q["w"]?.toIntOrNull()
                    val h = q["h"]?.toIntOrNull()
                    val centerCrop = q["cc"]?.toBooleanStrictOrNull()
                    // get video/image thumbnail
                    if (w != null && h != null) {
                        call.respondBytes(file.toThumbBytes(MainApp.instance, w, h, centerCrop == true))
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
                        if (CastPlayer.items?.isNotEmpty() == true) {
                            CastPlayer.currentDevice?.let { device ->
                                val currentUri = CastPlayer.currentUri
                                var index = CastPlayer.items!!.indexOfFirst { it.path == currentUri }
                                index++
                                if (index > CastPlayer.items!!.size - 1) {
                                    index = 0
                                }
                                val current = CastPlayer.items!![index]
                                if (current.path != currentUri) {
                                    LogCat.d(current.path)
                                    UPnPController.setAVTransportURIAsync(device, UrlHelper.getMediaHttpUrl(current.path))
                                    CastPlayer.currentUri = current.path
                                }
                            }
                        }
                    }
                }
                call.respond(HttpStatusCode.OK)
            }
        }

        post("/upload") {
            val clientId = call.request.header("c-id") ?: ""
            if (clientId.isEmpty()) {
                call.respond(HttpStatusCode.BadRequest)
                return@post
            }

            val token = HttpServerManager.tokenCache[clientId]
            if (token == null) {
                call.response.status(HttpStatusCode.Unauthorized)
                return@post
            }
            try {
                var dir = ""
                call.receiveMultipart().forEachPart { part ->
                    part as PartData.FileItem
                    when (part.name) {
                        "info" -> {
                            var requestStr = ""
                            val decryptedBytes = CryptoHelper.aesDecrypt(token, part.streamProvider().readBytes())
                            if (decryptedBytes != null) {
                                requestStr = decryptedBytes.decodeToString()
                            }
                            if (requestStr.isEmpty()) {
                                call.response.status(HttpStatusCode.Unauthorized)
                                return@forEachPart
                            }
                            val json = JSONObject(requestStr)
                            dir = json.optString("dir")
                        }
                        "file" -> {
                            val fileName = part.originalFileName as String
                            if (dir.isEmpty() || fileName.isEmpty()) {
                                call.respond(HttpStatusCode.BadRequest)
                                return@forEachPart
                            }
                            val file = File("${dir}/${fileName}")
                            Files.copy(part.streamProvider(), file.toPath(), StandardCopyOption.REPLACE_EXISTING)
                            MainApp.instance.scanFileByConnection(file, null)
                        }
                        else -> {}
                    }
                }
                call.respond(HttpStatusCode.Created)
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
            HttpServerManager.clientIpCache[clientId] = call.request.origin.remoteHost
            call.respond(HttpStatusCode.NoContent)
        }

        webSocket("/") {
            val q = call.request.queryParameters
            val clientId = q["cid"] ?: ""
            if (clientId.isEmpty()) {
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
                                val hash = CryptoHelper.sha512(HttpServerManager.password.toByteArray())
                                val token = HttpServerManager.hashToToken(hash)
                                val decryptedBytes = CryptoHelper.aesDecrypt(token, frame.readBytes())
                                if (decryptedBytes != null) {
                                    r = Json.decodeFromString<AuthRequest>(decryptedBytes.decodeToString())
                                }
                                if (r?.password == hash) {
                                    send(CryptoHelper.aesEncrypt(token, JsonHelper.jsonEncode(AuthResponse(AuthStatus.PENDING))))
                                    sendEvent(ConfirmToAcceptLoginEvent(this, clientId, r.browserName, r.browserVersion, r.osName, r.osVersion, r.isMobile))
                                } else {
                                    close(CloseReason(CloseReason.Codes.TRY_AGAIN_LATER, "invalid_password"))
                                }
                            } else {
                                val token = HttpServerManager.tokenCache[clientId]
                                if (token != null) {
                                    val decryptedBytes = CryptoHelper.aesDecrypt(token, frame.readBytes())
                                    if (decryptedBytes != null) {
                                        LogCat.d("add session ${session.id}, ts: ${decryptedBytes.decodeToString()}")
                                        HttpServerManager.wsSessions.add(session)
                                    } else {
                                        close(CloseReason(CloseReason.Codes.TRY_AGAIN_LATER, "invalid_request"))
                                    }
                                } else {
                                    close(CloseReason(CloseReason.Codes.TRY_AGAIN_LATER, "invalid_request"))
                                }
                            }
                        }
                        else -> {}
                    }
                }
            } catch (e: Exception) {
            } finally {
                LogCat.d("remove session ${session.id}")
                HttpServerManager.wsSessions.removeIf { it.id == session.id }
            }
        }
    }
    install(SXGraphQL) {
        init()
    }
}