package com.ismartcoding.plain.ui.media.viewer.previewer

import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ViewerHandoffTest {
    @Test
    fun mountedViewerTakesOverInTheCurrentTurn() = runTest {
        val container = ViewerContainerState(scope = this)
        container.transformSnapToViewer(false)
        container.viewerState.mountedFlow.value = true
        val opening = launch(start = CoroutineStart.UNDISPATCHED) { container.awaitOpenTransform() }
        assertTrue(opening.isCompleted)
        assertEquals(1f, container.viewerContainerAlpha.value)
    }

    @Test
    fun replacingAnOpeningKeepsTheNewHandoffCancellable() = runTest {
        val container = ViewerContainerState(scope = this)
        container.transformSnapToViewer(false)
        val first = launch(start = CoroutineStart.UNDISPATCHED) { container.awaitOpenTransform() }
        val next = launch(start = CoroutineStart.UNDISPATCHED) { container.awaitOpenTransform() }
        first.join()
        assertTrue(first.isCancelled)
        container.cancelOpenTransform()
        container.viewerState.mountedFlow.value = true
        next.join()
        assertTrue(next.isCancelled)
        assertEquals(0f, container.viewerContainerAlpha.value)
    }

    @Test
    fun closingBeforeMountCannotRevealTheRetiredViewer() = runTest {
        val container = ViewerContainerState(scope = this)
        container.transformSnapToViewer(false)
        val opening = launch(start = CoroutineStart.UNDISPATCHED) { container.awaitOpenTransform() }
        container.cancelOpenTransform()
        container.viewerState.mountedFlow.value = true
        opening.join()
        assertTrue(opening.isCancelled)
        assertEquals(0f, container.viewerContainerAlpha.value)
    }
}
