package com.ismartcoding.plain.events

import com.ismartcoding.plain.ai.HImageIndexProgressEvent
import com.ismartcoding.plain.ai.HImageSearchStatusChangedEvent
import com.ismartcoding.plain.ble.PairingTransport
import com.ismartcoding.plain.data.DPairingRequest
import com.ismartcoding.plain.discover.MdnsDiscoverManager
import com.ismartcoding.plain.enums.ActionSourceType
import com.ismartcoding.plain.enums.ActionType
import com.ismartcoding.plain.enums.AudioAction
import com.ismartcoding.plain.enums.ExportFileType
import com.ismartcoding.plain.enums.PickFileTag
import com.ismartcoding.plain.enums.PickFileType
import com.ismartcoding.plain.features.BookmarkHelper
import com.ismartcoding.plain.features.getGrantedWebPermissionsAsync
import com.ismartcoding.plain.features.bluetooth.client.BluetoothPermissionResultEvent
import com.ismartcoding.plain.features.feed.FeedWorkerStatus
import com.ismartcoding.plain.lib.JsonHelper.jsonEncode
import com.ismartcoding.plain.lib.coIO
import com.ismartcoding.plain.lib.coMain
import com.ismartcoding.plain.lib.Channel
import com.ismartcoding.plain.lib.ChannelEvent
import com.ismartcoding.plain.lib.sendEvent
import com.ismartcoding.plain.platform.Permission
import com.ismartcoding.plain.platform.audioPause
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
import com.ismartcoding.plain.ui.models.FolderOption
import com.ismartcoding.plain.features.session.AuthRequest
import com.ismartcoding.plain.api.WebSocketHelper
import com.ismartcoding.plain.platform.MediaDurationFixQueue
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

/** Native confirmation UI for a Rust-owned pending login. */
class HConfirmToAcceptLoginEvent(
    val clientId: String,
    val request: AuthRequest,
    val requestId: String,
    val clientIp: String,
) : HEvent()

// Pairing events
data class HPairingRequestReceivedEvent(val request: DPairingRequest) : HEvent()
data class HPairingSuccessEvent(val deviceId: String, val deviceName: String, val deviceIp: String, val key: String) : HEvent()
data class HPairingCanceledEvent(val fromId: String) : HEvent()

class FolderKanbanSelectEvent(val data: FolderOption) : ChannelEvent()

class StartHttpServerEvent : ChannelEvent()

/** Open the first-run service permission wizard (raised when a UI-initiated start is blocked on permissions). */
class ShowPermissionWizardEvent : ChannelEvent()


class RestartAppEvent : ChannelEvent()


class HPomodoroChangedEvent(val today: com.ismartcoding.plain.ui.page.pomodoro.PomodoroToday) : HEvent()

class FetchBookmarkMetadataEvent(val bookmarkId: String, val url: String) : ChannelEvent()

class WindowFocusChangedEvent(val hasFocus: Boolean) : ChannelEvent()


/** Fired when a channel invite is received from a remote peer. UI shows accept/decline dialog. */
data class HChannelInviteReceivedEvent(
    val channelId: String,
    val channelName: String,
    val ownerPeerId: String,
    val ownerPeerName: String,
) : HEvent()

/** Fired when the channel owner cancels a pending invite (i.e. removes us before we accept).
 *  The auto-opened [com.ismartcoding.plain.ui.nav.Routing.ChannelInviteRequest] page pops
 *  itself when it sees this event for the matching channel. */
data class HChannelInviteCanceledEvent(
    val channelId: String,
    val ownerPeerId: String,
) : HEvent()

class ExportFileEvent(val type: ExportFileType, val fileName: String) : ChannelEvent()

class ExportFileResultEvent(val type: ExportFileType, val uri: String) : ChannelEvent()

class PickFileEvent(val tag: PickFileTag, val type: PickFileType, val multiple: Boolean) : ChannelEvent()

class PickFileResultEvent(val tag: PickFileTag, val type: PickFileType, val uris: Set<String>) : ChannelEvent()

class HFeedStatusEvent(val feedId: String, val status: FeedWorkerStatus) : HEvent()

class ActionEvent(val source: ActionSourceType, val action: ActionType, val ids: Set<String>, val extra: Any? = null) : ChannelEvent()

class AudioActionEvent(val action: AudioAction) : ChannelEvent()

class IgnoreBatteryOptimizationEvent : ChannelEvent()
class PowerConnectedEvent : ChannelEvent()
class PowerDisconnectedEvent : ChannelEvent()
class HWebRequestReceivedEvent : HEvent()
data class KeepAwakeChangedEvent(val enabled: Boolean) : ChannelEvent()

data class ClipboardSyncChangedEvent(val enabled: Boolean) : ChannelEvent()

class IgnoreBatteryOptimizationResultEvent : ChannelEvent()

class ClearAudioQueueEvent : ChannelEvent()

class DownloadUpdateEvent : ChannelEvent()
class CancelUpdateDownloadEvent : ChannelEvent()
// UpdateDownloadProgressEvent / UpdateDownloadCompleteEvent / UpdateDownloadFailedEvent
// moved to shared/.../events/UpdateDownloadEvents.kt so UpdateViewModel can pattern-match
// on them without app/-side references.

class SleepTimerEvent(val durationMs: Long) : ChannelEvent()

class CancelSleepTimerEvent : ChannelEvent()

/**
 * Fired when media items with zero duration are found during list queries.
 * AppEvents handles this asynchronously to compute actual duration and update MediaStore.
 */
data class MediaDurationZeroEvent(
    val mediaType: String, // "video" or "audio"
    val items: List<MediaDurationZeroItem>,
) : ChannelEvent()

data class MediaDurationZeroItem(
    val id: String,
    val path: String,
)

class StartNearbyServiceEvent : ChannelEvent()

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
        com.ismartcoding.plain.platform.MediaDurationFixQueue.start()
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
                            BookmarkHelper.fetchAndUpdateSingle(event.bookmarkId)
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
                        // Push the new permission snapshot to web clients so they
                        // refetch the app query.
                        coIO {
                            sendEvent(WebSocketEvent(EventType.PERMISSIONS_UPDATED, jsonEncode(getGrantedWebPermissionsAsync())))
                        }
                    }

                    is StartHttpServerEvent -> {
                        startHttpServerService()
                    }

                    is StartNearbyServiceEvent -> {
                        MdnsDiscoverManager.startReceiver()
                        if (isBluetoothAdvertiseReady()) {
                            PairingTransport.startAdvertising()
                        }
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

                    is DownloadUpdateEvent -> {
                        downloadUpdateAsync()
                    }

                    is CancelUpdateDownloadEvent -> {
                        cancelUpdateDownloadAsync()
                    }

                    is ChatMessageNotificationEvent -> {
                        sendChatMessageNotification(
                            targetId = event.targetId,
                            targetName = event.targetName,
                            messageText = event.messageText,
                        )
                    }

                    is MediaDurationZeroEvent -> {
                        MediaDurationFixQueue.enqueue(event)
                    }
                }
            }
        }
    }
}
