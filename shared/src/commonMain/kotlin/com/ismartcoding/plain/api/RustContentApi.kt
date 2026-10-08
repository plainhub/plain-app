package com.ismartcoding.plain.api

import com.ismartcoding.plain.lib.JsonHelper

import com.ismartcoding.plain.events.*
import com.ismartcoding.plain.features.feed.FeedWorkerState
import com.ismartcoding.plain.features.feed.FeedWorkerStatus
import com.ismartcoding.plain.lib.sendEvent
import com.ismartcoding.plain.platform.*
import com.ismartcoding.plain.ui.models.NotesViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.*
import kotlin.concurrent.Volatile

/** A refusal or transport failure from the local Rust core. */
class RustApiException(message: String, cause: Throwable? = null) : Exception(message, cause)

object RustContentApi {
    private val lock = PlatformLock()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val client by lazy { createPlainHttpClient(PlainHttpClientSpec.Local()) }
    private val transferClient by lazy { createPlainHttpClient(PlainHttpClientSpec.Local(15 * 60)) }
    private val eventClient by lazy { createPeerStatusHttpClient() }
    private val statusLock = Mutex()
    private val eventSendLock = Mutex()
    private val eventSocket = MutableStateFlow<PlainWebSocketSession?>(null)
    @Volatile private var localSession: ContentApiSession? = null
    private val syncStates = MutableStateFlow<Map<String, Pair<String, String>>>(emptyMap())

    internal fun startedSession(): ContentApiSession? = localSession

    internal suspend fun transportSession(): ContentApiSession {
        start()
        return checkNotNull(localSession)
    }

    val directory: String get() = "${prefsFilePath().substringBeforeLast('/')}/rust-content"

    fun start() = lock.withLock {
        if (localSession != null) return@withLock
        com.ismartcoding.plain.preferences.SystemPrefs.ensureMasterSecret()
        val sessionToken = generateChaCha20Key()
        val port = RustCoreBridge.start("$directory/plain-content.db", sessionToken)
        localSession = ContentApiSession("http://127.0.0.1:$port", "local", sessionToken)
        RustHostApi.start(checkNotNull(localSession))
        scope.launch { collectEvents() }
    }

    suspend fun query(selection: String, session: ContentApiSession? = null): JsonObject = execute("query { $selection }", session)
    suspend fun mutate(selection: String, session: ContentApiSession? = null, longRunning: Boolean = false): JsonObject = execute("mutation { $selection }", session, longRunning)

    private suspend fun execute(document: String, session: ContentApiSession?, longRunning: Boolean = false): JsonObject {
        if (session == null) start()
        val target = session ?: checkNotNull(localSession)
        val body = JsonHelper.jsonEncode(ContentQueryRequest(document))
        val response = (if (longRunning) transferClient else client).postText("${target.baseUrl}/graphql", body, "application/json", target.headers())
        response.use {
            check(it.isOk()) { "Rust API returned HTTP ${it.status}" }
            val result = JsonHelper.jsonDecode<JsonElement>(it.bodyAsText()).jsonObject
            val errors = result["errors"] as? JsonArray
            check(errors.isNullOrEmpty()) { errors?.joinToString { error -> error.jsonObject["message"]?.jsonPrimitive?.content ?: "Rust API error" } ?: "Rust API error" }
            return result.getValue("data").jsonObject
        }
    }

    /**
     * Posts a JSON-RPC style body to the local Rust core.
     *
     * Rust refusals (login rate limits, revoked sessions, failed ECDH, internal
     * errors) are returned as [Result.failure] rather than thrown: several
     * callers run on the UI path, where an [IllegalStateException] from `check`
     * used to crash the app instead of surfacing the refusal. Callers that
     * already expect a refusal should unwrap with a message.
     */
    suspend fun postJson(path: String, body: JsonObject, longRunning: Boolean = false, session: ContentApiSession? = null): Result<JsonObject> {
        if (session == null) start()
        val target = session ?: checkNotNull(localSession)
        return (if (longRunning) transferClient else client).postText("${target.baseUrl}/$path", body.toString(), "application/json", target.headers()).use {
            val result = runCatching { JsonHelper.jsonDecode<JsonElement>(it.bodyAsText()).jsonObject }
                .getOrElse { error -> return Result.failure(RustApiException("Rust API returned an unreadable body", error)) }
            if (!it.isOk()) {
                val message = result["error"]?.jsonPrimitive?.contentOrNull ?: "Rust API returned HTTP ${it.status}"
                return Result.failure(RustApiException(message))
            }
            Result.success(result)
        }
    }

    /**
     * Convenience for callers that treat a Rust refusal as a programming error
     * (background workers, tests). UI paths should use [postJson] and handle
     * the failure, so a refusal cannot crash the app.
     */
    suspend fun postJsonOrThrow(path: String, body: JsonObject, longRunning: Boolean = false): JsonObject =
        postJson(path, body, longRunning).getOrThrow()

    suspend fun postStream(path: String, body: JsonObject): PlainResponse {
        start()
        val target = checkNotNull(localSession)
        return transferClient.postText("${target.baseUrl}/$path", body.toString(), "application/json", target.headers())
    }

    suspend fun sync(feedId: String) {
        val key = feedId.ifEmpty { "all" }
        mutate("syncFeeds(id: ${if (feedId.isEmpty()) "null" else gql(feedId)})")
        refreshSyncStates()
        val completed = withTimeout(5 * 60 * 1000L) {
            syncStates.first { it[key]?.first?.let { status -> status != "PENDING" } == true }.getValue(key)
        }
        check(completed.first == "COMPLETED") { completed.second.ifEmpty { "Feed sync failed" } }
    }

    private suspend fun refreshSyncStates() = statusLock.withLock {
        val values = query("feedSyncStates { feedId status error }")["feedSyncStates"]!!.jsonArray
        val states = values.associate { value ->
            val row = value.jsonObject
            row.getValue("feedId").jsonPrimitive.content to (row.getValue("status").jsonPrimitive.content to row.getValue("error").jsonPrimitive.content)
        }
        syncStates.value = states
        withContext(Dispatchers.Main) {
            states.forEach { (id, state) ->
                val status = FeedWorkerStatus.valueOf(state.first)
                FeedWorkerState.statusMap[id] = status
                if (state.second.isEmpty()) FeedWorkerState.errorMap.remove(id) else FeedWorkerState.errorMap[id] = state.second
                sendEvent(FeedStatusEvent(id, status))
            }
        }
    }

    suspend fun publish(event: WebSocketEvent): Int {
        start()
        return eventSendLock.withLock {
            val socket = withTimeoutOrNull(5_000) { eventSocket.filterNotNull().first() } ?: return@withLock 0
            try {
                when (val data = event.data) {
                    is WebSocketData.Text -> socket.sendText(JsonHelper.jsonEncode(ContentEventPacket(event.type.value, data.value)))
                    is WebSocketData.Binary -> {
                        val bytes = ByteArray(4 + data.value.size)
                        repeat(4) { index -> bytes[index] = (event.type.value shr (index * 8)).toByte() }
                        data.value.copyInto(bytes, 4)
                        socket.sendBinary(bytes)
                    }
                }
                com.ismartcoding.plain.features.session.onlineClientIds.value.size
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { 0 }
        }
    }

    private suspend fun collectEvents() {
        var retryMs = 500L
        while (currentCoroutineContext().isActive) {
            try {
                eventClient.webSocket(localSession!!.baseUrl.replace("http://", "ws://") + "/events", localSession!!.headers()) { socket ->
                    retryMs = 500L
                    eventSocket.value = socket
                    com.ismartcoding.plain.preferences.Prefs.refresh()
                    com.ismartcoding.plain.features.session.refreshOnlineClientIds()
                    runCatching { postJson("files/mutate", JsonHelper.jsonEncodeToElement(RecoverDeletionsRequest()).jsonObject) }
                    com.ismartcoding.plain.chat.download.DownloadQueue.refresh()
                    com.ismartcoding.plain.features.share.SharedFolderDownloadEngine.refresh()
                    com.ismartcoding.plain.discover.PairingProjection.reconcile()
                    com.ismartcoding.plain.discover.RustNearbyDevices.refresh()
                    com.ismartcoding.plain.discover.RustMdnsRuntime.refresh()
                    com.ismartcoding.plain.features.dlna.DlnaRendererState.refresh()
                    com.ismartcoding.plain.features.dlna.sender.RustDlnaSender.refresh()
                    com.ismartcoding.plain.ai.RustImageModels.refresh()
                    com.ismartcoding.plain.chat.peer.PeerTransportProjection.refresh()
                    com.ismartcoding.plain.chat.peer.PeerStatusProjection.refresh()
                    com.ismartcoding.plain.chat.peer.PeerCacher.load()
                    com.ismartcoding.plain.chat.channel.ChannelCacher.load()
                    com.ismartcoding.plain.chat.ChatCacher.load()
                    for (frame in socket.incoming) {
                        val text = frame.text ?: continue
                        val message = JsonHelper.jsonDecode<ContentEventPacket>(text)
                        val type = message.type
                        val payload = message.payload
                        when (type) {
                            EventType.PEER_STATUS_UPDATED.value -> {
                                com.ismartcoding.plain.chat.peer.PeerStatusProjection.refresh()
                                com.ismartcoding.plain.chat.peer.PeerCacher.load()
                                com.ismartcoding.plain.chat.ChatCacher.load()
                            }
                            com.ismartcoding.plain.chat.peer.PeerTransportProjection.EVENT_UPDATED -> com.ismartcoding.plain.chat.peer.PeerTransportProjection.refresh()
                            com.ismartcoding.plain.chat.peer.PeerStatusProjection.EVENT_UPDATED -> com.ismartcoding.plain.chat.peer.PeerStatusProjection.refresh(payload)
                            com.ismartcoding.plain.discover.RustMdnsRuntime.EVENT_UPDATED, EventType.NEARBY_DISCOVERY_STARTED.value, EventType.NEARBY_DISCOVERY_STOPPED.value -> com.ismartcoding.plain.discover.RustMdnsRuntime.refresh(if (type == com.ismartcoding.plain.discover.RustMdnsRuntime.EVENT_UPDATED) payload else null)
                            com.ismartcoding.plain.features.dlna.DlnaRendererState.EVENT_UPDATED -> com.ismartcoding.plain.features.dlna.DlnaRendererState.refresh()
                            EventType.NEARBY_DEVICE_FOUND.value -> com.ismartcoding.plain.discover.RustNearbyDevices.refresh(payload)
                            EventType.PAIRING_REQUEST_RECEIVED.value -> com.ismartcoding.plain.discover.PairingProjection.request(payload)
                            EventType.PAIRING_STARTED.value -> com.ismartcoding.plain.discover.PairingProjection.started(payload)
                            EventType.PAIRING_CANCELED.value -> com.ismartcoding.plain.discover.PairingProjection.canceled(payload)
                            EventType.PAIRING_SUCCESS.value -> com.ismartcoding.plain.discover.PairingProjection.success(payload)
                            EventType.PAIRING_FAILED.value -> com.ismartcoding.plain.discover.PairingProjection.timeout(payload)
                            EventType.MESSAGE_CREATED.value -> {
                                JsonHelper.jsonDecode<JsonElement>(payload).jsonArray.forEach { value ->
                                    com.ismartcoding.plain.chat.RustChatStore.getById(value.jsonObject.getValue("id").jsonPrimitive.content)?.let { item ->
                                        if (item.fromId == "me") {
                                            val target = if (item.channelId.isEmpty()) com.ismartcoding.plain.chat.data.ChatTarget.parseId("peer:" + item.toId)
                                                else com.ismartcoding.plain.chat.data.ChatTarget.parseId("channel:" + item.channelId)
                                            com.ismartcoding.plain.chat.ChatViewModel.onMessagesCreated(target, listOf(item), scroll = true)
                                            com.ismartcoding.plain.chat.ChatCacher.load()
                                        } else com.ismartcoding.plain.chat.peer.RustPeerStore.getById(item.fromId)?.let { peer ->
                                            val channel = item.channelId.takeIf { it.isNotEmpty() }?.let { com.ismartcoding.plain.chat.channel.RustChannelStore.getById(it) }
                                            com.ismartcoding.plain.chat.ChatMessageReceiver.applyCommitted(item, peer, channel)
                                        }
                                    }
                                }
                            }
                            EventType.MESSAGE_DELETED.value -> {
                                val target = JsonHelper.jsonDecode<JsonElement>(payload).jsonPrimitive.content
                                if (target.startsWith("ids=")) com.ismartcoding.plain.chat.ChatViewModel.onMessagesDeleted(target.removePrefix("ids=").split(',').filter { it.isNotEmpty() }.toSet())
                                else com.ismartcoding.plain.chat.ChatViewModel.onConversationCleared(target)
                                com.ismartcoding.plain.chat.ChatCacher.load()
                            }
                            EventType.CHANNELS_UPDATED.value -> {
                                val result = JsonHelper.jsonDecode<JsonElement>(payload).jsonObject
                                com.ismartcoding.plain.chat.channel.ChannelSystemMessageReceiver.applyCommitted(result)
                            }
                            EventType.MESSAGE_UPDATED.value -> {
                                val items = JsonHelper.jsonDecode<JsonElement>(payload).jsonArray.mapNotNull { value ->
                                    com.ismartcoding.plain.chat.RustChatStore.getById(value.jsonObject.getValue("id").jsonPrimitive.content)
                                }
                                items.forEach { com.ismartcoding.plain.chat.ChatViewModel.update(it) }
                                com.ismartcoding.plain.chat.ChatCacher.load()
                            }
                            EventType.DOWNLOAD_PROGRESS.value -> {
                                com.ismartcoding.plain.chat.download.DownloadQueue.refresh()
                            }
                            10006 -> com.ismartcoding.plain.features.session.setOnlineClientIds(
                                JsonHelper.jsonDecode<JsonElement>(payload).jsonArray.map { it.jsonPrimitive.content }.toSet())
                            10010 -> com.ismartcoding.plain.preferences.Prefs.refresh()
                            10009 -> com.ismartcoding.plain.ai.RustImageModels.apply(JsonHelper.jsonDecode<JsonObject>(payload))
                            10008 -> com.ismartcoding.plain.features.dlna.sender.RustDlnaSender.apply(JsonHelper.jsonDecode<JsonObject>(payload))
                            10007 -> {
                                sendEvent(WebRequestReceivedEvent())
                            }
                            10004 -> com.ismartcoding.plain.features.share.SharedFolderDownloadEngine.refresh()
                            EventType.CONTENT_CHANGED.value -> {
                                com.ismartcoding.plain.preferences.Prefs.refresh()
                                val channels = com.ismartcoding.plain.chat.channel.RustChannelRuntime.call(com.ismartcoding.plain.chat.channel.ChannelCommand.Snapshot)
                                com.ismartcoding.plain.chat.channel.ChannelSystemMessageReceiver.applyCommitted(channels)
                                try { com.ismartcoding.plain.features.FavoriteFolderHelper.refresh() }
                                catch (cancelled: CancellationException) { throw cancelled }
                                catch (error: Exception) { com.ismartcoding.plain.lib.logcat.LogCat.e("Favorite folders refresh",error) }
                                refreshSyncStates()
                                NotesViewModel.reloadAsync()
                                com.ismartcoding.plain.features.PomodoroHost.refresh()
                            }
                            EventType.POMODORO_ACTION.value -> {
                                com.ismartcoding.plain.features.PomodoroHost.refresh()
                            }
                        }
                    }
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { }
            finally { eventSocket.value = null; com.ismartcoding.plain.features.session.setOnlineClientIds(emptySet()) }
            delay(retryMs)
            retryMs = (retryMs * 2).coerceAtMost(5_000)
        }
    }
}
