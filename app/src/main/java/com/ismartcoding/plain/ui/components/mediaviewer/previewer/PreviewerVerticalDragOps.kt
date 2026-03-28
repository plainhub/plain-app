package com.ismartcoding.plain.ui.components.mediaviewer.previewer

import androidx.compose.animation.core.MutableTransitionState
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll

/**
 * Shrink and close viewer container
 */
internal suspend fun PreviewerVerticalDragState.viewerContainerShrinkDown() {
    stateCloseStart()
    viewerContainerState?.cancelOpenTransform()
    listOf(
        scope.async { viewerContainerState?.transformContentAlpha?.snapTo(0F) },
        scope.async { uiAlpha.animateTo(0F, DEFAULT_SOFT_ANIMATION_SPEC) },
        scope.async { animateContainerVisibleState = MutableTransitionState(false) }
    ).awaitAll()
    ticket.awaitNextTicket()
    stateCloseEnd()
    transformState?.setExitState()
}

/**
 * Respond to downward drag to close
 */
internal suspend fun PreviewerVerticalDragState.dragDownClose() {
    transformState?.notifyEnterChanged()
    viewerContainerState?.showLoading = false
    ticket.awaitNextTicket()
    viewerContainerState?.copyViewerContainerStateToTransformState()
    viewerContainerState?.resetImmediately()
    transformSnapToViewer(false)
    ticket.awaitNextTicket()
    closeTransform()
    viewerContainerState?.showLoading = true
}
