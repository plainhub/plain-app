package com.ismartcoding.plain.ui.base

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Unit tests for [PlayerSliderState]'s seek-hold behavior.
 *
 * The slider must NOT snap back to a stale parent-reported progress value
 * immediately after a user-initiated seek (regression: cast page slider
 * jumping back to 0 because the DLNA position poller reports 0 until the
 * renderer processes the seek). The hold window retains the user's drag
 * target until either the parent catches up or the hold timeout expires.
 */
class PlayerSliderStateTest {

    @Test
    fun `endDrag arms seek hold and returns target ratio`() {
        val state = PlayerSliderState(initialProgress = 0f)

        state.startDrag(0.4f)
        state.updateDrag(0.3f) // drag to 0.7
        val target = state.endDrag()

        assertEquals(0.7f, target, 0.0001f)
        assertTrue(state.seekHoldActive)
        assertFalse(state.isDragging)
        assertEquals(0.7f, state.displayProgress, 0.0001f)
    }

    @Test
    fun `seek hold prevents stale progress from overwriting drag position`() {
        val state = PlayerSliderState(initialProgress = 0f)

        state.startDrag(0.5f)
        state.updateDrag(0.2f) // -> 0.7
        state.endDrag()

        // Parent poller reports 0 (renderer hasn't processed seek yet).
        state.syncExternalProgress(0f)

        assertEquals(0.7f, state.displayProgress, 0.0001f)
        assertTrue(state.seekHoldActive)
    }

    @Test
    fun `after seek hold expires external progress syncs again`() {
        val state = PlayerSliderState(initialProgress = 0f)

        state.startDrag(0.5f)
        state.updateDrag(0.2f) // -> 0.7
        state.endDrag()

        state.expireSeekHold()
        assertFalse(state.seekHoldActive)

        // Now a fresh progress update should be applied.
        state.syncExternalProgress(0.3f)
        assertEquals(0.3f, state.displayProgress, 0.0001f)
    }

    @Test
    fun `stale progress during hold does not block later sync after expiry`() {
        val state = PlayerSliderState(initialProgress = 0f)

        state.startDrag(0.5f)
        state.endDrag() // hold active, target = 0.5

        // Stale 0 reported during hold — ignored.
        state.syncExternalProgress(0f)
        assertEquals(0.5f, state.displayProgress, 0.0001f)

        // Hold expires; subsequent update is applied.
        state.expireSeekHold()
        state.syncExternalProgress(0.9f)
        assertEquals(0.9f, state.displayProgress, 0.0001f)
    }

    @Test
    fun `starting a new drag cancels active seek hold`() {
        val state = PlayerSliderState(initialProgress = 0f)

        state.startDrag(0.5f)
        state.endDrag()
        assertTrue(state.seekHoldActive)

        state.startDrag(0.2f)
        assertFalse(state.seekHoldActive)
        assertTrue(state.isDragging)
        assertEquals(0.2f, state.displayProgress, 0.0001f)
    }

    @Test
    fun `tap arms seek hold and returns tapped ratio`() {
        val state = PlayerSliderState(initialProgress = 0f)

        val target = state.tap(0.6f)

        assertEquals(0.6f, target)
        assertTrue(state.seekHoldActive)
        assertEquals(0.6f, state.displayProgress, 0.0001f)
    }

    @Test
    fun `tap with out-of-range offset is clamped`() {
        val state = PlayerSliderState(initialProgress = 0f)

        state.tap(-1f)
        assertEquals(0f, state.displayProgress, 0.0001f)

        state.tap(2f)
        assertEquals(1f, state.displayProgress, 0.0001f)
    }

    @Test
    fun `cancelDrag resets to current external progress without hold`() {
        val state = PlayerSliderState(initialProgress = 0f)

        state.startDrag(0.5f)
        state.updateDrag(0.2f) // -> 0.7
        state.cancelDrag(currentProgress = 0.4f)

        assertFalse(state.isDragging)
        assertFalse(state.seekHoldActive)
        assertEquals(0.4f, state.displayProgress, 0.0001f)
    }

    @Test
    fun `updateDrag is ignored when not dragging`() {
        val state = PlayerSliderState(initialProgress = 0.3f)

        state.updateDrag(0.5f) // no-op, not dragging

        assertEquals(0.3f, state.displayProgress, 0.0001f)
    }

    @Test
    fun `external progress syncs when idle no drag no hold`() {
        val state = PlayerSliderState(initialProgress = 0f)

        state.syncExternalProgress(0.42f)
        assertEquals(0.42f, state.displayProgress, 0.0001f)
    }

    @Test
    fun `drag position is clamped to valid range during drag`() {
        val state = PlayerSliderState(initialProgress = 0f)

        state.startDrag(0.9f)
        state.updateDrag(0.5f) // would exceed 1.0
        assertEquals(1f, state.displayProgress, 0.0001f)

        state.updateDrag(-1.5f) // back below 0
        assertEquals(0f, state.displayProgress, 0.0001f)
    }

    @Test
    fun `custom seek hold duration is exposed for LaunchedEffect`() {
        val state = PlayerSliderState(initialProgress = 0f, seekHoldDurationMs = 2500L)
        assertEquals(2500L, state.seekHoldDuration)
    }

    @Test
    fun `default seek hold duration is 1500ms`() {
        val state = PlayerSliderState(initialProgress = 0f)
        assertEquals(1500L, state.seekHoldDuration)
    }
}
