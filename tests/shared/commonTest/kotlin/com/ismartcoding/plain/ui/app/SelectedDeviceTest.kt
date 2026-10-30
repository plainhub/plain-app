package com.ismartcoding.plain.ui.app

import com.ismartcoding.plain.platform.system.AppFeatureType
import kotlin.test.*

class SelectedDeviceTest {
    @AfterTest
    fun reset() {
        MainViewModel.selectDevice(null)
        MainViewModel.pendingDeviceContent.value = null
        MainViewModel.currentRootTab.intValue = 1
    }

    @Test
    fun selecting_a_different_device_resets_navigation_and_retires_its_pending_destination() {
        val first = DeviceIdentity("first", "first-key")
        val second = DeviceIdentity("second", "second-key")
        MainViewModel.openDeviceContent(first, DeviceContentEntry(AppFeatureType.NOTES, "first-note"))
        MainViewModel.currentRootTab.intValue = 2
        MainViewModel.selectDevice(second)
        assertEquals(second, MainViewModel.selectedDevice.value)
        assertEquals(1, MainViewModel.currentRootTab.intValue)
        assertNull(MainViewModel.pendingDeviceContent.value)
        MainViewModel.selectDevice(null)
        assertNull(MainViewModel.selectedDevice.value)
    }

    @Test
    fun selecting_a_repaired_device_uses_its_current_identity() {
        MainViewModel.selectDevice(DeviceIdentity("selected", "old-key"))
        MainViewModel.currentRootTab.intValue = 2
        val current = DeviceIdentity("selected", "new-key")
        MainViewModel.selectDevice(current)
        assertEquals(current, MainViewModel.selectedDevice.value)
        assertEquals(1, MainViewModel.currentRootTab.intValue)
    }

    @Test
    fun opening_another_result_on_the_same_device_preserves_the_device_scope() {
        val device = DeviceIdentity("selected", "selected-key")
        MainViewModel.selectDevice(device)
        MainViewModel.currentRootTab.intValue = 3
        val entry = DeviceContentEntry(AppFeatureType.NOTES, "selected-note")
        MainViewModel.openDeviceContent(device, entry)
        assertSame(device, MainViewModel.selectedDevice.value)
        assertEquals(3, MainViewModel.currentRootTab.intValue)
        assertEquals(entry, MainViewModel.pendingDeviceContent.value)
    }
}
