package com.ismartcoding.plain.mdns

import com.ismartcoding.plain.TempData
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
    @Volatile private var serviceInfo: MdnsServiceInfo? = null
    @Volatile private var socket: MdnsSocket? = null
    @Volatile private var worker: MdnsWorkerHandle? = null

    /** Inbound-packet listeners (browser). Kept across socket restarts. */
    @Volatile private var packetListeners: List<(ByteArray, String) -> Unit> = emptyList()

    val isRunning: Boolean
        get() = worker?.isAlive == true && socket != null

    /**
     * Starts the mDNS responder. [service] advertises the PlainApp service
     * (PTR/SRV/TXT/A answers); when null the responder only answers A-record
     * queries for [mdnsHostname].
     */
    internal fun start(mdnsHostname: String, service: MdnsServiceInfo? = null): Boolean {
        val normalized = normalizeHostname(mdnsHostname)
        if (normalized.isEmpty()) {
            LogCat.e("mDNS start skipped: empty hostname")
            return false
        }
        hostname = normalized
        serviceInfo = service
        return restartSocket()
    }

    /**
     * Ensures the responder socket is up so discovery works even while the HTTP
     * service is off. When already running this keeps the current configuration.
     */
    internal fun ensureStarted(mdnsHostname: String): Boolean {
        if (isRunning) return true
        return start(mdnsHostname, serviceInfo)
    }

    fun stop() {
        tearDownSocket()
        hostname = ""
        serviceInfo = null
    }

    /**
     * Withdraws the `_plainapp` service advertisement while KEEPING the socket
     * and hostname responder alive. Called when the HTTP service stops: the
     * shared socket must survive so a running browser keeps querying, and the
     * responder keeps answering A queries for [hostname]. Use [stop] only for
     * a full teardown.
     */
    internal fun clearService() {
        serviceInfo = null
        LogCat.d("mDNS service advertisement withdrawn, socket kept for hostname/browser")
    }

    /** Recreates the socket after a network change, preserving hostname/service config. */
    internal fun restartSocket(): Boolean {
        tearDownSocket()
        if (hostname.isEmpty()) return false

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

    private fun tearDownSocket() {
        val t = worker; worker = null
        val s = socket; socket = null
        runCatching { s?.close() }
        runCatching { t?.join(300L) }
    }

    /** Registers a listener for every inbound mDNS packet; survives socket restarts. */
    internal fun addPacketListener(listener: (ByteArray, String) -> Unit) {
        if (listener !in packetListeners) packetListeners = packetListeners + listener
    }

    internal fun removePacketListener(listener: (ByteArray, String) -> Unit) {
        packetListeners = packetListeners - listener
    }

    /**
     * Sends an mDNS query through the shared socket so responses come back on
     * port 5353 (RFC 6762 §6.7 requires the source port to be 5353).
     */
    internal fun sendQuery(bytes: ByteArray) {
        val s = socket ?: return
        runCatching {
            candidateInterfaces().firstOrNull()?.let { (iface, _) -> s.setOutgoingInterface(iface.name) }
            s.send(bytes, MDNS_GROUP, MDNS_PORT)
        }.onFailure { LogCat.e("mDNS sendQuery: ${it.message}") }
    }

    private fun notifyPacketListeners(bytes: ByteArray, senderIp: String) {
        packetListeners.forEach { it(bytes, senderIp) }
    }

    private fun runLoop(s: MdnsSocket) {
        val buf = ByteArray(1500)
        while (!s.isClosed) {
            try {
                val result = s.receive(buf) ?: continue
                val senderIp = result.senderIp ?: continue
                val packet = buf.copyOf(result.length)
                notifyPacketListeners(packet, senderIp)
                val fresh = candidateInterfaces()
                if (fresh.isEmpty()) continue
                val (responseIface, localIp) = findResponseIface(senderIp, fresh)
                val response = buildResponse(packet, listOf(localIp)) ?: continue
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

    /**
     * Answers a query with the PlainApp service records when one is published,
     * otherwise falls back to the A-record hostname responder.
     *
     * The service advertisement is gated by the user's "discoverable" setting:
     * when disabled we stop announcing the PlainApp service (identity data via
     * PTR/SRV/TXT) but still answer plain hostname A queries, which leak no
     * PlainApp-specific information.
     */
    private fun buildResponse(query: ByteArray, ips: List<String>): MdnsResponse? {
        if (serviceInfo != null) {
            val serviceResponse = MdnsServiceResponseBuilder.buildResponseIfMatch(
                query,
                serviceInfo!!.copy(ips = ips),
            ) ?: return null
            val questions = MdnsPacketCodec.readQuestions(query) ?: return null
            return MdnsResponse(
                bytes = serviceResponse.bytes,
                questions = questions,
                matchedQuestions = questions,
            )
        }
        return MdnsPacketCodec.buildResponseIfMatchDetails(query, hostname, ips)
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
