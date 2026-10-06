package com.ismartcoding.plain.features.dlna.receiver

import com.ismartcoding.plain.lib.dlna.common.DlnaSsdpMessages
import com.ismartcoding.plain.lib.dlna.common.DlnaSsdpSocket

/**
 * Create a [DlnaSsdpSocket] joined to the standard SSDP multicast group
 * (239.255.255.250). On Android this also acquires a WifiManager
 * multicast lock. Returns null on platforms without multicast support.
 *
 * The renderer's own advertiser binds its socket in Rust; this is the
 * platform socket the sender side still needs for [com.ismartcoding.plain.features.dlna.sender.DlnaDeviceScanner]
 * to send M-SEARCH and collect the unicast replies.
 *
 * @param bindPort local UDP port to bind to. Defaults to [DlnaSsdpMessages.SSDP_PORT]
 *   (1900) for a listener that must receive on the canonical port. Pass 0 (or
 *   any ephemeral port) for a scanner client that only sends M-SEARCH and
 *   receives unicast replies.
 */
expect fun createDlnaSsdpSocket(bindPort: Int = DlnaSsdpMessages.SSDP_PORT): DlnaSsdpSocket?
