package com.ismartcoding.plain.mdns

import com.ismartcoding.plain.lib.logcat.LogCat
import kotlin.concurrent.Volatile

/**
 * Lightweight mDNS responder — single receive socket, standards-aware reply.
 *
 * RECEIVE: One socket bound to 0.0.0.0:5353 joins 224.0.0.251 on every valid LAN interface.
 *
 * SEND: Replies are sent via the same socket so the source port is always 5353.
 * RFC 6762 §6.7 requires this — resolvers silently discard mDNS responses whose
 * source port ≠ 5353. QU/legacy-unicast queries are answered directly; ordinary
 * multicast queries are answered to 224.0.0.251:5353.
 */
object MdnsHostResponder {
    private const val MDNS_GROUP = "224.0.0.251"
    private const val MDNS_PORT = 5353

    @Volatile private var hostname = "plainapp.local"
    @Volatile private var socket: MdnsSocket? = null
    @Volatile private var worker: MdnsWorkerHandle? = null

    val isRunning: Boolean
        get() = worker?.isAlive == true && socket != null

    fun start(mdnsHostname: String): Boolean {
        val normalized = normalizeHostname(mdnsHostname)
        if (normalized.isEmpty()) {
            LogCat.e("mDNS start skipped: empty hostname")
            return false
        }
        stop()
        hostname = normalized

        val candidates = candidateInterfaces()
        if (candidates.isEmpty()) {
            LogCat.e("mDNS: no candidate interfaces found")
            return false
        }

        val s = runCatching { createMdnsSocket() }.getOrElse {
            LogCat.e("mDNS socket create failed: ${it.message}")
            return false
        }

        runCatching {
            s.bind(MDNS_PORT, 1000)
            var joined = false
            for ((iface, _) in candidates) {
                runCatching { s.joinGroup(MDNS_GROUP, iface.name) }
                    .onSuccess { joined = true; LogCat.d("mDNS joined ${iface.name}") }
                    .onFailure { LogCat.e("mDNS joinGroup ${iface.name}: ${it.message}") }
            }
            if (!joined) {
                runCatching { s.joinGroup(MDNS_GROUP, null) }
                    .onSuccess { LogCat.d("mDNS joined (default)") }
                    .onFailure { LogCat.e("mDNS joinGroup default: ${it.message}") }
            }
        }.getOrElse {
            runCatching { s.close() }
            LogCat.e("mDNS bind/join failed: ${it.message}")
            return false
        }

        socket = s
        worker = startMdnsWorker("plain-mdns-responder") { runLoop(s) }
        LogCat.d("mDNS responder started for $hostname on ${candidates.size} interface(s)")
        return true
    }

    fun stop() {
        val t = worker; worker = null
        val s = socket; socket = null
        runCatching { s?.close() }
        runCatching { t?.join(300L) }
    }

    private fun runLoop(s: MdnsSocket) {
        val buf = ByteArray(1500)
        while (!s.isClosed) {
            try {
                val result = s.receive(buf) ?: continue
                val senderIp = result.senderIp ?: continue
                val fresh = candidateInterfaces()
                if (fresh.isEmpty()) continue
                val (responseIface, localIp) = findResponseIface(senderIp, fresh)
                val response = MdnsPacketCodec.buildResponseIfMatchDetails(
                    query = buf.copyOf(result.length),
                    hostname = hostname,
                    ips = listOf(localIp),
                ) ?: continue
                val useUnicast = response.unicastResponseRequested || result.senderPort != MDNS_PORT
                val destAddress = if (useUnicast) senderIp else MDNS_GROUP
                val destPort = if (useUnicast) result.senderPort else MDNS_PORT
                runCatching {
                    if (!useUnicast) s.setOutgoingInterface(responseIface.name)
                    s.send(response.bytes, destAddress, destPort)
                    LogCat.d("mDNS reply $hostname → $localIp from $senderIp:${result.senderPort} dest=$destAddress:$destPort")
                }.onFailure { LogCat.e("mDNS send to $senderIp: ${it.message}") }
            } catch (_: Exception) {
                if (s.isClosed) break
            }
        }
    }

    internal fun normalizeHostname(value: String): String {
        val trimmed = value.trim().trim('.').lowercase()
        if (trimmed.isEmpty()) return ""
        return if (trimmed.endsWith(".local")) trimmed else "$trimmed.local"
    }
}

/** Platform-agnostic multicast UDP socket. */
internal interface MdnsSocket {
    val isClosed: Boolean
    fun bind(port: Int, timeoutMs: Int)
    fun joinGroup(groupIp: String, ifaceName: String?)
    fun setOutgoingInterface(ifaceName: String)
    fun receive(buf: ByteArray): ReceiveResult?
    fun send(bytes: ByteArray, destIp: String, destPort: Int)
    fun close()
}

/** Result of [MdnsSocket.receive]; null means timeout. */
internal class ReceiveResult(val length: Int, val senderIp: String?, val senderPort: Int)

/** Network interface info for subnet matching. */
internal data class MdnsIface(val name: String, val networkPrefixLength: Short)

/** Background thread/worker handle. */
internal interface MdnsWorkerHandle {
    val isAlive: Boolean
    fun join(timeoutMs: Long)
}

internal expect fun createMdnsSocket(): MdnsSocket
internal expect fun startMdnsWorker(name: String, block: () -> Unit): MdnsWorkerHandle
