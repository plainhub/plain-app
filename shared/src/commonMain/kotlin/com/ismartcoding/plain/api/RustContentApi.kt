package com.ismartcoding.plain.api

import com.ismartcoding.plain.events.*
import com.ismartcoding.plain.httpserver.models.toModel
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

object RustContentApi {
    private val lock = PlatformLock()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val client by lazy { createPlainHttpClient(PlainHttpClientSpec.Local()) }
    private val transferClient by lazy { createPlainHttpClient(PlainHttpClientSpec.Local(15 * 60)) }
    private val eventClient by lazy { createPeerStatusHttpClient() }
    private val statusLock = Mutex()
    private var localSession: ContentApiSession? = null
    private val syncStates = MutableStateFlow<Map<String, Pair<String, String>>>(emptyMap())

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
        val body = buildJsonObject { put("query", document) }.toString()
        val response = (if (longRunning) transferClient else client).postText("${target.baseUrl}/graphql", body, "application/json", target.headers())
        response.use {
            check(it.isOk()) { "Rust API returned HTTP ${it.status}" }
            val result = Json.parseToJsonElement(it.bodyAsText()).jsonObject
            val errors = result["errors"] as? JsonArray
            check(errors.isNullOrEmpty()) { errors?.joinToString { error -> error.jsonObject["message"]?.jsonPrimitive?.content ?: "Rust API error" } ?: "Rust API error" }
            return result.getValue("data").jsonObject
        }
    }

    suspend fun postJson(path: String, body: JsonObject, longRunning: Boolean = false): JsonObject {
        start()
        val target = checkNotNull(localSession)
        return (if (longRunning) transferClient else client).postText("${target.baseUrl}/$path", body.toString(), "application/json", target.headers()).use {
            val result = Json.parseToJsonElement(it.bodyAsText()).jsonObject
            check(it.isOk()) { result["error"]?.jsonPrimitive?.content ?: "Rust API returned HTTP ${it.status}" }
            result
        }
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

    private suspend fun collectEvents() {
        var retryMs = 500L
        while (currentCoroutineContext().isActive) {
            try {
                eventClient.webSocket(localSession!!.baseUrl.replace("http://", "ws://") + "/events", localSession!!.headers()) { socket ->
                    retryMs = 500L
                    com.ismartcoding.plain.chat.download.DownloadQueue.refresh()
                    com.ismartcoding.plain.discover.PairingProjection.reconcile()
                    com.ismartcoding.plain.chat.peer.PeerCacher.load()
                    com.ismartcoding.plain.chat.channel.ChannelCacher.load()
                    com.ismartcoding.plain.chat.ChatCacher.load()
                    for (frame in socket.incoming) {
                        frame.binary?.let { bytes ->
                            if (bytes.size >= 4) {
                                val type = (0..3).fold(0) { value, index -> value or ((bytes[index].toInt() and 255) shl (index * 8)) }
                                if (type == EventType.IMAGE_EDITOR_UPDATE.value) {
                                    com.ismartcoding.plain.httpserver.websocket.WebSocketHelper.sendEventAsync(
                                        WebSocketEvent(EventType.IMAGE_EDITOR_UPDATE, bytes.copyOfRange(4, bytes.size)))
                                }
                            }
                        }
                        val text = frame.text ?: continue
                        val message = Json.parseToJsonElement(text).jsonObject
                        val type = message.getValue("type").jsonPrimitive.int
                        val payload = message.getValue("payload").jsonPrimitive.content
                        when (type) {
                            EventType.PAIRING_FAILED.value -> com.ismartcoding.plain.discover.PairingProjection.timeout(payload)
                            EventType.MESSAGE_CREATED.value -> {
                                Json.parseToJsonElement(payload).jsonArray.forEach { value ->
                                    com.ismartcoding.plain.chat.RustChatStore.getById(value.jsonObject.getValue("id").jsonPrimitive.content)?.let { item ->
                                        com.ismartcoding.plain.chat.peer.RustPeerStore.getById(item.fromId)?.let { peer ->
                                            val channel = item.channelId.takeIf { it.isNotEmpty() }?.let { com.ismartcoding.plain.chat.channel.RustChannelStore.getById(it) }
                                            com.ismartcoding.plain.chat.ChatMessageReceiver.applyCommitted(item, peer, channel)
                                        }
                                    }
                                }
                            }
                            EventType.CHANNELS_UPDATED.value -> {
                                com.ismartcoding.plain.chat.channel.ChannelSystemMessageReceiver.applyCommitted(Json.parseToJsonElement(payload).jsonObject)
                                sendEvent(WebSocketEvent(EventType.CHANNELS_UPDATED, ""))
                            }
                            EventType.MESSAGE_UPDATED.value -> {
                                val items = Json.parseToJsonElement(payload).jsonArray.mapNotNull { value ->
                                    com.ismartcoding.plain.chat.RustChatStore.getById(value.jsonObject.getValue("id").jsonPrimitive.content)
                                }
                                items.forEach { com.ismartcoding.plain.chat.ChatViewModel.update(it) }
                                com.ismartcoding.plain.chat.ChatCacher.load()
                                if (items.isNotEmpty()) sendEvent(WebSocketEvent(EventType.MESSAGE_UPDATED,
                                    com.ismartcoding.plain.lib.JsonHelper.jsonEncode(items.map { it.toModel() })))
                            }
                            EventType.DOWNLOAD_PROGRESS.value -> {
                                com.ismartcoding.plain.chat.download.DownloadQueue.refresh()
                                sendEvent(WebSocketEvent(EventType.DOWNLOAD_PROGRESS, payload))
                            }
                            EventType.CONTENT_CHANGED.value -> {
                                try { com.ismartcoding.plain.features.FavoriteFolderHelper.refresh() }
                                catch (cancelled: CancellationException) { throw cancelled }
                                catch (error: Exception) { com.ismartcoding.plain.lib.logcat.LogCat.e("Favorite folders refresh",error) }
                                refreshSyncStates()
                                NotesViewModel.reloadAsync()
                                com.ismartcoding.plain.features.PomodoroHost.refresh()
                                sendEvent(WebSocketEvent(EventType.CONTENT_CHANGED, payload))
                            }
                            EventType.POMODORO_ACTION.value -> {
                                com.ismartcoding.plain.features.PomodoroHost.refresh()
                                sendEvent(WebSocketEvent(EventType.POMODORO_ACTION, payload))
                            }
                            EventType.BOOKMARK_UPDATED.value -> sendEvent(WebSocketEvent(EventType.BOOKMARK_UPDATED, payload))
                            EventType.FEEDS_FETCHED.value -> sendEvent(WebSocketEvent(EventType.FEEDS_FETCHED, payload))
                        }
                    }
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { }
            delay(retryMs)
            retryMs = (retryMs * 2).coerceAtMost(5_000)
        }
    }
}
