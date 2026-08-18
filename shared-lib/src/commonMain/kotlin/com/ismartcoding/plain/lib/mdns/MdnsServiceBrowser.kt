package com.ismartcoding.plain.lib.mdns

import com.ismartcoding.plain.lib.coIO
import kotlin.concurrent.Volatile
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

/** A fully resolved service instance, emitted once id / port / IPs are known. */
class MdnsFoundDevice(
    val id: String,
    val name: String,
    val ips: List<String>,
    val port: Int,
    val deviceType: String,
    val version: String,
    val platform: String,
)

/**
 * mDNS service browser for `_plainapp._tcp.local`.
 *
 * Flow (design doc §4.2):
 *  1. periodically send a PTR query for the service type
 *  2. parse PTR responses to learn instance names
 *  3. for each new instance send SRV + TXT (+ A) queries
 *  4. combine port / metadata / IPs into a [MdnsFoundDevice] delivered via [onDevice]
 *
 * The browser shares [MdnsHostResponder]'s socket (one bind on 5353), so its
 * queries and the responder's answers stay on the same port.
 *
 * Concurrency: [handlePacket] is the single writer (called serially from the
 * responder worker thread) and publishes immutable state via [Volatile] maps;
 * [browseOnce] / [snapshot] run on other threads and only read snapshots.
 */
object MdnsServiceBrowser {
    private const val DISCOVER_INTERVAL_MS = 5_000L

    /** Re-query an incomplete instance at most this often (multicast responses get lost). */
    private const val FOLLOW_UP_RETRY_MS = 10_000L

    /** Supplied by the app layer; self-heals the responder socket with the current hostname. */
    @Volatile var hostnameProvider: (() -> String)? = null

    /** Called on the responder worker thread for every device whose data became complete. */
    @Volatile var onDevice: ((MdnsFoundDevice) -> Unit)? = null

    /** Immutable mDNS info for one service instance, accumulated across packets. */
    private data class Instance(
        val instanceFqdn: String,
        val instanceName: String,
        val id: String = "",
        val port: Int = 0,
        val deviceType: String = "",
        val version: String = "",
        val platform: String = "",
        val targetHostname: String = "",
        val ips: Set<String> = emptySet(),
        val txtRecords: List<String> = emptyList(),
    ) {
        val complete: Boolean get() = id.isNotEmpty() && port > 0 && ips.isNotEmpty()
    }

    /** Copy-on-write state; only replaced wholesale, never mutated in place. */
    @Volatile private var instances: Map<String, Instance> = emptyMap()          // instanceFqdn(lower) -> state
    @Volatile private var hostnameToInstance: Map<String, String> = emptyMap()   // targetHostname(lower) -> instanceFqdn(lower)

    /** Per-instance last follow-up query times; written only by the browse coroutine. */
    @Volatile private var srvTxtQueriedAt: Map<String, Long> = emptyMap()
    @Volatile private var aQueriedAt: Map<String, Long> = emptyMap()

    @Volatile private var discoverJob: Job? = null
    @Volatile private var listener: ((ByteArray, String) -> Unit)? = null

    val isRunning: Boolean
        get() = discoverJob?.isActive == true

    fun start() {
        if (isRunning) return
        ensureListening()
        discoverJob = coIO {
            while (isActive) {
                runCatching { browseOnce() }
                delay(DISCOVER_INTERVAL_MS)
            }
        }
    }

    /**
     * Stops the periodic scan loop only. The packet listener and accumulated
     * instance state stay installed: passive listening keeps refreshing paired
     * peers' IPs after a network change even when no page is scanning.
     */
    fun stop() {
        discoverJob?.cancel()
        discoverJob = null
    }

    /**
     * One-shot PTR query used by [com.ismartcoding.plain.lib.mdns] consumers. Safe to call
     * while periodic discovery is stopped (e.g. a failed chat send triggering
     * peer rediscovery): without a registered packet listener the PTR reply
     * would be dropped by the responder and the peer's IP/port never refresh.
     */
    fun sendPtrQuery() {
        ensureListening()
        MdnsHostResponder.sendQuery(MdnsPacketCodec.buildPtrQuery(PLAINAPP_SERVICE_TYPE))
    }

    /** Registers the packet listener so inbound responses reach [handlePacket]; idempotent. */
    fun ensureListening() {
        if (listener != null) return
        val l: (ByteArray, String) -> Unit = { data, sender -> handlePacket(data, sender) }
        listener = l
        MdnsHostResponder.addPacketListener(l)
    }

    /** Read-only snapshot of every currently-known service instance, for the mDNS debug page. */
    fun snapshot(): List<MdnsServiceSnapshot> = instances.values.map { instance ->
        MdnsServiceSnapshot(
            serviceType = PLAINAPP_SERVICE_TYPE,
            instanceName = instance.instanceName,
            instanceFqdn = instance.instanceFqdn,
            hostname = instance.targetHostname,
            port = instance.port,
            txtRecords = instance.txtRecords,
            ips = instance.ips.toList(),
            complete = instance.complete,
        )
    }.sortedBy { it.instanceFqdn }

    private fun browseOnce() {
        // Self-heal after an external socket teardown (e.g. HTTP service stop):
        // the responder keeps packet listeners, so discovery resumes seamlessly.
        hostnameProvider?.let { MdnsHostResponder.ensureStarted(it()) }
        sendPtrQuery()
        // Follow up on instances that still lack port / metadata / IPs,
        // re-asking periodically because multicast responses can be dropped.
        // Completed instances refresh from every PTR announcement, which
        // carries SRV/TXT/A in its additional section (RFC 6763 §12).
        val now = mdnsNowMillis()
        instances.values.filter { !it.complete }.forEach { instance ->
            val key = instance.instanceFqdn
            if (now - (srvTxtQueriedAt[key] ?: 0L) >= FOLLOW_UP_RETRY_MS) {
                srvTxtQueriedAt = srvTxtQueriedAt + (key to now)
                // buildSrvQuery/buildTxtQuery append the service type themselves —
                // pass the SHORT instance name, NOT the full FQDN (double-suffixed
                // query names never match the responder's instanceFqdn).
                MdnsHostResponder.sendQuery(MdnsPacketCodec.buildSrvQuery(instance.instanceName, PLAINAPP_SERVICE_TYPE))
                MdnsHostResponder.sendQuery(MdnsPacketCodec.buildTxtQuery(instance.instanceName, PLAINAPP_SERVICE_TYPE))
            }
            if (instance.targetHostname.isNotEmpty() && now - (aQueriedAt[key] ?: 0L) >= FOLLOW_UP_RETRY_MS) {
                aQueriedAt = aQueriedAt + (key to now)
                MdnsHostResponder.sendQuery(MdnsPacketCodec.buildQuery(instance.targetHostname, MdnsPacketCodec.TYPE_A))
            }
        }
    }

    private fun handlePacket(data: ByteArray, sender: String) {
        // Ignore our own looped-back packets so we don't discover ourselves
        // and re-query our own SRV/TXT records on every discovery cycle.
        if (isLocalIp(sender)) return
        val parsed = MdnsPacketCodec.parseResponse(data) ?: return
        if (!parsed.isResponse) return

        var nextInstances = instances
        var nextHostnames = hostnameToInstance
        val touched = mutableSetOf<String>()
        val discovered = mutableListOf<Instance>() // instances first seen in this packet

        for (record in parsed.allRecords) {
            when (record.type) {
                MdnsPacketCodec.TYPE_PTR -> record.ptrTarget?.let { target ->
                    findInstance(nextInstances, target)?.let { (key, instance) ->
                        if (!nextInstances.containsKey(key)) discovered.add(instance)
                        nextInstances = nextInstances + (key to instance)
                        touched.add(key)
                    }
                }
                MdnsPacketCodec.TYPE_SRV -> record.srv?.let { srv ->
                    findInstance(nextInstances, record.name)?.let { (key, instance) ->
                        val updated = instance.copy(port = srv.port, targetHostname = srv.target)
                        nextInstances = nextInstances + (key to updated)
                        if (srv.target.isNotEmpty()) {
                            nextHostnames = nextHostnames + (srv.target.lowercase() to key)
                        }
                        touched.add(key)
                    }
                }
                MdnsPacketCodec.TYPE_TXT -> record.txtStrings?.let { strings ->
                    findInstance(nextInstances, record.name)?.let { (key, instance) ->
                        var updated = instance.copy(txtRecords = strings)
                        for (entry in strings) {
                            val eq = entry.indexOf('=')
                            if (eq <= 0) continue
                            when (entry.substring(0, eq)) {
                                "id" -> updated = updated.copy(id = entry.substring(eq + 1))
                                "dv" -> updated = updated.copy(deviceType = entry.substring(eq + 1))
                                "ver" -> updated = updated.copy(version = entry.substring(eq + 1))
                                "pf" -> updated = updated.copy(platform = entry.substring(eq + 1))
                            }
                        }
                        nextInstances = nextInstances + (key to updated)
                        touched.add(key)
                    }
                }
                MdnsPacketCodec.TYPE_A -> record.ip?.let { ip ->
                    nextHostnames[record.name.lowercase()]?.let { key ->
                        nextInstances[key]?.let { instance ->
                            // An A record is authoritative for the target hostname's CURRENT
                            // address. Replace, don't accumulate — a device that changed IPs
                            // would otherwise keep its stale address in the set forever.
                            nextInstances = nextInstances + (key to instance.copy(ips = setOf(ip)))
                            touched.add(key)
                        }
                    }
                }
            }
        }

        // Immediately resolve newly discovered instances instead of waiting
        // for the next browseOnce() cycle (up to 5 s). This is the single most
        // impactful latency optimization for first device appearance.
        if (discovered.isNotEmpty()) {
            val now = mdnsNowMillis()
            val srvNext = srvTxtQueriedAt.toMutableMap()
            discovered.forEach { instance ->
                val key = instance.instanceFqdn
                // Skip when the same packet already carried SRV/TXT (instance complete).
                if (nextInstances[key]?.complete == true) return@forEach
                srvNext[key] = now
                MdnsHostResponder.sendQuery(MdnsPacketCodec.buildSrvQuery(instance.instanceName, PLAINAPP_SERVICE_TYPE))
                MdnsHostResponder.sendQuery(MdnsPacketCodec.buildTxtQuery(instance.instanceName, PLAINAPP_SERVICE_TYPE))
            }
            srvTxtQueriedAt = srvNext
        }

        instances = nextInstances
        hostnameToInstance = nextHostnames

        touched.forEach { key ->
            nextInstances[key]?.takeIf { it.complete }?.let { instance ->
                onDevice?.invoke(
                    MdnsFoundDevice(
                        id = instance.id,
                        name = instance.instanceName,
                        ips = instance.ips.toList(),
                        port = instance.port,
                        deviceType = instance.deviceType,
                        version = instance.version,
                        platform = instance.platform,
                    )
                )
            }
        }
    }

    /** Resolves [name] against [current]; null when it is not one of our service instances. */
    private fun findInstance(current: Map<String, Instance>, name: String): Pair<String, Instance>? {
        if (!name.endsWith(PLAINAPP_SERVICE_TYPE, ignoreCase = true)) return null
        val key = instanceKey(name)
        val instanceName = name.dropLast(PLAINAPP_SERVICE_TYPE.length + 1)
        if (instanceName.isEmpty()) return null
        return key to (current[key] ?: Instance(key, instanceName))
    }

    private fun instanceKey(fqdn: String): String = fqdn.lowercase()
}
