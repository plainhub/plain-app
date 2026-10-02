package com.ismartcoding.plain.api

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

object RustContentApi {
    private val lock = PlatformLock()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val client by lazy { createHttpClient() }
    private val statusLock = Mutex()
    private var localSession: ContentApiSession? = null
    private val syncStates = MutableStateFlow<Map<String, Pair<String, String>>>(emptyMap())

    fun start() = lock.withLock {
        if (localSession != null) return@withLock
        val sessionToken = generateChaCha20Key()
        val directory = prefsFilePath().substringBeforeLast('/')
        val port = RustCoreBridge.start("$directory/rust-content/plain-content.db", sessionToken)
        localSession = ContentApiSession("http://127.0.0.1:$port", "local", sessionToken)
        scope.launch { collectEvents() }
    }

    suspend fun query(selection: String, session: ContentApiSession? = null): JsonObject = execute("query { $selection }", session)
    suspend fun mutate(selection: String, session: ContentApiSession? = null): JsonObject = execute("mutation { $selection }", session)

    private suspend fun execute(document: String, session: ContentApiSession?): JsonObject {
        if (session == null) start()
        val target = session ?: checkNotNull(localSession)
        val body = buildJsonObject { put("query", document) }.toString()
        val response = client.postText("${target.baseUrl}/graphql", body, "application/json", target.headers())
        response.use {
            check(it.isOk()) { "Rust API returned HTTP ${it.status}" }
            val result = Json.parseToJsonElement(it.bodyAsText()).jsonObject
            val errors = result["errors"] as? JsonArray
            check(errors.isNullOrEmpty()) { errors?.joinToString { error -> error.jsonObject["message"]?.jsonPrimitive?.content ?: "Rust API error" } ?: "Rust API error" }
            return result.getValue("data").jsonObject
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
                client.webSocket(localSession!!.baseUrl.replace("http://", "ws://") + "/events", localSession!!.headers()) { socket ->
                    retryMs = 500L
                    for (frame in socket.incoming) {
                        val text = frame.text ?: continue
                        val message = Json.parseToJsonElement(text).jsonObject
                        val type = message.getValue("type").jsonPrimitive.int
                        val payload = message.getValue("payload").jsonPrimitive.content
                        when (type) {
                            EventType.CONTENT_CHANGED.value -> {
                                refreshSyncStates()
                                NotesViewModel.reloadAsync()
                                sendEvent(WebSocketEvent(EventType.CONTENT_CHANGED, payload))
                            }
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
