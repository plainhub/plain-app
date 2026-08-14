package com.ismartcoding.plain.mdns

import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.chat.peer.PeerManager
import com.ismartcoding.plain.chat.peer.PeerStatusManager
import com.ismartcoding.plain.data.DNearbyDevice
import com.ismartcoding.plain.enums.DeviceType
import com.ismartcoding.plain.enums.DiscoveryMethod
import com.ismartcoding.plain.helpers.TimeHelper
import com.ismartcoding.plain.helpers.coIO
import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.ui.models.NearbyViewModel
import kotlin.concurrent.Volatile
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

/**
 * mDNS service browser for `_plainapp._tcp.local`.
 *
 * Flow (design doc §4.2):
 *  1. periodically send a PTR query for the service type
 *  2. parse PTR responses to learn instance names
 *  3. for each new instance send SRV + TXT (+ A) queries
 *  4. combine port / metadata / IPs into a [DNearbyDevice]
 *
 * The browser shares [MdnsHostResponder]'s socket (one bind on 5353), so its
 * queries and the responder's answers stay on the same port.
 *
 * Concurrency: [handlePacket] is the single writer (called serially from the
 * responder worker thread) and publishes immutable state via [Volatile] maps;
 * [browseOnce] / [snapshot] run on other threads and only read snapshots.
 */
internal object MdnsServiceBrowser {
    private const val DISCOVER_INTERVAL_MS = 5_000L

    /** Re-query an incomplete instance at most this often (multicast responses get lost). */
    private const val FOLLOW_UP_RETRY_MS = 10_000L

    /** Immutable mDNS info for one service instance, accumulated across packets. */
    private data class Instance(
        val instanceFqdn: String,
        val instanceName: String,
        val id: String = "",
        val port: Int = 0,
        val deviceType: DeviceType = DeviceType.OTHER,
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
        clearState()
        val l: (ByteArray, String) -> Unit = { data, _ -> handlePacket(data) }
        listener = l
        MdnsHostResponder.addPacketListener(l)
        discoverJob = coIO {
            while (isActive) {
                runCatching { browseOnce() }
                    .onFailure { LogCat.e("mDNS browse error: ${it.message}") }
                delay(DISCOVER_INTERVAL_MS)
            }
        }
        LogCat.d("mDNS browser started")
    }

    fun stop() {
        discoverJob?.cancel()
        discoverJob = null
        listener?.let { MdnsHostResponder.removePacketListener(it) }
        listener = null
        clearState()
        LogCat.d("mDNS browser stopped")
    }

    /** One-shot PTR query used by [MdnsDiscoverManager.discoverSpecificDevice]. */
    fun sendPtrQuery() {
        MdnsHostResponder.sendQuery(MdnsPacketCodec.buildPtrQuery(PLAINAPP_SERVICE_TYPE))
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

    private fun clearState() {
        instances = emptyMap()
        hostnameToInstance = emptyMap()
        srvTxtQueriedAt = emptyMap()
        aQueriedAt = emptyMap()
    }

    private fun browseOnce() {
        // Self-heal after an external socket teardown (e.g. HTTP service stop):
        // the responder keeps packet listeners, so discovery resumes seamlessly.
        MdnsHostResponder.ensureStarted(TempData.mdnsHostname)
        sendPtrQuery()
        // Follow up on instances that still lack port / metadata / IPs, re-asking
        // periodically because multicast responses can be dropped.
        val now = TimeHelper.nowMillis()
        instances.values.forEach { instance ->
            val key = instance.instanceFqdn
            if (!instance.complete && now - (srvTxtQueriedAt[key] ?: 0L) >= FOLLOW_UP_RETRY_MS) {
                srvTxtQueriedAt = srvTxtQueriedAt + (key to now)
                // buildSrvQuery/buildTxtQuery append the service type themselves —
                // pass the SHORT instance name, NOT the full FQDN (double-suffixed
                // query names never match the responder's instanceFqdn).
                MdnsHostResponder.sendQuery(MdnsPacketCodec.buildSrvQuery(instance.instanceName, PLAINAPP_SERVICE_TYPE))
                MdnsHostResponder.sendQuery(MdnsPacketCodec.buildTxtQuery(instance.instanceName, PLAINAPP_SERVICE_TYPE))
            }
            if (!instance.complete && instance.targetHostname.isNotEmpty() && now - (aQueriedAt[key] ?: 0L) >= FOLLOW_UP_RETRY_MS) {
                aQueriedAt = aQueriedAt + (key to now)
                MdnsHostResponder.sendQuery(MdnsPacketCodec.buildQuery(instance.targetHostname, MdnsPacketCodec.TYPE_A))
            }
        }
    }

    private fun handlePacket(data: ByteArray) {
        val parsed = MdnsPacketCodec.parseResponse(data) ?: return
        if (!parsed.isResponse) return

        var nextInstances = instances
        var nextHostnames = hostnameToInstance
        val touched = mutableSetOf<String>()

        for (record in parsed.allRecords) {
            when (record.type) {
                MdnsPacketCodec.TYPE_PTR -> record.ptrTarget?.let { target ->
                    findInstance(nextInstances, target)?.let { (key, instance) ->
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
                                "dv" -> updated = updated.copy(
                                    deviceType = runCatching { DeviceType.valueOf(entry.substring(eq + 1)) }
                                        .getOrDefault(DeviceType.OTHER),
                                )
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
                            nextInstances = nextInstances + (key to instance.copy(ips = instance.ips + ip))
                            touched.add(key)
                        }
                    }
                }
            }
        }

        instances = nextInstances
        hostnameToInstance = nextHostnames

        // Skip our own looped-back announcements (multicast loop is enabled on
        // purpose so multiple same-device sockets keep working) instead of
        // emitting this device into the nearby list / peer tables.
        touched.filter { key -> nextInstances[key]?.id != TempData.clientId }.forEach { key ->
            nextInstances[key]?.takeIf { it.complete }?.let { emitDevice(it) }
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

    private fun emitDevice(instance: Instance) {
        val device = DNearbyDevice(
            id = instance.id,
            name = instance.instanceName,
            ips = instance.ips.toList(),
            port = instance.port,
            deviceType = instance.deviceType,
            version = instance.version,
            platform = instance.platform,
            lastSeen = TimeHelper.now(),
            discoveryMethods = setOf(DiscoveryMethod.LAN),
        )
        coIO {
            NearbyViewModel.handleNewDevice(device)
            PeerStatusManager.setOnline(device.id, true)
            PeerManager.applyDeviceDiscovered(
                deviceId = device.id,
                ips = device.ips,
                port = device.port,
                name = device.name,
                deviceType = device.deviceType,
            )
        }
    }
}
