package com.ismartcoding.plain.ui.page.home

import com.ismartcoding.plain.enums.HttpServerState
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Truth table for the home service card's loading spinner
 * ([httpServerShowLoading]). Each row pins a historical bug fix:
 * - processing states spin, but since 2026-09 STARTING is only written by a
 *   running orchestration, so it always resolves (orphaned-STARTING bug);
 * - OFF + preference on spins only while a start is actually possible — with
 *   notifications blocked the wizard owns the start and the spinner would
 *   never end (cancelled-permission-permission-dialog bug);
 * - ON/ERROR never spin.
 */
class HttpServiceStateUiTest {

    @Test
    fun processingStatesAlwaysShowLoading() {
        assertTrue(httpServerShowLoading(HttpServerState.STARTING, serviceEnabled = true, canAutoStart = true))
        assertTrue(httpServerShowLoading(HttpServerState.STOPPING, serviceEnabled = true, canAutoStart = true))
        // Even with the preference off: a stop in flight is still processing.
        assertTrue(httpServerShowLoading(HttpServerState.STOPPING, serviceEnabled = false, canAutoStart = false))
    }

    @Test
    fun offWithPreferenceOnSpinsOnlyWhileStartIsPossible() {
        // Auto-restore window between the tap and the service spawning.
        assertTrue(httpServerShowLoading(HttpServerState.OFF, serviceEnabled = true, canAutoStart = true))
        // Notifications blocked: the permission wizard owns the start decision,
        // spinning here would never end.
        assertFalse(httpServerShowLoading(HttpServerState.OFF, serviceEnabled = true, canAutoStart = false))
    }

    @Test
    fun offWithPreferenceOffNeverSpins() {
        assertFalse(httpServerShowLoading(HttpServerState.OFF, serviceEnabled = false, canAutoStart = true))
        assertFalse(httpServerShowLoading(HttpServerState.OFF, serviceEnabled = false, canAutoStart = false))
    }

    @Test
    fun terminalStatesNeverSpin() {
        assertFalse(httpServerShowLoading(HttpServerState.ON, serviceEnabled = true, canAutoStart = true))
        assertFalse(httpServerShowLoading(HttpServerState.ERROR, serviceEnabled = true, canAutoStart = true))
    }
}
