package com.ismartcoding.plain.events

import com.ismartcoding.plain.ai.ImageIndexProgressEvent
import com.ismartcoding.plain.ai.ImageSearchStatusChangedEvent
import com.ismartcoding.plain.ble.PairingTransport
import com.ismartcoding.plain.chat.ChatManager
import com.ismartcoding.plain.data.DNearbyDevice
import com.ismartcoding.plain.data.DPairingRequest
import com.ismartcoding.plain.db.DChat
import com.ismartcoding.plain.discover.LANDiscoverManager
import com.ismartcoding.plain.enums.ActionSourceType
import com.ismartcoding.plain.enums.ActionType
import com.ismartcoding.plain.enums.AudioAction
import com.ismartcoding.plain.enums.ExportFileType
import com.ismartcoding.plain.enums.HttpServerState
import com.ismartcoding.plain.enums.PickFileTag
import com.ismartcoding.plain.enums.PickFileType
import com.ismartcoding.plain.features.BookmarkHelper
import com.ismartcoding.plain.features.bluetooth.client.BluetoothPermissionResultEvent
import com.ismartcoding.plain.features.feed.FeedWorkerStatus
import com.ismartcoding.plain.helpers.JsonHelper.jsonEncode
import com.ismartcoding.plain.helpers.coIO
import com.ismartcoding.plain.helpers.coMain
import com.ismartcoding.plain.lib.channel.Channel
import com.ismartcoding.plain.lib.channel.ChannelEvent
import com.ismartcoding.plain.lib.channel.sendEvent
import com.ismartcoding.plain.platform.Permission
import com.ismartcoding.plain.platform.audioPause
import com.ismartcoding.plain.platform.audioPlay
import com.ismartcoding.plain.platform.buildImageSearchStatus
import com.ismartcoding.plain.platform.cancelImageModelDownload
import com.ismartcoding.plain.platform.cancelUpdateDownloadAsync
import com.ismartcoding.plain.platform.disableImageSearchAsync
import com.ismartcoding.plain.platform.downloadUpdateAsync
import com.ismartcoding.plain.platform.enableImageSearchAsync
import com.ismartcoding.plain.platform.isBluetoothAdvertiseReady
import com.ismartcoding.plain.platform.restartAudioIfPlaying
import com.ismartcoding.plain.platform.sendChatMessageNotification
import com.ismartcoding.plain.platform.setBluetoothCanContinue
import com.ismartcoding.plain.platform.startHttpServerService
import com.ismartcoding.plain.platform.startMmsPolling
import com.ismartcoding.plain.ui.models.FolderOption
import com.ismartcoding.plain.web.AuthRequest
import com.ismartcoding.plain.web.WsSessionHandle
import com.ismartcoding.plain.web.models.toModel
import com.ismartcoding.plain.web.websocket.WebSocketHelper
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

data class NearbyDeviceFoundEvent(val device: DNearbyDevice) : ChannelEvent()

/**
 * A web login request that needs user confirmation. The [session] handle is
 * platform-agnostic so the event can flow through commonMain business logic.
 */
class ConfirmToAcceptLoginEvent(
    val session: WsSessionHandle,
    val clientId: String,
    val request: AuthRequest,
) : ChannelEvent()

// Pairing events
data class PairingRequestReceivedEvent(val request: DPairingRequest) : ChannelEvent()
data class PairingSuccessEvent(val deviceId: String, val deviceName: String, val deviceIp: String, val key: String) : ChannelEvent()
data class PairingFailedEvent(val deviceId: String, val reason: String) : ChannelEvent()
data class PairingCanceledEvent(val fromId: String) : ChannelEvent()

class FolderKanbanSelectEvent(val data: FolderOption) : ChannelEvent()

class StartHttpServerEvent : ChannelEvent()

class HttpServerStateChangedEvent(val state: HttpServerState) : ChannelEvent()

class RestartAppEvent : ChannelEvent()

class FetchLinkPreviewsEvent(val chat: DChat) : ChannelEvent()

class FetchBookmarkMetadataEvent(val bookmarkId: String, val url: String) : ChannelEvent()

class WindowFocusChangedEvent(val hasFocus: Boolean) : ChannelEvent()

class DeleteChatItemViewEvent(val id: String) : ChannelEvent()

/** Fired when a channel invite is received from a remote peer. UI shows accept/decline dialog. */
data class ChannelInviteReceivedEvent(
    val channelId: String,
    val channelName: String,
    val ownerPeerId: String,
    val ownerPeerName: String,
) : ChannelEvent()

/** Fired when the channel owner cancels a pending invite (i.e. removes us before we accept).
 *  The auto-opened [com.ismartcoding.plain.ui.nav.Routing.ChannelInviteRequest] page pops
 *  itself when it sees this event for the matching channel. */
data class ChannelInviteCanceledEvent(
    val channelId: String,
    val ownerPeerId: String,
) : ChannelEvent()

class ExportFileEvent(val type: ExportFileType, val fileName: String) : ChannelEvent()

class ExportFileResultEvent(val type: ExportFileType, val uri: String) : ChannelEvent()

class PickFileEvent(val tag: PickFileTag, val type: PickFileType, val multiple: Boolean) : ChannelEvent()

class PickFileResultEvent(val tag: PickFileTag, val type: PickFileType, val uris: Set<String>) : ChannelEvent()

class FeedStatusEvent(val feedId: String, val status: FeedWorkerStatus) : ChannelEvent()

class ActionEvent(val source: ActionSourceType, val action: ActionType, val ids: Set<String>, val extra: Any? = null) : ChannelEvent()

class AudioActionEvent(val action: AudioAction) : ChannelEvent()

class IgnoreBatteryOptimizationEvent : ChannelEvent()
class PowerConnectedEvent : ChannelEvent()
class PowerDisconnectedEvent : ChannelEvent()
class WebRequestReceivedEvent : ChannelEvent()
data class KeepAwakeChangedEvent(val enabled: Boolean) : ChannelEvent()

class IgnoreBatteryOptimizationResultEvent : ChannelEvent()

class ClearAudioPlaylistEvent : ChannelEvent()

class DownloadUpdateEvent : ChannelEvent()
class CancelUpdateDownloadEvent : ChannelEvent()
// UpdateDownloadProgressEvent / UpdateDownloadCompleteEvent / UpdateDownloadFailedEvent
// moved to shared/.../events/UpdateDownloadEvents.kt so UpdateViewModel can pattern-match
// on them without app/-side references.

class SleepTimerEvent(val durationMs: Long) : ChannelEvent()

class CancelSleepTimerEvent : ChannelEvent()

class StartNearbyServiceEvent : ChannelEvent()
class StartNearbyDiscoveryEvent : ChannelEvent()
class StopNearbyDiscoveryEvent : ChannelEvent()

data class ChatMessageNotificationEvent(
    val targetId: String,
    val targetName: String,
    val messageText: String,
) : ChannelEvent()

/**
 * Central event dispatcher for the app. Subscribes to [Channel.sharedFlow] and
 * dispatches each event to its handler. Pure business logic lives here; platform
 * specifics (audio service, notifications, HTTP server service, MMS polling,
 * APK download, image search, BLE) are delegated to `expect fun`s in
 * `commonMain/.../platform/` so this object has zero Android dependencies.
 *
 * Call [register] once at app startup. The collect loop runs on the Main
 * dispatcher; per-event work is dispatched to the IO dispatcher as needed.
 */
object AppEvents {
    private var sleepTimerJob: Job? = null

    fun register() {
        val sharedFlow = Channel.sharedFlow
        coMain {
            sharedFlow.collect { event ->
                when (event) {
                    is BluetoothPermissionResultEvent -> {
                        setBluetoothCanContinue(true)
                    }

                    is SleepTimerEvent -> {
                        sleepTimerJob?.cancel()
                        sleepTimerJob = coIO {
                            delay(event.durationMs.milliseconds)
                            audioPause()
                        }
                    }

                    is CancelSleepTimerEvent -> {
                        sleepTimerJob?.cancel()
                        sleepTimerJob = null
                    }

                    is FetchBookmarkMetadataEvent -> {
                        coIO {
                            val updated = BookmarkHelper.fetchAndUpdateSingle(event.bookmarkId)
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
                                restartAudioIfPlaying()
                            }
                        }
                    }

                    is StartHttpServerEvent -> {
                        startHttpServerService()
                    }

                    is StartNearbyServiceEvent -> {
                        LANDiscoverManager.startReceiver()
                        if (isBluetoothAdvertiseReady()) {
                            PairingTransport.startAdvertising()
                        }
                    }

                    is StartNearbyDiscoveryEvent -> {
                        LANDiscoverManager.startPeriodicDiscovery()
                    }

                    is StopNearbyDiscoveryEvent -> {
                        LANDiscoverManager.stopPeriodicDiscovery()
                    }

                    is ImageSearchStatusChangedEvent -> {
                        sendEvent(
                            WebSocketEvent(
                                EventType.IMAGE_SEARCH_UPDATED,
                                jsonEncode(buildImageSearchStatus()),
                            ),
                        )
                    }

                    is ImageIndexProgressEvent -> {
                        sendEvent(
                            WebSocketEvent(
                                EventType.IMAGE_SEARCH_UPDATED,
                                jsonEncode(buildImageSearchStatus()),
                            ),
                        )
                    }

                    is HEnableImageSearchEvent -> {
                        coIO { enableImageSearchAsync() }
                    }

                    is HDisableImageSearchEvent -> {
                        coIO { disableImageSearchAsync() }
                    }

                    is HCancelImageModelDownloadEvent -> {
                        cancelImageModelDownload()
                    }

                    is HStartMmsPollingEvent -> {
                        startMmsPolling(event.pendingId, event.launchTimeSec, event.attachmentPaths)
                    }

                    is DownloadUpdateEvent -> {
                        downloadUpdateAsync()
                    }

                    is CancelUpdateDownloadEvent -> {
                        cancelUpdateDownloadAsync()
                    }

                    is HRetryChatItemEvent -> {
                        coIO {
                            ChatManager.resendMessage(event.item)
                        }
                    }

                    is ChatMessageNotificationEvent -> {
                        sendChatMessageNotification(
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
