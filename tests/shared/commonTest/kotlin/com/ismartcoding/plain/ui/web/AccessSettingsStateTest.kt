package com.ismartcoding.plain.ui.web

import com.ismartcoding.plain.features.web.AccessFeature
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AccessSettingsStateTest {
    private val features = setOf(AccessFeature.CONTACTS)

    @Test
    fun permission_is_required_before_remote_access_is_enabled() = runTest {
        val permission = CompletableDeferred<Boolean>()
        val writes = mutableListOf<Pair<Set<AccessFeature>, Boolean>>()
        val state = AccessSettingsState(request = { permission.await() }, update = { features, enabled -> writes.add(features to enabled) })
        val job = launch(start = CoroutineStart.UNDISPATCHED) { state.setEnabled(features, true) }
        assertTrue(state.busy)
        assertTrue(writes.isEmpty())
        permission.complete(true)
        job.join()
        assertEquals(listOf(features to true), writes)
        assertFalse(state.busy)
        assertTrue(state.deniedFeatures.isEmpty())
    }

    @Test
    fun denial_keeps_access_off_and_explains_why_then_retry_can_succeed() = runTest {
        var granted = false
        val writes = mutableListOf<Boolean>()
        val state = AccessSettingsState(request = { granted }, update = { _, enabled -> writes.add(enabled) })
        state.setEnabled(features, true)
        assertTrue(writes.isEmpty())
        assertEquals(features, state.deniedFeatures)
        assertFalse(state.busy)
        granted = true
        state.setEnabled(features, true)
        assertEquals(listOf(true), writes)
        assertTrue(state.deniedFeatures.isEmpty())
    }

    @Test
    fun disabling_never_requests_system_permissions() = runTest {
        val writes = mutableListOf<Pair<Set<AccessFeature>, Boolean>>()
        val state = AccessSettingsState(request = { error("Must not request permission") }, update = { features, enabled -> writes.add(features to enabled) })
        state.setEnabled(managedFileAccessFeatures, false)
        assertEquals(listOf(managedFileAccessFeatures to false), writes)
    }

    @Test
    fun repeated_taps_do_not_launch_concurrent_permission_requests() = runTest {
        val permission = CompletableDeferred<Boolean>()
        var requests = 0
        var writes = 0
        val state = AccessSettingsState(request = { requests++; permission.await() }, update = { _, _ -> writes++ })
        val job = launch(start = CoroutineStart.UNDISPATCHED) { state.setEnabled(features, true) }
        state.setEnabled(setOf(AccessFeature.SMS), true)
        assertEquals(1, requests)
        permission.complete(true)
        job.join()
        assertEquals(1, writes)
    }

    @Test
    fun failure_is_propagated_and_releases_pending_state() = runTest {
        val state = AccessSettingsState(request = { throw IllegalStateException("Permission request failed") }, update = { _, _ -> error("Must not write") })
        assertFailsWith<IllegalStateException> { state.setEnabled(features, true) }
        assertFalse(state.busy)
        assertTrue(state.deniedFeatures.isEmpty())
    }

    @Test
    fun cancellation_does_not_enable_access() = runTest {
        val permission = CompletableDeferred<Boolean>()
        var writes = 0
        val state = AccessSettingsState(request = { permission.await() }, update = { _, _ -> writes++ })
        val job = launch(start = CoroutineStart.UNDISPATCHED) { state.setEnabled(features, true) }
        job.cancelAndJoin()
        assertEquals(0, writes)
        assertFalse(state.busy)
    }
}
