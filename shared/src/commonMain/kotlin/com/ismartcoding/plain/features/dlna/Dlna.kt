package com.ismartcoding.plain.features.dlna

import com.ismartcoding.plain.lib.coIO

/**
 * Start the DLNA renderer service (HTTP endpoints + SSDP advertiser).
 *
 * The engine lives in Rust; the app only asks it to start and then follows
 * the state it broadcasts.
 */
fun startDlnaRenderer() = coIO { DlnaRendererState.start() }

/** Stop the DLNA renderer service and release its resources. */
fun stopDlnaRenderer() = coIO { DlnaRendererState.stop() }
