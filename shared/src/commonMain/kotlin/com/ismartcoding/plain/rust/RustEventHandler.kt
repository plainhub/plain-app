package com.ismartcoding.plain.rust

import com.ismartcoding.plain.app.events.EventType
import com.ismartcoding.plain.app.events.HConfirmToAcceptLoginEvent
import com.ismartcoding.plain.app.events.HWebLoginClosedEvent
import com.ismartcoding.plain.app.events.HWebRequestReceivedEvent
import com.ismartcoding.plain.app.events.HostEvent
import com.ismartcoding.plain.app.events.HostEventData
import com.ismartcoding.plain.app.refreshHttpServerState
import com.ismartcoding.plain.features.chat.ChatCacher
import com.ismartcoding.plain.features.chat.ChatClient
import com.ismartcoding.plain.features.chat.ChatMessageReceiver
import com.ismartcoding.plain.features.chat.RustChatStore
import com.ismartcoding.plain.features.chat.channel.ChannelCacher
import com.ismartcoding.plain.features.chat.channel.ChannelSystemMessageReceiver
import com.ismartcoding.plain.features.chat.channel.channelGraphql
import com.ismartcoding.plain.features.chat.data.ChatTarget
import com.ismartcoding.plain.features.chat.download.DownloadQueue
import com.ismartcoding.plain.features.chat.peer.PeerTransportProjection
import com.ismartcoding.plain.features.chat.peer.RustPeerStore
import com.ismartcoding.plain.features.dlna.DlnaRendererState
import com.ismartcoding.plain.features.dlna.sender.RustDlnaSender
import com.ismartcoding.plain.features.files.FavoriteFoldersClient
import com.ismartcoding.plain.features.images.search.RustImageModels
import com.ismartcoding.plain.features.nearby.PairingProjection
import com.ismartcoding.plain.features.nearby.RustMdnsRuntime
import com.ismartcoding.plain.features.nearby.RustNearbyDevices
import com.ismartcoding.plain.features.sessions.WebLoginClosedFacts
import com.ismartcoding.plain.features.sessions.WebLoginCompletedFacts
import com.ismartcoding.plain.features.sessions.WebLoginRequestFacts
import com.ismartcoding.plain.features.sessions.setOnlineClientIds
import com.ismartcoding.plain.features.settings.AppUpdateDownloads
import com.ismartcoding.plain.features.shares.SharedFolderDownloadEngine
import com.ismartcoding.plain.lib.JsonHelper
import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.lib.sendEvent as sendAppEvent
import com.ismartcoding.plain.platform.notifications.sendWebLoginNotification
import com.ismartcoding.plain.rust.host.AudioEngineHost
import com.ismartcoding.plain.rust.native.RustNativeApi
import com.ismartcoding.plain.rust.native.readRustStream
import com.ismartcoding.plain.ui.app.LocalDeviceState
import com.ismartcoding.plain.ui.chat.ChatViewModel
import com.ismartcoding.plain.ui.feeds.FeedSyncUpdate
import com.ismartcoding.plain.ui.feeds.FeedWorkerState
import com.ismartcoding.plain.ui.media.actions.MediaActionProjection
import com.ismartcoding.plain.ui.notes.NotesViewModel
import com.ismartcoding.plain.ui.pomodoro.PomodoroProjection
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.*

suspend fun sendEventToRust(event: HostEvent) {
    val error = withContext(Dispatchers.IO) {
        when (val data = event.data) {
            is HostEventData.Text -> RustNativeApi.send("text", JsonHelper.jsonEncode(mapOf("type" to event.type.name, "payload" to data.value)), byteArrayOf())
            is HostEventData.Binary -> RustNativeApi.send("binary", event.type.name, data.value)
        }
    }
    // Host events are best-effort: a core that is stopped or reconnecting drops
    // them, and that must not take the caller (often a background coroutine) down.
    if (error.isNotEmpty()) LogCat.e("Send host event ${event.type.name}: $error")
}

object RustEventHandler {
    private val incoming = MutableSharedFlow<RustEvent>(extraBufferCapacity = 64)
    val events: SharedFlow<RustEvent> = incoming.asSharedFlow()

    internal fun start(scope: CoroutineScope) {
        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            events.collect { event ->
                try {
                    handleEvent(event)
                } catch (cancelled: CancellationException) { throw cancelled }
                catch (error: Exception) { LogCat.e("Handle Rust event", error) }
            }
        }
        scope.launch {
            try {
                readRustStream(
                    next = { RustNativeApi.next("events") },
                    consume = { value ->
                        try {
                            incoming.emit(JsonHelper.jsonDecode<RustEvent>(value))
                        } catch (error: SerializationException) {
                            // Skip unknown or malformed frames without closing the event stream.
                            LogCat.e("Decode Rust event", error)
                        }
                    },
                    closed = {},
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                // The core closes the stream when its host connection stops; end this
                // poller instead of letting the failure take the process down.
                LogCat.e("Rust event stream ended", error)
            }
        }
    }

    internal suspend fun handleEvent(event: RustEvent) {
        val type = event.type
        val payload = event.payload
        when (type) {
            EventType.NETWORK_CHANGED -> LocalDeviceState.refresh()
            EventType.WEB_LOGIN_COMPLETED -> {
                val facts = JsonHelper.jsonDecode<WebLoginCompletedFacts>(payload)
                val request = facts.request
                sendWebLoginNotification(request.browserName, request.browserVersion, request.osName, request.osVersion, facts.clientIp)
            }

            EventType.MEDIA_ACTION_FINISHED -> MediaActionProjection.apply(payload)
            EventType.HTTP_SERVER_STATE_CHANGED -> {
                refreshHttpServerState()
                LocalDeviceState.refresh()
            }
            EventType.PEER_STATUS_UPDATED -> {
                ChatCacher.load()
            }
            EventType.PEER_TRANSPORT_UPDATED -> PeerTransportProjection.apply(JsonHelper.jsonDecode<JsonArray>(payload))
            EventType.MDNS_UPDATED, EventType.NEARBY_DISCOVERY_STARTED, EventType.NEARBY_DISCOVERY_STOPPED -> RustMdnsRuntime.refresh()
            EventType.DLNA_RENDERER_UPDATED -> DlnaRendererState.apply(
                JsonHelper.jsonDecode<JsonObject>(payload).getValue("renderer").jsonObject)
            EventType.NEARBY_DEVICES_UPDATED -> {
                val devices = RustNearbyDevices.snapshot()
                RustNearbyDevices.apply(devices)
            }
            EventType.PAIRING_REQUEST_RECEIVED -> PairingProjection.request(payload)
            EventType.PAIRING_STATE_CHANGED -> PairingProjection.applyState(JsonHelper.jsonDecode<JsonObject>(payload))
            EventType.PAIRING_STARTED -> PairingProjection.started(payload)
            EventType.PAIRING_CANCELED -> PairingProjection.canceled(payload)
            EventType.PAIRING_SUCCESS -> PairingProjection.success(payload)
            EventType.PAIRING_FAILED -> PairingProjection.timeout(payload)
            EventType.MESSAGE_CREATED -> {
                JsonHelper.jsonDecode<List<PeerIdEvent>>(payload).forEach { value ->
                    RustChatStore.getById(value.id)?.let { item ->
                        if (item.fromId == "me") {
                            val target = if (item.channelId.isNullOrEmpty()) ChatTarget.parseId("peer:" + item.toId)
                                else ChatTarget.parseId("channel:" + item.channelId)
                            ChatViewModel.onMessagesCreated(target, listOf(item), scroll = true)
                            ChatCacher.load()
                        } else RustPeerStore.getById(item.fromId)?.let { peer ->
                            val channel = item.channelId?.takeIf { it.isNotEmpty() }?.let { channelGraphql(it) }
                            ChatMessageReceiver.applyCommitted(item, peer, channel)
                        }
                    }
                }
            }
            EventType.MESSAGE_DELETED -> {
                val target = JsonHelper.jsonDecode<String>(payload)
                if (target.startsWith("ids=")) ChatViewModel.onMessagesDeleted(target.removePrefix("ids=").split(',').filter { it.isNotEmpty() }.toSet())
                else ChatViewModel.onConversationCleared(target)
                ChatCacher.load()
            }
            EventType.CHANNELS_UPDATED -> {
                val result = JsonHelper.jsonDecode<JsonElement>(payload).jsonObject
                ChannelSystemMessageReceiver.applyCommitted(result)
            }
            EventType.MESSAGE_UPDATED -> {
                val items = JsonHelper.jsonDecode<List<PeerIdEvent>>(payload).mapNotNull { value ->
                    RustChatStore.getById(value.id)
                }
                items.forEach { ChatViewModel.update(it) }
                ChatCacher.load()
            }
            EventType.DOWNLOAD_PROGRESS -> {
                DownloadQueue.refresh()
            }
            EventType.ONLINE_CLIENTS_UPDATED -> setOnlineClientIds(
                JsonHelper.jsonDecode<Set<String>>(payload))
            EventType.PREFS_UPDATED -> {
                LocalDeviceState.refresh()
            }
            EventType.IMAGE_MODELS_UPDATED -> RustImageModels.apply(JsonHelper.jsonDecode<JsonObject>(payload))
            EventType.DLNA_SENDER_UPDATED -> RustDlnaSender.apply(JsonHelper.jsonDecode<JsonObject>(payload))
            EventType.WEB_LOGIN_REQUESTED -> {
                val facts = JsonHelper.jsonDecode<WebLoginRequestFacts>(payload)
                sendAppEvent(HConfirmToAcceptLoginEvent(
                    facts.clientId, facts.request, facts.requestId, facts.clientIp))
            }
            EventType.WEB_LOGIN_CLOSED -> {
                val facts = JsonHelper.jsonDecode<WebLoginClosedFacts>(payload)
                sendAppEvent(HWebLoginClosedEvent(facts.requestId))
            }
            EventType.WEB_REQUEST_RECEIVED -> {
                sendAppEvent(HWebRequestReceivedEvent())
            }
            EventType.SHARED_FOLDER_DOWNLOAD_UPDATED -> SharedFolderDownloadEngine.refresh()
            EventType.AUDIO_PLAYBACK_UPDATED -> AudioEngineHost.restore(JsonHelper.jsonDecode<JsonObject>(payload))
            EventType.APP_UPDATE_DOWNLOAD_UPDATED -> AppUpdateDownloads.apply(JsonHelper.jsonDecode(payload))
            EventType.FEED_SYNC_UPDATED -> FeedWorkerState.apply(JsonHelper.jsonDecode<FeedSyncUpdate>(payload))
            EventType.CONTENT_CHANGED -> {
                ChannelCacher.load()
                ChatClient.refreshLatestChats()
                try { FavoriteFoldersClient.refresh() }
                catch (cancelled: CancellationException) { throw cancelled }
                catch (error: Exception) { LogCat.e("Favorite folders refresh",error) }
                NotesViewModel.reloadAsync()
            }
            EventType.POMODORO_ACTION -> PomodoroProjection.apply(payload)
            else -> Unit
        }
    }

}

@Serializable
data class RustEvent(
    val type: EventType,
    val payload: String,
    val reconcile: Boolean = false,
)

@Serializable
private data class PeerIdEvent(val id: String)

