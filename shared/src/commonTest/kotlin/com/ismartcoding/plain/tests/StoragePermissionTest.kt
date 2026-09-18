package com.ismartcoding.plain.tests

import androidx.compose.runtime.mutableStateOf
import com.ismartcoding.plain.ui.base.refreshStoragePermission
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Guards the storage-permission transition rule that all media/files pages
 * route their permission signals through. The DocsPage-on-Android-12
 * regression: the all-files-access settings screen is launched with
 * NEW_TASK and some Android versions deliver the activity result before the
 * user grants, so a signal can arrive denied; the page must still recover
 * on the next signal once the grant is real, and [onGranted] must fire
 * exactly once per false → true transition no matter how many redundant
 * signals arrive.
 */
class StoragePermissionTest {

    private class GateHarness(initial: Boolean = false) {
        val state = mutableStateOf(initial)
        var granted = false
        var loads = 0

        fun signal() = refreshStoragePermission(state, { granted }, { loads++ })
    }

    @Test
    fun `early denied signal then grant loads exactly once`() {
        // Android 12 ordering: event fires while the settings screen is still
        // up (permission not yet granted), ON_RESUME arrives after the grant.
        val gate = GateHarness()
        gate.signal()
        assertFalse(gate.state.value)
        assertEquals(0, gate.loads)

        gate.granted = true
        gate.signal()
        assertTrue(gate.state.value)
        assertEquals(1, gate.loads)
    }

    @Test
    fun `return-time grant followed by redundant signals loads exactly once`() {
        // Android 13+ ordering: PermissionsResultEvent and ON_RESUME both
        // arrive after the grant.
        val gate = GateHarness()
        gate.granted = true
        gate.signal()
        gate.signal()
        gate.signal()
        assertTrue(gate.state.value)
        assertEquals(1, gate.loads)
    }

    @Test
    fun `persistent denial never loads`() {
        val gate = GateHarness()
        repeat(5) { gate.signal() }
        assertFalse(gate.state.value)
        assertEquals(0, gate.loads)
    }

    @Test
    fun `revoke closes the gate and a later re-grant loads again`() {
        val gate = GateHarness()
        gate.granted = true
        gate.signal()
        assertEquals(1, gate.loads)

        gate.granted = false
        gate.signal()
        assertFalse(gate.state.value)

        gate.granted = true
        gate.signal()
        assertTrue(gate.state.value)
        assertEquals(2, gate.loads)
    }

    @Test
    fun `already granted state ignores transition signals`() {
        // Shared view models keep hasPermission=true across page entries;
        // transition signals must not re-trigger the entry load.
        val gate = GateHarness(initial = true)
        gate.granted = true
        gate.signal()
        gate.signal()
        assertEquals(0, gate.loads)
    }
}
