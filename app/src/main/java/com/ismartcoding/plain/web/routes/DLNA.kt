package com.ismartcoding.plain.web.routes

import android.net.Uri
import android.os.Build
import com.ismartcoding.lib.extensions.isImageFast
import com.ismartcoding.lib.extensions.isUrl
import com.ismartcoding.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.lib.logcat.LogCat
import com.ismartcoding.plain.MainApp
import com.ismartcoding.plain.api.HttpClientManager
import com.ismartcoding.plain.features.dlna.sender.DlnaTransportController
import com.ismartcoding.plain.features.media.CastPlayer
import com.ismartcoding.plain.helpers.UrlHelper
import io.ktor.client.request.get
import io.ktor.client.statement.readRawBytes
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.EntityTagVersion
import io.ktor.http.content.LastModifiedVersion
import io.ktor.http.contentType
import io.ktor.server.request.receiveText
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.response.respondBytes
import io.ktor.server.response.respondFile
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import java.io.File
import java.io.IOException
import java.util.Date
import kotlin.text.split

fun Route.addDLNA() {
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
                    call.respondBytes(
                        r.readRawBytes(),
                        r.contentType() ?: ContentType.Application.OctetStream
                    )
                } catch (e: IOException) {
                    call.respondText(
                        "Failed to fetch data from URL: $path",
                        status = HttpStatusCode.InternalServerError
                    )
                }
            } else if (path.startsWith("content://")) {
                val bytes =
                    MainApp.instance.contentResolver.openInputStream(Uri.parse(path))?.buffered()
                        ?.use { it.readBytes() }
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
            call.respondText(
                "File is expired or does not exist. $ex",
                status = HttpStatusCode.Forbidden
            )
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
                                DlnaTransportController.setAVTransportURIAsync(
                                    device,
                                    UrlHelper.getMediaHttpUrl(current.path),
                                    current.title
                                )
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
                        LogCat.e(e.toString())
                    }
                }
            }
            call.respond(HttpStatusCode.OK)
        }
    }

}