package com.ismartcoding.plain.features

import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.os.PowerManager
import com.ismartcoding.lib.channel.ChannelEvent
import com.ismartcoding.lib.channel.receiveEventHandler
import com.ismartcoding.lib.channel.sendEvent
import com.ismartcoding.lib.helpers.CoroutinesHelper.coIO
import com.ismartcoding.lib.helpers.SslHelper
import com.ismartcoding.lib.logcat.LogCat
import com.ismartcoding.plain.BuildConfig
import com.ismartcoding.plain.MainApp
import com.ismartcoding.plain.enums.ActionSourceType
import com.ismartcoding.plain.enums.ActionType
import com.ismartcoding.plain.enums.AudioAction
import com.ismartcoding.plain.enums.ExportFileType
import com.ismartcoding.plain.enums.HttpServerState
import com.ismartcoding.plain.enums.PickFileTag
import com.ismartcoding.plain.enums.PickFileType
import com.ismartcoding.plain.features.feed.FeedWorkerStatus
import com.ismartcoding.plain.powerManager
import com.ismartcoding.plain.services.HttpServerService
import com.ismartcoding.plain.web.AuthRequest
import com.ismartcoding.plain.web.websocket.WebSocketEvent
import com.ismartcoding.plain.web.websocket.WebSocketHelper
import io.ktor.server.websocket.DefaultWebSocketServerSession
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class StartHttpServerEvent: ChannelEvent()

class HttpServerStateChangedEvent(val state: HttpServerState): ChannelEvent()

class StartScreenMirrorEvent: ChannelEvent()

class RestartAppEvent: ChannelEvent()

class ConfirmDialogEvent(
    val title: String,
    val message: String,
    val confirmButton: Pair<String, () -> Unit>,
    val dismissButton: Pair<String, () -> Unit>?
): ChannelEvent()

class LoadingDialogEvent(
    val show: Boolean,
    val message: String = ""
): ChannelEvent()

class WindowFocusChangedEvent(val hasFocus: Boolean): ChannelEvent()

class DeleteChatItemViewEvent(val id: String): ChannelEvent()

class ConfirmToAcceptLoginEvent(
    val session: DefaultWebSocketServerSession,
    val clientId: String,
    val request: AuthRequest,
): ChannelEvent()

class RequestPermissionsEvent(vararg val permissions: Permission): ChannelEvent()
class PermissionsResultEvent(val map: Map<String, Boolean>): ChannelEvent() {
    fun has(permission: Permission): Boolean {
        return map.containsKey(permission.toSysPermission())
    }
}

class PickFileEvent(val tag: PickFileTag, val type: PickFileType, val multiple: Boolean): ChannelEvent()

class PickFileResultEvent(val tag: PickFileTag, val type: PickFileType, val uris: Set<Uri>): ChannelEvent()

class ExportFileEvent(val type: ExportFileType, val fileName: String): ChannelEvent()

class ExportFileResultEvent(val type: ExportFileType, val uri: Uri): ChannelEvent()

class ActionEvent(val source: ActionSourceType, val action: ActionType, val ids: Set<String>, val extra: Any? = null): ChannelEvent()

class AudioActionEvent(val action: AudioAction): ChannelEvent()

class IgnoreBatteryOptimizationEvent: ChannelEvent()
class AcquireWakeLockEvent: ChannelEvent()
class ReleaseWakeLockEvent: ChannelEvent()

class IgnoreBatteryOptimizationResultEvent: ChannelEvent()

class CancelNotificationsEvent(val ids: Set<String>): ChannelEvent()

class ClearAudioPlaylistEvent: ChannelEvent()

class FeedStatusEvent(val feedId: String, val status: FeedWorkerStatus): ChannelEvent()

data class PlayAudioEvent(val uri: Uri): ChannelEvent()

data class PlayAudioResultEvent(val uri: Uri): ChannelEvent()

object AppEvents {
    private lateinit var mediaPlayer: MediaPlayer
    private var mediaPlayingUri: Uri? = null
    val wakeLock: PowerManager.WakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "${BuildConfig.APPLICATION_ID}:http_server")

    fun register() {
        mediaPlayer = MediaPlayer()
        receiveEventHandler<PlayAudioEvent> { event ->
            launch {
                try {
                    SslHelper.disableSSLCertificateChecking()
                    if (mediaPlayer.isPlaying) {
                        mediaPlayer.stop()
                        mediaPlayingUri?.let {
                            if (it.toString() != event.uri.toString()) {
                                sendEvent(PlayAudioResultEvent(it))
                            }
                        }
                    }
                    mediaPlayingUri = event.uri
                    mediaPlayer.reset()
                    mediaPlayer.setDataSource(MainApp.instance, event.uri)
                    mediaPlayer.setOnCompletionListener {
                        mediaPlayer.stop()
                        sendEvent(PlayAudioResultEvent(event.uri))
                    }
                    mediaPlayer.prepare()
                    mediaPlayer.start()
                } catch (ex: Exception) {
                    ex.printStackTrace()
                    sendEvent(PlayAudioResultEvent(event.uri))
                }
            }
        }

        receiveEventHandler<WebSocketEvent> { event ->
            coIO {
                WebSocketHelper.sendEventAsync(event)
            }
        }

        receiveEventHandler<AcquireWakeLockEvent> {
            coIO {
                LogCat.d("AcquireWakeLockEvent")
                if (!wakeLock.isHeld) {
                    wakeLock.acquire()
                }
            }
        }

        receiveEventHandler<ReleaseWakeLockEvent> {
            coIO {
                LogCat.d("ReleaseWakeLockEvent")
                if (wakeLock.isHeld) {
                    wakeLock.release()
                }
            }
        }

        receiveEventHandler<PermissionsResultEvent> { event ->
            if (event.map.containsKey(Permission.POST_NOTIFICATIONS.toSysPermission())) {
                if (AudioPlayer.isPlaying()) {
                    AudioPlayer.pause()
                    AudioPlayer.play()
                }
            }
        }

        receiveEventHandler<StartHttpServerEvent> {
            coIO {
                var retry = 3
                while (retry > 0) {
                    try {
                        val context = MainApp.instance
                        context.startService(Intent(context, HttpServerService::class.java))
                        break
                    } catch (ex: Exception) {
                        LogCat.e(ex.toString())
                        delay(500)
                        retry--
                    }
                }
            }
        }
    }
}
