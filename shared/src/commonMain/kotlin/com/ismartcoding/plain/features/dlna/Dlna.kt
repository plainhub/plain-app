package com.ismartcoding.plain.features.dlna

import com.ismartcoding.plain.features.dlna.receiver.DlnaReceiverEngine

/**
 * Start the DLNA renderer service (HTTP + SSDP advertiser).
 * Delegates to [DlnaReceiverEngine]; the only platform-specific piece
 * (SSDP multicast socket) is handled by [createDlnaSsdpSocket].
 */
fun startDlnaRenderer() = DlnaReceiverEngine.start()

/**
 * Stop the DLNA renderer service and release the server socket.
 * Delegates to [DlnaReceiverEngine].
 */
fun stopDlnaRenderer() = DlnaReceiverEngine.stop()
