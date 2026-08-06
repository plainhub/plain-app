package com.ismartcoding.plain.features.dlna.receiver

/**
 * Parsed HTTP request from a DLNA control point.
 *
 * The HTTP control endpoints are served by the shared web server (see
 * `web/routes/DlnaRoutes.kt`); this type is the adapter the web server's
 * [HttpCall][com.ismartcoding.plain.httpserver.http.HttpCall] is converted into
 * before being handed to [DlnaHttpRouter]. [headers] keys are lowercased.
 */
data class DlnaHttpRequest(
    val method: String,
    val path: String,
    val headers: Map<String, String>,
    val body: String,
)

/**
 * Platform-agnostic SSDP multicast socket for the DLNA advertiser.
 * Handles M-SEARCH discovery and ssdp:alive/byebye notifications.
 */
interface DlnaSsdpSocket {
    /**
     * Block until a datagram is received or [timeoutMs] elapses.
     * @return the received SSDP packet (message + sender address:port), or null on timeout.
     */
    suspend fun receive(timeoutMs: Int): DlnaSsdpPacket?

    /** Send [message] to the multicast group. */
    fun sendMulticast(message: String)

    /** Send [message] to a specific unicast [address]:[port]. */
    fun sendUnicast(message: String, address: String, port: Int)

    fun close()
}

/**
 * A received SSDP datagram: the raw message text plus the sender's unicast
 * address and port, so the receiver can reply via unicast (per UPnP spec,
 * M-SEARCH responses must be unicasted to the source of the query, not
 * re-multicast to the group — control points listen on a random port and
 * cannot receive multicast sent to port 1900).
 */
data class DlnaSsdpPacket(
    val message: String,
    val sourceAddress: String,
    val sourcePort: Int,
)

/**
 * Create a [DlnaSsdpSocket] joined to the standard SSDP multicast group
 * (239.255.255.250). On Android this also acquires a WifiManager
 * multicast lock. Returns null on platforms without multicast support.
 *
 * @param bindPort local UDP port to bind to. Defaults to [DlnaSsdpMessages.SSDP_PORT]
 *   (1900) for the renderer/advertiser which must listen on the canonical port.
 *   Pass 0 (or any ephemeral port) for a scanner client that only sends
 *   M-SEARCH and receives unicast replies.
 */
expect fun createDlnaSsdpSocket(bindPort: Int = DlnaSsdpMessages.SSDP_PORT): DlnaSsdpSocket?
