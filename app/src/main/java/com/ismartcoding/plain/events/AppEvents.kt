package com.ismartcoding.plain.events

import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import com.ismartcoding.plain.lib.channel.Channel
import com.ismartcoding.plain.lib.channel.ChannelEvent
import com.ismartcoding.plain.lib.channel.sendEvent
import com.ismartcoding.plain.lib.helpers.CoroutinesHelper.coIO
import com.ismartcoding.plain.lib.helpers.CoroutinesHelper.coMain
import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.helpers.JsonHelper.jsonEncode
import com.ismartcoding.plain.helpers.NotificationHelper
import com.ismartcoding.plain.AndroidTempData
import com.ismartcoding.plain.MainApp
import com.ismartcoding.plain.db.DChat
import com.ismartcoding.plain.enums.ExportFileType
import com.ismartcoding.plain.enums.HttpServerState
import com.ismartcoding.plain.enums.PickFileTag
import com.ismartcoding.plain.enums.PickFileType
import com.ismartcoding.plain.audio.AudioPlayer
import com.ismartcoding.plain.features.BookmarkHelper
import com.ismartcoding.plain.features.Permission
import com.ismartcoding.plain.features.bluetooth.BluetoothFindOneEvent
import com.ismartcoding.plain.features.bluetooth.BluetoothPermissionResultEvent
import com.ismartcoding.plain.features.bluetooth.BluetoothUtil
import com.ismartcoding.plain.ai.ImageSearchManager
import com.ismartcoding.plain.ai.ImageSearchStatusChangedEvent
import com.ismartcoding.plain.ai.ImageIndexProgressEvent
import com.ismartcoding.plain.api.KtorClientFactory
import com.ismartcoding.plain.api.OkHttpClientFactory
import com.ismartcoding.plain.web.models.buildImageSearchStatus
import com.ismartcoding.plain.features.feed.FeedWorkerStatus
import com.ismartcoding.plain.discover.NearbyDiscoverManager
import com.ismartcoding.plain.chat.ChatManager
import com.ismartcoding.plain.db.DPeer
import com.ismartcoding.plain.preferences.UpdateInfoPreference
import com.ismartcoding.plain.services.HttpServerService
import com.ismartcoding.plain.web.AuthRequest
import com.ismartcoding.plain.web.models.toModel
import com.ismartcoding.plain.web.websocket.WebSocketHelper
import io.ktor.server.websocket.DefaultWebSocketServerSession
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.Request
import java.io.File

// Android-only events that depend on Android types (Uri/Parcelable/Permission/DefaultWebSocketServerSession).
// Pure events live in shared/commonMain/events/AppEvents.kt.

class HttpServerStateChangedEvent(val state: HttpServerState) : ChannelEvent()

class ConfirmToAcceptLoginEvent(
    val session: DefaultWebSocketServerSession,
    val clientId: String,
    val request: AuthRequest,
) : ChannelEvent()

class RequestPermissionsEvent(vararg val permissions: Permission) : ChannelEvent()
class PermissionsResultEvent(val map: Map<String, Boolean>) : ChannelEvent() {
    fun has(permission: Permission): Boolean {
        return map.containsKey(permission.toSysPermission())
    }
}

class PickFileEvent(val tag: PickFileTag, val type: PickFileType, val multiple: Boolean) : ChannelEvent()

class PickFileResultEvent(val tag: PickFileTag, val type: PickFileType, val uris: Set<Uri>) : ChannelEvent()

class ExportFileResultEvent(val type: ExportFileType, val uri: Uri) : ChannelEvent()

class FeedStatusEvent(val feedId: String, val status: FeedWorkerStatus) : ChannelEvent()

object AppEvents {
    private lateinit var mediaPlayer: MediaPlayer
    private var sleepTimerJob: Job? = null
    private var downloadJob: Job? = null
    private val downloadHttpClient by lazy { OkHttpClientFactory.downloadClient() }

    fun register() {
        mediaPlayer = MediaPlayer()
        val sharedFlow = Channel.sharedFlow
        coMain {
            sharedFlow.collect { event ->
                when (event) {
                    is BluetoothPermissionResultEvent -> {
                        BluetoothUtil.canContinue = true
                    }

                    is BluetoothFindOneEvent -> {
                        if (BluetoothUtil.isScanning) {
                            return@collect
                        }
                        coIO {
                            withTimeoutOrNull(3000) {
                                BluetoothUtil.currentBTDevice = BluetoothUtil.findOneAsync(event.mac)
                            }
                        }

                        BluetoothUtil.stopScan()
                    }

                    is SleepTimerEvent -> {
                        sleepTimerJob?.cancel()
                        sleepTimerJob = coIO {
                            delay(event.durationMs)
                            AudioPlayer.pause()
                        }
                    }

                    is CancelSleepTimerEvent -> {
                        sleepTimerJob?.cancel()
                        sleepTimerJob = null
                    }

                    is FetchBookmarkMetadataEvent -> {
                        coIO {
                            val updated = BookmarkHelper.fetchAndUpdateSingle(MainApp.instance, event.bookmarkId)
                            if (updated != null) {
                                sendEvent(
                                    WebSocketEvent(
                                        EventType.BOOKMARK_UPDATED,
                                        jsonEncode(listOf(updated.toModel())),
                                    ),
                                )
                            }
                        }
                    }

                    is WebSocketEvent -> {
                        coIO {
                            WebSocketHelper.sendEventAsync(event)
                        }
                    }

                    is PermissionsResultEvent -> {
                        coMain {
                            if (event.map.containsKey(Permission.POST_NOTIFICATIONS.toSysPermission())) {
                                if (AudioPlayer.isPlaying()) {
                                    AudioPlayer.pause()
                                    AudioPlayer.play()
                                }
                            }
                        }
                    }

                    is StartHttpServerEvent -> {
                        var retry = 3
                        val context = MainApp.instance
                        coIO {
                            while (retry > 0) {
                                try {
                                    androidx.core.content.ContextCompat.startForegroundService(
                                        context,
                                        Intent(context, HttpServerService::class.java)
                                    )
                                    break
                                } catch (ex: Exception) {
                                    LogCat.e(ex.toString())
                                    delay(500)
                                    retry--
                                }
                            }
                        }
                    }

                    is StartNearbyServiceEvent -> {
                        NearbyDiscoverManager.start()
                    }

                    is StartNearbyDiscoveryEvent -> {
                        NearbyDiscoverManager.startPeriodicDiscovery()
                    }

                    is StopNearbyDiscoveryEvent -> {
                        NearbyDiscoverManager.stopPeriodicDiscovery()
                    }

                    is ImageSearchStatusChangedEvent -> {
                        sendEvent(
                            WebSocketEvent(
                                EventType.IMAGE_SEARCH_UPDATED,
                                jsonEncode(buildImageSearchStatus()),
                            )
                        )
                    }

                    is ImageIndexProgressEvent -> {
                        sendEvent(
                            WebSocketEvent(
                                EventType.IMAGE_SEARCH_UPDATED,
                                jsonEncode(buildImageSearchStatus()),
                            )
                        )
                    }

                    is HEnableImageSearchEvent -> {
                        coIO { ImageSearchManager.enableAsync() }
                    }

                    is HDisableImageSearchEvent -> {
                        coIO { ImageSearchManager.disableAsync() }
                    }

                    is HCancelImageModelDownloadEvent -> {
                        ImageSearchManager.cancelDownload()
                    }

                    is HStartMmsPollingEvent -> {
                        coIO {
                            val context = MainApp.instance
                            repeat(150) { // 2 s × 150 = 5 minutes max
                                delay(2000)
                                val found = context.contentResolver.query(
                                    Uri.parse("content://mms"),
                                    arrayOf("_id"),
                                    "msg_box = 2 AND m_type = 128 AND date >= ?",
                                    arrayOf(event.launchTimeSec.toString()),
                                    null
                                )?.use { cursor -> cursor.count > 0 } ?: false
                                if (found) {
                                    AndroidTempData.pendingMmsMessages.removeIf { it.id == event.pendingId }
                                    event.attachmentPaths.forEach { path ->
                                        try {
                                            java.io.File(path).delete()
                                        } catch (_: Exception) {
                                        }
                                    }
                                    sendEvent(WebSocketEvent(EventType.MMS_SENT, jsonEncode(event.pendingId)))
                                    return@coIO
                                }
                            }
                        }
                    }

                    is DownloadUpdateEvent -> {
                        downloadJob?.cancel()
                        downloadJob = coIO {
                            val context = MainApp.instance
                            val url = UpdateInfoPreference.getValueAsync().downloadUrl
                            if (url.isEmpty()) {
                                sendEvent(UpdateDownloadFailedEvent())
                                return@coIO
                            }
                            val outputFile = File(context.cacheDir, "plain-update.apk")
                            val call = downloadHttpClient.newCall(Request.Builder().url(url).build())
                            try {
                                val response = call.execute()
                                val body = response.body
                                val contentLength = body.contentLength()
                                var downloaded = 0L
                                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                                body.source().use { source ->
                                    outputFile.outputStream().use { output ->
                                        while (true) {
                                            ensureActive()
                                            val read = source.read(buffer)
                                            if (read == -1) break
                                            output.write(buffer, 0, read)
                                            downloaded += read
                                            val progress = if (contentLength > 0) {
                                ((downloaded * 100) / contentLength).toInt().coerceIn(0, 99)
                            } else 0
                                            sendEvent(UpdateDownloadProgressEvent(progress))
                                        }
                                    }
                                }
                                UpdateInfoPreference.updateAsync { it.copy(downloadedApkPath = outputFile.absolutePath) }
                                sendEvent(UpdateDownloadCompleteEvent(outputFile.absolutePath))
                            } catch (e: CancellationException) {
                                call.cancel()
                                outputFile.delete()
                                throw e
                            } catch (e: Exception) {
                                e.printStackTrace()
                                LogCat.e("APK download failed: $url, ${e.message}")
                                outputFile.delete()
                                sendEvent(UpdateDownloadFailedEvent())
                            }
                        }
                    }

                    is CancelUpdateDownloadEvent -> {
                        downloadJob?.cancel()
                        downloadJob = null
                        coIO { UpdateInfoPreference.updateAsync { it.copy(downloadedApkPath = "") } }
                    }

                    is HRetryChatItemEvent -> {
                        coIO {
                            ChatManager.resendMessage(event.item)
                        }
                    }

                    is ChatMessageNotificationEvent -> {
                        NotificationHelper.sendChatMessageNotification(
                            context = MainApp.instance,
                            targetId = event.targetId,
                            targetName = event.targetName,
                            messageText = event.messageText,
                        )
                    }
                }
            }
        }
    }
}
