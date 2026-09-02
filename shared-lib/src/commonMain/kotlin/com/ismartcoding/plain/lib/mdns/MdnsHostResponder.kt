package com.ismartcoding.plain.lib.mdns

import com.ismartcoding.plain.lib.coIO
import kotlin.concurrent.Volatile
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

/**
 * Lightweight mDNS responder — single receive socket, standards-aware reply.
 *
 * RECEIVE: One socket bound to 0.0.0.0:5353 joins 224.0.0.251 on every valid LAN
 * interface, plus a second ephemeral-port QU socket for unicast responses. Both
 * receive threads only enqueue raw datagrams into [inboundChannel]; a single
 * consumer coroutine drains it, dispatches packet listeners and answers queries.
 * This serialization guarantees [MdnsServiceBrowser.handlePacket] is a true
 * single writer and must not be broken.
 *
 * SEND: Replies are sent via the same socket so the source port is always 5353.
 * RFC 6762 §6.7 requires this — resolvers silently discard mDNS responses whose
 * source port ≠ 5353. QU/legacy-unicast queries are answered directly; ordinary
 * multicast queries are answered to 224.0.0.251:5353.
 */
object MdnsHostResponder {
    private const val MDNS_GROUP = "224.0.0.251"
    private const val MDNS_PORT = 5353

    /** How often to re-broadcast the service so neighbors whose cache expired
     *  (multicast responses get lost) still find us: every 60s, < the 120s TTL. */
    private const val REANNOUNCE_MS = 60_000L

    /** Exponential backoff for transient bring-up failures (create/bind/join). */
    private const val INITIAL_RETRY_MS = 2_000L
    private const val MAX_RETRY_MS = 32_000L

    /**
     * Diagnostics sink for the mDNS lifecycle. Defaults to `println` so logs
     * always show up (logcat "System.out" on Android); the app layer replaces
     * it with its `LogCat` so messages carry a proper tag. Set to a no-op in
     * tests that want silence.
     */
    @Volatile var logSink: ((String) -> Unit)? = { println("mDNS: $it") }

    private fun log(msg: String) {
        logSink?.invoke(msg)
    }

    @Volatile private var hostname = ""
    @Volatile private var serviceInfo: MdnsServiceInfo? = null
    @Volatile private var socket: MdnsSocket? = null
    @Volatile private var worker: MdnsWorkerHandle? = null

    /** Interface names the current [socket] has successfully joined the group on. */
    @Volatile private var joinedIfaces: Set<String> = emptySet()

    /** Periodic re-announcer, active while the responder runs. */
    @Volatile private var announceJob: Job? = null

    /** Retry-on-failure job with exponential backoff for transient bring-up errors. */
    @Volatile private var retryJob: Job? = null
    @Volatile private var retryDelayMs = INITIAL_RETRY_MS

    /** Inbound-packet listeners (browser). Kept across socket restarts. */
    @Volatile private var packetListeners: List<(ByteArray, String) -> Unit> = emptyList()

    /** Whether any external (non-local) packet reached the shared socket since the last [takeExternalMulticastSeen]. */
    @Volatile private var sawExternalMulticast = false

    /** QU (unicast-response) query socket: ephemeral port, created with the responder socket. */
    @Volatile private var quSocket: MdnsSocket? = null
    @Volatile private var quWorker: MdnsWorkerHandle? = null

    /**
     * Single pinch-point for every inbound packet (main multicast socket AND the
     * QU unicast socket both feed it). A dedicated consumer coroutine drains the
     * channel, so packet-listener dispatch and query answering are strictly
     * serialized — this is what makes [MdnsServiceBrowser.handlePacket] a true
     * single writer. Without it the two receive threads would race on the
     * browser's copy-on-write maps and drop discovered devices.
     */
    private val inboundChannel = Channel<InboundPacket>(capacity = 64)
    @Volatile private var inboundConsumer: Job? = null

    /** Starts (only once) the coroutine that processes every inbound mDNS packet. */
    private fun ensureSingleConsumer() {
        if (inboundConsumer?.isActive == true) return
        inboundConsumer = coIO {
            log("inbound consumer running")
            for (packet in inboundChannel) {
                notifyPacketListeners(packet.bytes, packet.senderIp)
                if (packet.respond) respondToPacket(packet)
            }
        }
    }

    /**
     * Reads and resets the external-multicast-seen flag. The browser polls this
     * every scan cycle: no external multicast means the receive path is dead
     * (e.g. an AP dropping cross-band multicast) and QU queries must take over.
     */
    internal fun takeExternalMulticastSeen(): Boolean {
        val seen = sawExternalMulticast
        sawExternalMulticast = false
        return seen
    }

    val isRunning: Boolean
        get() = worker?.isAlive == true && socket != null

    /**
     * Starts the mDNS responder. [service] advertises the PlainApp service
     * (PTR/SRV/TXT/A answers); when null the responder only answers A-record
     * queries for [mdnsHostname].
     */
    fun start(mdnsHostname: String, service: MdnsServiceInfo? = null): Boolean {
        val normalized = normalizeHostname(mdnsHostname)
        if (normalized.isEmpty()) {
            log("start: empty hostname (\"$mdnsHostname\")")
            return false
        }
        hostname = normalized
        serviceInfo = service
        retryDelayMs = INITIAL_RETRY_MS
        log("start hostname=$normalized service=${service?.serviceType ?: "none"}")
        return restartSocket()
    }

    /**
     * Ensures the responder socket is up so discovery works even while the HTTP
     * service is off. When already running this keeps the current configuration.
     */
    fun ensureStarted(mdnsHostname: String): Boolean {
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
    fun clearService() {
        serviceInfo = null
    }

    /**
     * Brings the responder up for the current network. Reuses the already-bound
     * socket when one exists, only joining interfaces that are missing — so a
     * network change does not tear down and rebuild (no dropped membership, no
     * churn). The socket is rebuilt only when it does not exist yet.
     */
    fun restartSocket(): Boolean {
        if (hostname.isEmpty()) {
            log("restart: abort, hostname not configured")
            return false
        }
        val candidates = candidateInterfaces()
        if (candidates.isEmpty()) {
            log("restart: abort, no LAN interfaces")
            return false
        }

        val existing = socket
        val isNew = existing == null || existing.isClosed
        val s: MdnsSocket
        if (isNew) {
            s = runCatching { createMdnsSocket() }.getOrNull() ?: run {
                log("restart: failed to create socket")
                scheduleRetry()
                return false
            }
            runCatching { s.bind(MDNS_PORT, 1000) }.getOrElse {
                log("restart: bind failed: ${it.message}")
                runCatching { s.close() }
                scheduleRetry()
                return false
            }
            joinedIfaces = emptySet()
            socket = s
            worker = startMdnsWorker("plain-mdns-responder") { receiveLoop(s, respond = true) }
        } else {
            s = existing
        }

        if (!syncMemberships(s, candidates)) {
            log("restart: no group membership on any interface")
            if (isNew) scheduleRetry()
            return false
        }

        ensureSingleConsumer()
        ensureQuSocket()
        log("listener up hostname=$hostname interfaces=${if (joinedIfaces.isEmpty()) "<default>" else joinedIfaces.joinToString(",")}")
        broadcastService()
        ensureReannounceJob()
        return true
    }

    /**
     * Joins the multicast group on every candidate interface, keeping existing
     * memberships so the socket is never rebuilt on an interface-only change.
     * Falls back to a plain (interface-less) join when no per-interface join works.
     */
    private fun syncMemberships(s: MdnsSocket, candidates: List<Pair<MdnsIface, String>>): Boolean {
        val desired = candidates.map { it.first.name }.toSet()
        val joined = joinedIfaces
        val toJoin = interfacesToJoin(desired, joined)
        val fresh = toJoin.filter { runCatching { s.joinGroup(MDNS_GROUP, it) }.isSuccess }.toSet()
        val success = desired.intersect(joined) + fresh
        joinedIfaces = success
        if (success.isEmpty()) runCatching { s.joinGroup(MDNS_GROUP, null) }
        return success.isNotEmpty()
    }

    /** Interface names that are desired but not yet joined — pure, unit-tested. */
    internal fun interfacesToJoin(desired: Set<String>, joined: Set<String>): List<String> =
        desired.filter { it !in joined }

    /** Next backoff delay after an unsuccessful attempt — pure, unit-tested. */
    internal fun nextRetryDelay(current: Long): Long = (current * 2L).coerceAtMost(MAX_RETRY_MS)

    /** Reschedules a bring-up attempt with exponential backoff (2s → 4s → … → 32s). */
    private fun scheduleRetry() {
        retryJob?.cancel()
        val waitMs = retryDelayMs
        retryJob = coIO {
            delay(waitMs)
            retryDelayMs = nextRetryDelay(waitMs)
            restartSocket()
        }
    }

    /**
     * Starts the periodic re-announcer. Broadcasts the full service info every
     * [REANNOUNCE_MS] so neighbours whose cache expired (multicast responses
     * are lossy) still discover us without waiting for their own query.
     */
    private fun ensureReannounceJob() {
        if (announceJob?.isActive == true) return
        announceJob = coIO {
            while (isActive) {
                delay(REANNOUNCE_MS)
                if (serviceInfo != null) broadcastService()
            }
        }
    }

    /**
     * Sends a gratuitous mDNS announcement (RFC 6762 §8.3). Reuses the regular
     * response builders: a PTR query yields the full service info (PTR +
     * SRV/TXT/A), an A query yields the hostname record when no service is
     * published.
     */
    fun broadcastService() {
        val s = socket ?: return
        val info = serviceInfo
        val candidates = candidateInterfaces()
        if (candidates.isEmpty()) return
        log("announce ${info?.serviceType ?: hostname} -> $MDNS_GROUP:$MDNS_PORT on ${candidates.size} iface(s)")
        candidates.forEach { (iface, ip) ->
            val announcement = buildAnnouncement(info, hostname, ip) ?: return@forEach
            MdnsPacketCapture.recordOut(ip, MDNS_PORT, MDNS_GROUP, MDNS_PORT, announcement)
            runCatching {
                s.setOutgoingInterface(iface.name)
                s.send(announcement, MDNS_GROUP, MDNS_PORT)
            }
        }
    }

    /** Builds an announcement containing only the address reachable on its outgoing interface. */
    internal fun buildAnnouncement(info: MdnsServiceInfo?, hostname: String, ip: String): ByteArray? {
        if (ip.isEmpty()) return null
        return if (info != null) {
            MdnsServiceResponseBuilder.buildResponseIfMatch(
                MdnsPacketCodec.buildPtrQuery(info.serviceType),
                info.copy(ips = listOf(ip)),
            )?.bytes
        } else {
            MdnsPacketCodec.buildResponseIfMatch(
                MdnsPacketCodec.buildQuery(hostname, MdnsPacketCodec.TYPE_A),
                hostname,
                listOf(ip),
            )
        }
    }

    private fun tearDownSocket() {
        announceJob?.cancel(); announceJob = null
        retryJob?.cancel(); retryJob = null
        joinedIfaces = emptySet()
        val t = worker; worker = null
        val s = socket; socket = null
        val qt = quWorker; quWorker = null
        val qs = quSocket; quSocket = null
        runCatching { s?.close() }
        runCatching { qs?.close() }
        runCatching { t?.join(300L) }
        runCatching { qt?.join(300L) }
    }

    /** Registers a listener for every inbound mDNS packet; survives socket restarts. */
    internal fun addPacketListener(listener: (ByteArray, String) -> Unit) {
        if (listener !in packetListeners) packetListeners = packetListeners + listener
    }

    /**
     * Sends an mDNS query through the shared socket so responses come back on
     * port 5353 (RFC 6762 §6.7 requires the source port to be 5353).
     */
    internal fun sendQuery(bytes: ByteArray) {
        val s = socket ?: return
        sendToGroup(s, bytes)
    }

    /**
     * Sends a QU (unicast-response requested, RFC 6762 §5.4) query through a
     * dedicated ephemeral-port socket. Broken APs drop cross-band multicast
     * while unicast still flows; the unicast responses then come back to this
     * exact socket — a 5353-bound socket could lose them to another
     * SO_REUSEPORT peer on the same device.
     */
    internal fun sendQuQuery(bytes: ByteArray) {
        val s = quSocket ?: return
        sendToGroup(s, bytes)
    }

    private fun sendToGroup(s: MdnsSocket, bytes: ByteArray) {
        // Send once per interface: the outgoing interface is a socket-wide
        // setting, so a single send can only leave one NIC. Picking just the
        // first candidate silently drops the query when that interface is not
        // where the peers live.
        val candidates = candidateInterfaces()
        val srcIp = candidates.firstOrNull()?.second.orEmpty()
        MdnsPacketCapture.recordOut(srcIp, MDNS_PORT, MDNS_GROUP, MDNS_PORT, bytes)
        if (candidates.isEmpty()) {
            runCatching { s.send(bytes, MDNS_GROUP, MDNS_PORT) }
            return
        }
        candidates.forEach { (iface, _) ->
            runCatching {
                s.setOutgoingInterface(iface.name)
                s.send(bytes, MDNS_GROUP, MDNS_PORT)
            }
        }
    }

    /** Best-effort: QU queries simply no-op until the socket exists. Idempotent — 
     *  kept alive across network changes so a reuse path never rebuilds it. */
    private fun ensureQuSocket() {
        val existing = quSocket
        if (existing != null && !existing.isClosed) return
        val created = runCatching {
            createMdnsSocket().apply { bind(0, 1000) }
        }.getOrNull() ?: return
        runCatching { existing?.close() }
        ensureSingleConsumer()
        quSocket = created
        quWorker = startMdnsWorker("plain-mdns-qu") { receiveLoop(created, respond = false) }
    }

    /**
     * Receive loops for both sockets are the same: enqueue every datagram for the
     * single consumer. The only difference is [respond] — the 5353 multicast socket
     * answers queries while the QU socket (not group-joined; unicast-only) just feeds
     * the shared consumer.
     */
    private fun receiveLoop(s: MdnsSocket, respond: Boolean) {
        val buf = ByteArray(1500)
        while (!s.isClosed) {
            try {
                val result = s.receive(buf) ?: continue
                val senderIp = result.senderIp ?: continue
                val length = result.length
                // Guarded so production (capture disabled) never pays for the
                // extra copy of the raw datagram.
                if (MdnsPacketCapture.enabled) MdnsPacketCapture.recordIn(senderIp, result.senderPort, buf.copyOf(length))
                inboundChannel.trySend(
                    InboundPacket(buf.copyOf(length), senderIp, result.senderPort, respond)
                )
            } catch (_: Exception) {
                if (s.isClosed) break
            } catch (_: Error) {
                // Transient JVM/native Errors (e.g. OutOfMemoryError in
                // DatagramSocket.receive under memory pressure) must not kill the
                // responder thread; back off briefly and keep receiving.
                if (s.isClosed) break
                Thread.sleep(1000)
            }
        }
    }

    private fun notifyPacketListeners(bytes: ByteArray, senderIp: String) {
        packetListeners.forEach { it(bytes, senderIp) }
    }

    /**
     * Builds and sends the response for one inbound packet. Runs on the single
     * consumer coroutine, so it never races listener dispatch or the browser.
     * QU/legacy-unicast queries are answered directly; ordinary multicast
     * queries are answered back to the group (RFC 6762 §6.7: source port 5353).
     */
    private fun respondToPacket(packet: InboundPacket) {
        val fresh = candidateInterfaces()
        if (fresh.isEmpty()) return
        val local = fresh.any { it.second == packet.senderIp }
        // Any external packet proves the multicast receive path works; the
        // browser polls this for the QU fallback.
        if (!local) sawExternalMulticast = true
        // Our own multicast packets loop back to this socket; answering them
        // would double traffic on every discovery cycle.
        if (local) return
        val (responseIface, localIp) = findResponseIface(packet.senderIp, fresh)
        val response = buildResponse(packet.bytes, listOf(localIp)) ?: return
        val useUnicast = response.unicastResponseRequested || packet.senderPort != MDNS_PORT
        val destAddress = if (useUnicast) packet.senderIp else MDNS_GROUP
        val destPort = if (useUnicast) packet.senderPort else MDNS_PORT
        val s = socket ?: return
        MdnsPacketCapture.recordOut(localIp, MDNS_PORT, destAddress, destPort, response.bytes)
        runCatching {
            if (!useUnicast) s.setOutgoingInterface(responseIface.name)
            s.send(response.bytes, destAddress, destPort)
            log("reply to ${packet.senderIp}:${packet.senderPort} -> ${if (useUnicast) "unicast $destAddress:$destPort" else "multicast $destAddress:$destPort"} (${response.bytes.size}B)")
        }
    }

    /**
     * Answers a query with the PlainApp service records when one is published,
     * otherwise falls back to the A-record hostname responder.
     *
     * The service advertisement is gated by the app layer: when disabled we
     * stop announcing the PlainApp service (identity data via PTR/SRV/TXT)
     * but still answer plain hostname A queries, which leak no
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

/** One inbound datagram queued for the single consumer coroutine. */
internal data class InboundPacket(
    val bytes: ByteArray,
    val senderIp: String,
    val senderPort: Int,
    /** True only for the 5353 multicast socket, which answers queries; the QU socket never answers. */
    val respond: Boolean,
)

/** Network interface info for subnet matching. */
internal data class MdnsIface(val name: String, val networkPrefixLength: Short)

/** Background thread/worker handle. */
internal interface MdnsWorkerHandle {
    val isAlive: Boolean
    fun join(timeoutMs: Long)
}

internal expect fun createMdnsSocket(): MdnsSocket
internal expect fun startMdnsWorker(name: String, block: () -> Unit): MdnsWorkerHandle
