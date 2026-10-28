package com.ismartcoding.plain.ui.tools

import com.ismartcoding.plain.platform.system.AppFeatureType
import com.ismartcoding.plain.platform.system.Capability
import kotlin.test.*

class ManagedFeatureItemsTest {
    @Test
    fun exposes_remote_content_and_excludes_local_sensor_timer_and_editor_tools() {
        val selected = mutableListOf<AppFeatureType>()
        val items = managedFeatureItems(Capability.entries, selected::add)
        assertEquals(setOf(AppFeatureType.FILES, AppFeatureType.NOTES, AppFeatureType.IMAGES, AppFeatureType.VIDEOS, AppFeatureType.AUDIO, AppFeatureType.DOCS, AppFeatureType.FEEDS, AppFeatureType.APPS), items.map { it.type }.toSet())
        assertTrue(items.none { it.type in setOf(AppFeatureType.SOUND_METER, AppFeatureType.POMODORO_TIMER, AppFeatureType.IMAGE_EDITOR) })
        items.forEach { it.click() }
        assertEquals(items.map { it.type }, selected)
    }
}
