package com.ismartcoding.plain.tests

import android.content.res.Configuration
import com.ismartcoding.plain.services.screenmirror.ScreenMirrorOrientationState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScreenMirrorOrientationStateTest {
    @Test
    fun `committed landscape configuration updates the mirror once`() {
        val changes = mutableListOf<Boolean>()
        val state = ScreenMirrorOrientationState(initialIsPortrait = true, changes::add)

        assertTrue(state.isPortrait)

        state.onConfigurationChanged(Configuration.ORIENTATION_LANDSCAPE)
        state.onConfigurationChanged(Configuration.ORIENTATION_LANDSCAPE)

        assertFalse(state.isPortrait)
        assertEquals(listOf(false), changes)
    }

    @Test
    fun `committed portrait configuration restores the mirror`() {
        val changes = mutableListOf<Boolean>()
        val state = ScreenMirrorOrientationState(initialIsPortrait = false, changes::add)

        state.onConfigurationChanged(Configuration.ORIENTATION_PORTRAIT)

        assertTrue(state.isPortrait)
        assertEquals(listOf(true), changes)
    }

    @Test
    fun `undefined configuration does not rebuild the mirror`() {
        val changes = mutableListOf<Boolean>()
        val state = ScreenMirrorOrientationState(initialIsPortrait = true, changes::add)

        state.onConfigurationChanged(Configuration.ORIENTATION_UNDEFINED)

        assertTrue(state.isPortrait)
        assertTrue(changes.isEmpty())
    }
}
