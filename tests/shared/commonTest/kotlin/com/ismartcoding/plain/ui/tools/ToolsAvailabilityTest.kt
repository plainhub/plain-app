package com.ismartcoding.plain.ui.tools

import com.ismartcoding.plain.platform.system.Capability
import kotlin.test.*

class ToolsAvailabilityTest {
    @Test fun sharedToolsAppearWithoutAndroidSpecificCapabilities() {
        assertTrue(hasTools(listOf(Capability.NOTES)))
        assertTrue(hasTools(listOf(Capability.FEEDS)))
        assertTrue(hasTools(listOf(Capability.SOUND_METER)))
        assertTrue(hasTools(listOf(Capability.FILE_BROWSER)))
    }
    @Test fun discoveryAndBackgroundFacilitiesDoNotImplyAToolsPage() {
        assertFalse(hasTools(emptyList()))
        assertFalse(hasTools(listOf(Capability.BACKGROUND_WAKE)))
    }
}
