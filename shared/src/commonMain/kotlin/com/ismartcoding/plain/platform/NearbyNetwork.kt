package com.ismartcoding.plain.platform

/**
 * Low-level UDP transport for nearby device discovery and pairing.
 *
 * Platform-specific implementation: Android uses WiFi multicast lock + java.net sockets;
 * iOS is currently a no-op (LAN multicast discovery is Android-only for now).
 */
expect object NearbyNetwork {
    /** Send [message] to the local-subnet multicast group (fire-and-forget). */
    fun sendMulticast(message: String)

    /** Send [message] to a specific [targetIP] via unicast (fire-and-forget). */
    fun sendUnicast(message: String, targetIP: String)

    /**
     * Start the multicast receiver loop.
     *
     * @param onMessage called for every incoming datagram with (message, senderIP).
     */
    fun startReceiver(onMessage: (message: String, senderIP: String) -> Unit)

    /** Stop the multicast receiver loop. */
    fun stopReceiver()
}
