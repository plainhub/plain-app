package com.ismartcoding.plain.events

import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.os.PowerManager
import com.ismartcoding.lib.channel.Channel
import com.ismartcoding.lib.channel.ChannelEvent
import com.ismartcoding.lib.helpers.CoroutinesHelper.coIO
import com.ismartcoding.lib.helpers.CoroutinesHelper.coMain
import com.ismartcoding.lib.logcat.LogCat
import com.ismartcoding.plain.BuildConfig
import com.ismartcoding.plain.MainApp
import com.ismartcoding.plain.data.DNearbyDevice
import com.ismartcoding.plain.data.DPairingRequest
import com.ismartcoding.plain.db.DChat
import com.ismartcoding.plain.enums.ActionSourceType
import com.ismartcoding.plain.enums.ActionType
import com.ismartcoding.plain.enums.AudioAction
import com.ismartcoding.plain.enums.ExportFileType
import com.ismartcoding.plain.enums.HttpServerState
import com.ismartcoding.plain.enums.PickFileTag
import com.ismartcoding.plain.enums.PickFileType
import com.ismartcoding.plain.features.AudioPlayer
import com.ismartcoding.plain.features.Permission
import com.ismartcoding.plain.features.bluetooth.BluetoothFindOneEvent
import com.ismartcoding.plain.features.bluetooth.BluetoothPermissionResultEvent
import com.ismartcoding.plain.features.bluetooth.BluetoothUtil
import com.ismartcoding.plain.features.feed.FeedWorkerStatus
import com.ismartcoding.plain.chat.discover.NearbyDiscoverManager
import com.ismartcoding.plain.chat.discover.NearbyPairManager
import com.ismartcoding.plain.powerManager
import com.ismartcoding.plain.services.HttpServerService
import com.ismartcoding.plain.ui.models.FolderOption
import com.ismartcoding.plain.web.AuthRequest
import com.ismartcoding.plain.web.websocket.WebSocketHelper
import io.ktor.server.websocket.DefaultWebSocketServerSession
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull

data class NearbyDeviceFoundEvent(val device: DNearbyDevice) : ChannelEvent()

// Pairing events
data class PairingRequestReceivedEvent(val request: DPairingRequest, val fromIp: String) : ChannelEvent()
data class PairingResponseEvent(val request: DPairingRequest, val fromIp: String, val accepted: Boolean) : ChannelEvent()
data class PairingSuccessEvent(val deviceId: String, val deviceName: String, val deviceIp: String, val key: String) : ChannelEvent()
data class PairingFailedEvent(val deviceId: String, val reason: String) : ChannelEvent()
data class PairingCancelledEvent(val fromId: String) : ChannelEvent()

class FolderKanbanSelectEvent(val data: FolderOption) : ChannelEvent()

// The events raised by the app
class StartHttpServerEvent : ChannelEvent()

class HttpServerStateChangedEvent(val state: HttpServerState) : ChannelEvent()

class StartScreenMirrorEvent : ChannelEvent()

class RestartAppEvent : ChannelEvent()

class FetchLinkPreviewsEvent(val chat: DChat) : ChannelEvent()

class ConfirmDialogEvent(
    val title: String,
    val message: String,
    val confirmButton: Pair<String, () -> Unit>,
    val dismissButton: Pair<String, () -> Unit>?
) : ChannelEvent()

class LoadingDialogEvent(
    val show: Boolean,
    val message: String = ""
) : ChannelEvent()

class WindowFocusChangedEvent(val hasFocus: Boolean) : ChannelEvent()

class DeleteChatItemViewEvent(val id: String) : ChannelEvent()

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

class ExportFileEvent(val type: ExportFileType, val fileName: String) : ChannelEvent()

class ExportFileResultEvent(val type: ExportFileType, val uri: Uri) : ChannelEvent()

class ActionEvent(val source: ActionSourceType, val action: ActionType, val ids: Set<String>, val extra: Any? = null) : ChannelEvent()

class AudioActionEvent(val action: AudioAction) : ChannelEvent()

class IgnoreBatteryOptimizationEvent : ChannelEvent()
class AcquireWakeLockEvent : ChannelEvent()
class ReleaseWakeLockEvent : ChannelEvent()

class IgnoreBatteryOptimizationResultEvent : ChannelEvent()

class CancelNotificationsEvent(val ids: Set<String>) : ChannelEvent()

class ClearAudioPlaylistEvent : ChannelEvent()

class FeedStatusEvent(val feedId: String, val status: FeedWorkerStatus) : ChannelEvent()

class SleepTimerEvent(val durationMs: Long) : ChannelEvent()

class CancelSleepTimerEvent : ChannelEvent()

class StartNearbyServiceEvent : ChannelEvent()
class StartNearbyDiscoveryEvent : ChannelEvent()
class StopNearbyDiscoveryEvent : ChannelEvent()

object AppEvents {
    private lateinit var mediaPlayer: MediaPlayer
    private var sleepTimerJob: Job? = null

    val wakeLock: PowerManager.WakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "${BuildConfig.APPLICATION_ID}:http_server")

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

                    is WebSocketEvent -> {
                        coIO {
                            WebSocketHelper.sendEventAsync(event)
                        }
                    }

                    is AcquireWakeLockEvent -> {
                        coIO {
                            LogCat.d("AcquireWakeLockEvent")
                            if (!wakeLock.isHeld) {
                                wakeLock.acquire()
                            }
                        }
                    }

                    is ReleaseWakeLockEvent -> {
                        coIO {
                            LogCat.d("ReleaseWakeLockEvent")
                            if (wakeLock.isHeld) {
                                wakeLock.release()
                            }
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
                        coIO {
                            NearbyDiscoverManager.startListener()
                        }
                    }

                    is PairingResponseEvent -> {
                        coIO {
                            NearbyPairManager.respondToPairing(event.request, event.fromIp, event.accepted)
                        }
                    }

                    is StartNearbyDiscoveryEvent -> {
                        NearbyDiscoverManager.startPeriodicDiscovery()
                    }

                    is StopNearbyDiscoveryEvent -> {
                        NearbyDiscoverManager.stopPeriodicDiscovery()
                    }
                }
            }
        }
    }
}
