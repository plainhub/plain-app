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
    fun optionalDistributionAndToolEntriesUseTheirOwnCapabilities() {
        for ((feature, capability) in listOf(
            AppFeatureType.CHECK_UPDATES to Capability.APP_UPDATES,
            AppFeatureType.SOUND_METER to Capability.SOUND_METER,
        )) {
            assertFalse(feature.has(emptyList()))
            assertTrue(feature.has(listOf(capability)))
            assertFalse(feature.has(Capability.entries.filter { it != capability }))
        }
    }

    @Test
    fun baseFileAndMediaEntriesAreAvailableWithoutCapabilities() {
        for (feature in listOf(
            AppFeatureType.FILES, AppFeatureType.IMAGES, AppFeatureType.VIDEOS,
            AppFeatureType.AUDIO, AppFeatureType.DOCS,
        )) {
            assertTrue(feature.has(emptyList()))
        }
    }
}
