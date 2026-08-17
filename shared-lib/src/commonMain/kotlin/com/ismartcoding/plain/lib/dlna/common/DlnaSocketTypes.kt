package com.ismartcoding.plain.lib.dlna.common

/**
 * Parsed HTTP request from a DLNA control point.
 *
 * The HTTP control endpoints are served by the shared web server; this type
 * is the adapter the web server's [HttpCall] is converted into before being
 * handed to the DLNA HTTP router. [headers] keys are lowercased.
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
