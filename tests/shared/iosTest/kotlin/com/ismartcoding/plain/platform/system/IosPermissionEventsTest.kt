package com.ismartcoding.plain.platform.system

import com.ismartcoding.plain.events.WindowFocusChangedEvent
import com.ismartcoding.plain.features.web.AccessFeature
import com.ismartcoding.plain.lib.Channel
import com.ismartcoding.plain.lib.ChannelEvent
import com.ismartcoding.plain.lib.sendEvent
import com.ismartcoding.plain.ui.web.AccessSettingsState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class IosPermissionEventsTest {
    @Test
    fun focus_events_during_permission_prompt_do_not_block_access_switches() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val originalFlow = Channel.sharedFlow
        Channel.sharedFlow = MutableSharedFlow<ChannelEvent>()
        var granted = false
        var allow = true
        var requests = 0
        IosPlatformRegistry.setPermissionChecker(object : IosPermissionChecker {
            override fun isGranted(permission: String) = granted
            override fun requestPermission(permission: String) {
                requests++
                sendEvent(WindowFocusChangedEvent(false))
                granted = allow
                IosPermissionCallback.onPermissionResult(permission, allow)
                sendEvent(WindowFocusChangedEvent(true))
            }
            override fun openAppSettings() = Unit
        })
        val writes = mutableListOf<Pair<Set<AccessFeature>, Boolean>>()
        val state = AccessSettingsState(update = { features, enabled -> writes.add(features to enabled) })
        val features = setOf(AccessFeature.CONTACTS)
        var toggle = launch { state.setEnabled(features, true) }
        try {
            IosPermissionEvents.register()
            runCurrent()
            assertTrue(toggle.isCompleted, "The access switch must finish after the permission callback")
            assertFalse(state.busy)
            assertEquals(listOf(features to true), writes)
            granted = false
            toggle = launch { state.setEnabled(features, true) }
            runCurrent()
            assertTrue(toggle.isCompleted, "Later permission requests must still be delivered")
            assertEquals(4, requests)
            assertEquals(listOf(features to true, features to true), writes)
            granted = false
            allow = false
            toggle = launch { state.setEnabled(features, true) }
            runCurrent()
            assertTrue(toggle.isCompleted)
            assertFalse(state.busy)
            assertEquals(features, state.deniedFeatures)
            assertEquals(2, writes.size)
            state.setEnabled(features, false)
            assertTrue(state.deniedFeatures.isEmpty())
            assertEquals(features to false, writes.last())
            assertEquals(5, requests)
        } finally {
            toggle.cancelAndJoin()
            IosPermissionEvents.eventJob?.cancelAndJoin()
            Channel.sharedFlow = originalFlow
            Dispatchers.resetMain()
        }
    }
}
