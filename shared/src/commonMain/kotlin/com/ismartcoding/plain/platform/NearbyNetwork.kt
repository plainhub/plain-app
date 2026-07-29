package com.ismartcoding.plain.platform

/**
 * Low-level UDP transport for nearby device discovery and pairing.
 *
 * Platform-specific implementation: Android uses WiFi multicast lock + java.net sockets;
 * iOS is currently a no-op (LAN multicast discovery is Android-only for now).
 */

/** Send [message] to the local-subnet multicast group (fire-and-forget). */
expect fun nearbySendMulticast(message: String)

/** Send [message] to a specific [targetIP] via unicast (fire-and-forget). */
expect fun nearbySendUnicast(message: String, targetIP: String)

/**
 * Start the multicast receiver loop.
 *
 * @param onMessage called for every incoming datagram with (message, senderIP).
 */
expect fun nearbyStartReceiver(onMessage: (message: String, senderIP: String) -> Unit)

/** Stop the multicast receiver loop. */
expect fun nearbyStopReceiver()
