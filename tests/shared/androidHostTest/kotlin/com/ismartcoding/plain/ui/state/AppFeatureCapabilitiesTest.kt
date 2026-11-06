package com.ismartcoding.plain.ui.state

import com.ismartcoding.plain.platform.system.AppFeatureType
import com.ismartcoding.plain.platform.system.has

import com.ismartcoding.plain.platform.system.Capability
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AppFeatureCapabilitiesTest {
    @Test
    fun optionalFeaturesFollowTheAdvertisedCapabilitiesWithoutPlatformChecks() {
        val capabilities = listOf(Capability.PACKAGES)
        for (feature in listOf(AppFeatureType.NOTES, AppFeatureType.FEEDS, AppFeatureType.POMODORO_TIMER)) {
            assertTrue(feature.has(emptyList()))
        }
        assertTrue(AppFeatureType.APPS.has(capabilities))
        assertFalse(AppFeatureType.NOTIFICATIONS.has(capabilities))
    }

    @Test
    fun optionalDistributionEntriesUseTheirOwnCapabilities() {
        assertFalse(AppFeatureType.CHECK_UPDATES.has(emptyList()))
        assertTrue(AppFeatureType.CHECK_UPDATES.has(listOf(Capability.APP_UPDATES)))
        assertFalse(AppFeatureType.CHECK_UPDATES.has(Capability.entries.filter { it != Capability.APP_UPDATES }))
    }

    @Test
    fun imageEditorVisibilityFollowsDebugInsteadOfDeviceCapabilities() {
        for (capabilities in listOf(emptyList(), Capability.entries)) {
            assertTrue(AppFeatureType.IMAGE_EDITOR.has(capabilities, debug = true))
            assertFalse(AppFeatureType.IMAGE_EDITOR.has(capabilities, debug = false))
        }
    }

    @Test
    fun baseFileAndMediaEntriesAreAvailableWithoutCapabilities() {
        for (feature in listOf(
            AppFeatureType.FILES, AppFeatureType.IMAGES, AppFeatureType.VIDEOS,
            AppFeatureType.AUDIO, AppFeatureType.DOCS, AppFeatureType.SOUND_METER,
        )) {
            assertTrue(feature.has(emptyList()))
        }
    }
}
