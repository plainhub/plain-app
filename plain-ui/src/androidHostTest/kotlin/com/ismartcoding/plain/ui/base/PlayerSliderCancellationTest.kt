package com.ismartcoding.plain.ui.base

import androidx.compose.foundation.interaction.DragInteraction
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

class PlayerSliderCancellationTest {
    @Test
    fun cancellationResetsBeforeDraggableCallsFinished() = runTest {
        val state = PlayerSliderState(0.2f)
        var externalProgress = 0.3f
        val source = PlayerSliderInteractionSource { state.cancelDrag(externalProgress) }
        val start = DragInteraction.Start()
        source.emit(start)
        state.startDrag(0.8f)
        externalProgress = 0.4f
        source.emit(DragInteraction.Cancel(start))
        assertFalse(state.isDragging)
        assertFalse(state.seekHoldActive)
        assertEquals(0.4f, state.displayProgress)
    }

    @Test
    fun disposalCancelsAndNormalReleaseRetainsSeekTarget() {
        val state = PlayerSliderState(0.2f)
        val source = PlayerSliderInteractionSource { state.cancelDrag(0.4f) }
        val start = DragInteraction.Start()
        state.startDrag(0.8f)
        source.tryEmit(DragInteraction.Stop(start))
        assertTrue(state.isDragging)
        assertEquals(0.8f, state.endDrag())
        state.startDrag(0.6f)
        source.tryEmit(DragInteraction.Cancel(start))
        assertFalse(state.isDragging)
        assertEquals(0.4f, state.displayProgress)
    }
}
