package com.ismartcoding.plain.discover

import com.ismartcoding.plain.api.RustContentApi
import com.ismartcoding.plain.chat.peer.PeerCacher
import com.ismartcoding.plain.events.EventType
import com.ismartcoding.plain.events.WebSocketEvent
import com.ismartcoding.plain.lib.coIO
import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.lib.mdns.MdnsPacketDirection
import com.ismartcoding.plain.lib.mdns.MdnsPacketLog
import com.ismartcoding.plain.lib.mdns.MdnsServiceSnapshot
import com.ismartcoding.plain.lib.sendEvent
import com.ismartcoding.plain.ui.models.NearbyViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.*
import kotlin.concurrent.Volatile

object RustMdnsRuntime {
    const val EVENT_UPDATED = 10001
    private class Command(val body: JsonObject, val reply: CompletableDeferred<JsonObject>)
    private val commands = Channel<Command>(64)
    private val projection = Mutex()
    @Volatile private var current = JsonObject(emptyMap())
    val running: Boolean get() = current["running"]?.jsonPrimitive?.booleanOrNull == true
    val scanning: Boolean get() = current["scanning"]?.jsonPrimitive?.booleanOrNull == true

    init {
        coIO {
            for (command in commands) {
                try {
                    val row = call(command.body)
                    apply(row)
                    command.reply.complete(row)
                } catch (cancelled: CancellationException) {
                    command.reply.cancel(cancelled)
                    throw cancelled
                } catch (error: Exception) {
                    command.reply.completeExceptionally(error)
                    LogCat.e("Rust mDNS control", error)
                }
            }
        }
    }

    private fun enqueue(action: String, enabled: Boolean? = null, token: String? = null): CompletableDeferred<JsonObject> {
        val reply = CompletableDeferred<JsonObject>()
        val body = buildJsonObject { put("action", action); enabled?.let { put("enabled", it) }; token?.let { put("token", it) } }
        if (!commands.trySend(Command(body, reply)).isSuccess) {
            val error = IllegalStateException("mDNS control queue is full")
            reply.completeExceptionally(error)
            LogCat.e("Rust mDNS control", error)
        }
        return reply
    }
    fun request(action: String) { enqueue(action) }
    suspend fun control(action: String): JsonObject = enqueue(action).await()
    suspend fun capture(enabled: Boolean) = enqueue("capture", enabled).await()
    fun stopCapture() { enqueue("capture", false) }
    suspend fun debugStart(token: String) = enqueue("debugStart", token = token).await()
    fun debugStop(token: String) { enqueue("debugStop", token = token) }
    suspend fun snapshot(): JsonObject = call(buildJsonObject { put("action", "snapshot") }).also { apply(it) }
    private suspend fun call(body: JsonObject) = RustContentApi.postJson("chat/mdns", body).getValue("result").jsonObject

    private suspend fun apply(row: JsonObject) = projection.withLock {
        val revision = row.getValue("revision").jsonPrimitive.long
        if (row["runtimeId"] == current["runtimeId"] && revision < (current["revision"]?.jsonPrimitive?.long ?: -1)) return@withLock
        val previous = scanning
        current = row
        NearbyViewModel.isDiscovering.value = scanning
        if (previous != scanning) {
            sendEvent(WebSocketEvent(if (scanning) EventType.NEARBY_DISCOVERY_STARTED else EventType.NEARBY_DISCOVERY_STOPPED, "{}"))
        }
    }
    suspend fun refresh(payload: String? = null) {
        val row = snapshot()
        if (payload == null) return
        val event = Json.parseToJsonElement(payload).jsonObject
        if (event["runtimeId"] != row["runtimeId"]) return
        val peer = event["peerId"]?.jsonPrimitive?.contentOrNull ?: return
        PeerCacher.load()
        if (event["online"]?.jsonPrimitive?.booleanOrNull != true) return
        val revision = event.getValue("revision").jsonPrimitive.long
        if (row.getValue("revision").jsonPrimitive.long != revision) return
        val latest = snapshot()
        if (latest["runtimeId"] == event["runtimeId"] && latest.getValue("revision").jsonPrimitive.long == revision && latest.getValue("scanning").jsonPrimitive.boolean) {
            com.ismartcoding.plain.chat.peer.PeerStatusProjection.refresh()
        }
    }
    fun services(row: JsonObject): List<MdnsServiceSnapshot> = row.getValue("services").jsonArray.map {
        val service = it.jsonObject
        fun text(key: String) = service.getValue(key).jsonPrimitive.content
        fun strings(key: String) = service.getValue(key).jsonArray.map { value -> value.jsonPrimitive.content }
        MdnsServiceSnapshot(text("serviceType"), text("instanceName"), text("instanceFqdn"), text("hostname"), service.getValue("port").jsonPrimitive.int, strings("txtRecords"), strings("ips") + strings("ipv6"), service.getValue("complete").jsonPrimitive.boolean)
    }
    fun packets(row: JsonObject, inbound: Boolean): List<MdnsPacketLog> = row.getValue(if (inbound) "packetsIn" else "packetsOut").jsonArray.map {
        val packet = it.jsonObject
        fun text(key: String) = packet.getValue(key).jsonPrimitive.content
        fun int(key: String) = packet.getValue(key).jsonPrimitive.int
        MdnsPacketLog(packet.getValue("time").jsonPrimitive.long, MdnsPacketDirection.valueOf(text("direction")), text("srcIp"), int("srcPort"), text("dstIp"), int("dstPort"), int("size"), text("summary"), text("detail"))
    }
}
