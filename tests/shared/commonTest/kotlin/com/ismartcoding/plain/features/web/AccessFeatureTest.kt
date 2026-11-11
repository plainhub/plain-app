package com.ismartcoding.plain.features.web

import com.ismartcoding.plain.lib.JsonHelper
import com.ismartcoding.plain.models.AppAccess
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails

class AccessFeatureTest {
    @Test
    fun decodesThePublicFeatureSnapshot() {
        val snapshot = JsonHelper.jsonDecode<AppAccess>("""{"access":["IMAGES","FILES","ADB"]}""")
        assertEquals(listOf(AccessFeature.IMAGES, AccessFeature.FILES, AccessFeature.ADB), snapshot.access)
        assertEquals("\"DOCS\"", JsonHelper.jsonEncode(AccessFeature.DOCS))
    }

    @Test
    fun rejectsRetiredAndPlatformPermissionPayloads() {
        assertFails { JsonHelper.jsonDecode<AppAccess>("""{"permissions":["WRITE_EXTERNAL_STORAGE"]}""") }
        assertFails { JsonHelper.jsonDecode<AppAccess>("""{"access":["WRITE_EXTERNAL_STORAGE"]}""") }
    }
    @Test
    fun manageFilesUsesOneSwitchForAllFiveFeatures() {
        val features = com.ismartcoding.plain.ui.web.managedFileAccessFeatures
        assertEquals(setOf(AccessFeature.IMAGES, AccessFeature.VIDEOS, AccessFeature.AUDIO, AccessFeature.DOCS, AccessFeature.FILES), features)
        val item = com.ismartcoding.plain.ui.web.AccessItem(features, issues = emptyList())
        kotlin.test.assertFalse(item.isEnabled(features - AccessFeature.VIDEOS))
        kotlin.test.assertFalse(item.isEnabled(emptySet()))
        kotlin.test.assertTrue(item.isEnabled(features + AccessFeature.CLIPBOARD))
    }
}
