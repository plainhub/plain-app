package com.ismartcoding.plain.features.dlna.receiver

/**
 * Platform-agnostic server socket abstraction for the DLNA HTTP server.
 *
 * Android wraps `java.net.ServerSocket`; iOS returns null (DLNA receiver
 * is not supported on iOS). The commonMain [DlnaReceiverEngine] drives the
 * accept loop via this interface so all HTTP routing / SOAP handling can
 * live in commonMain.
 */
interface DlnaServerSocket {
    val localPort: Int
    suspend fun accept(): DlnaClientConnection?
    fun close()
}

/**
 * A single accepted TCP connection from a DLNA control point.
 * [readHttpRequest] parses the raw HTTP request; [writeResponse] sends the
 * pre-built response string. [senderIp] is the remote IP for logging.
 */
interface DlnaClientConnection {
    val senderIp: String
    fun readHttpRequest(): DlnaHttpRequest?
    fun writeResponse(response: String)
    fun close()
}

/**
 * Parsed HTTP request from a DLNA control point.
 * [headers] keys are lowercased.
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
     * @return the raw SSDP message text, or null on timeout.
     */
    suspend fun receive(timeoutMs: Int): String?

    /** Send [message] to the multicast group. */
    fun sendMulticast(message: String)

    /** Send [message] to a specific unicast [address]:[port]. */
    fun sendUnicast(message: String, address: String, port: Int)

    fun close()
}

/**
 * Open a [DlnaServerSocket] bound to [port] with SO_REUSEADDR enabled.
 * @return the socket, or null if the port is unavailable.
 */
expect fun createDlnaServerSocket(port: Int): DlnaServerSocket?

/**
 * Create a [DlnaSsdpSocket] joined to the standard SSDP multicast group
 * (239.255.255.250:1900). On Android this also acquires a WifiManager
 * multicast lock. Returns null on platforms without multicast support.
 */
expect fun createDlnaSsdpSocket(): DlnaSsdpSocket?
