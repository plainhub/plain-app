package com.ismartcoding.plain.ui.components.mediaviewer.previewer

import androidx.compose.animation.core.MutableTransitionState
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

internal suspend fun PreviewerTransformState.doOpen(
    index: Int = 0,
    itemState: TransformItemState? = null,
) = suspendCoroutine<Unit> { c ->
    openCallback = {
        c.resume(Unit)
        openCallback = null
        scope.launch { stateOpenEnd() }
    }
    scope.launch {
        showActions = true
        stateOpenStart()
        uiAlpha.snapTo(1F)
        animateContainerVisibleState = MutableTransitionState(false)
        animateContainerVisibleState.targetState = true
        pagerState.scrollToPage(index)
        ticket.awaitNextTicket()
        viewerContainerState?.showLoading = true
        viewerContainerState?.viewerContainerAlpha?.snapTo(1F)
        if (itemState != null) {
            scope.launch {
                viewerContainerState?.transformContentAlpha?.snapTo(1F)
                transformState?.awaitContainerSizeSpecifier()
                transformState?.enterTransform(itemState, androidx.compose.animation.core.tween(0))
            }
        }
        viewerContainerState?.awaitOpenTransform()
    }
}

internal suspend fun PreviewerTransformState.doClose() = suspendCoroutine<Unit> { c ->
    closeCallback = {
        c.resume(Unit)
        closeCallback = null
        scope.launch { stateCloseEnd() }
    }
    scope.launch {
        stateCloseStart()
        viewerContainerState?.cancelOpenTransform()
        listOf(
            scope.async { viewerContainerState?.transformContentAlpha?.snapTo(0F) },
            scope.async { uiAlpha.animateTo(0F, DEFAULT_SOFT_ANIMATION_SPEC) },
            scope.async { animateContainerVisibleState = MutableTransitionState(false) }
        ).awaitAll()
        showActions = true
        ticket.awaitNextTicket()
        transformState?.setExitState()
    }
}

internal suspend fun PreviewerTransformState.doOpenTransform(
    index: Int,
    itemState: TransformItemState,
) {
    stateOpenStart()
    uiAlpha.snapTo(0F)
    viewerAlpha.snapTo(0F)
    animateContainerVisibleState = MutableTransitionState(true)
    pagerState.scrollToPage(index)
    ticket.awaitNextTicket()
    viewerContainerState?.showLoading = false
    transformSnapToViewer(false)
    viewerAlpha.snapTo(1F)
    listOf(
        scope.async {
            transformState?.enterTransform(itemState, DEFAULT_SOFT_ANIMATION_SPEC)
            viewerContainerState?.showLoading = true
        },
        scope.async { uiAlpha.animateTo(1F, DEFAULT_SOFT_ANIMATION_SPEC) }
    ).awaitAll()
    stateOpenEnd()
    viewerContainerState?.awaitOpenTransform()
}

internal suspend fun PreviewerTransformState.doCloseTransform() {
    stateCloseStart()
    val canNowTransformOut = canTransformOut
    viewerContainerState?.cancelOpenTransform()
    viewerContainerState?.showLoading = false
    val itemState = findTransformItemByIndex(pagerState.currentPage)
    if (itemState != null && canNowTransformOut) {
        if (viewerContainerVisible) {
            transformState?.setEnterState()
            transformState?.notifyEnterChanged()
            ticket.awaitNextTicket()
            viewerContainerState?.copyViewerPosToContent(itemState)
            transformSnapToViewer(false)
        }
        ticket.awaitNextTicket()
        listOf(
            scope.async {
                transformState?.exitTransform(DEFAULT_SOFT_ANIMATION_SPEC)
                viewerContainerState?.transformContentAlpha?.snapTo(0F)
            },
            scope.async { uiAlpha.animateTo(0F, DEFAULT_SOFT_ANIMATION_SPEC) }
        ).awaitAll()
        ticket.awaitNextTicket()
        animateContainerVisibleState = MutableTransitionState(false)
    } else {
        transformState?.setExitState()
        animateContainerVisibleState.targetState = false
    }
    viewerContainerState?.showLoading = true
    showActions = true
    stateCloseEnd()
}
