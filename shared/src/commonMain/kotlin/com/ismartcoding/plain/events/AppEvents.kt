package com.ismartcoding.plain.events

import com.ismartcoding.plain.data.DNearbyDevice
import com.ismartcoding.plain.data.DPairingRequest
import com.ismartcoding.plain.db.DChat
import com.ismartcoding.plain.enums.ActionSourceType
import com.ismartcoding.plain.enums.ActionType
import com.ismartcoding.plain.enums.AudioAction
import com.ismartcoding.plain.enums.ExportFileType
import com.ismartcoding.plain.lib.channel.ChannelEvent
import com.ismartcoding.plain.ui.models.FolderOption

data class NearbyDeviceFoundEvent(val device: DNearbyDevice) : ChannelEvent()

// Pairing events
data class PairingRequestReceivedEvent(val request: DPairingRequest) : ChannelEvent()
data class PairingSuccessEvent(val deviceId: String, val deviceName: String, val deviceIp: String, val key: String) : ChannelEvent()
data class PairingFailedEvent(val deviceId: String, val reason: String) : ChannelEvent()
data class PairingCanceledEvent(val fromId: String) : ChannelEvent()

class FolderKanbanSelectEvent(val data: FolderOption) : ChannelEvent()

class StartHttpServerEvent : ChannelEvent()

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
