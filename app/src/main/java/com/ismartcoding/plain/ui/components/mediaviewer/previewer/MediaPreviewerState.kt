package com.ismartcoding.plain.ui.components.mediaviewer.previewer

import androidx.annotation.IntRange
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.mapSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.util.fastAny
import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.ui.components.mediaviewer.MediaViewerState
import com.ismartcoding.plain.ui.components.mediaviewer.Ticket
import com.ismartcoding.plain.ui.components.mediaviewer.video.VideoState
import com.ismartcoding.plain.ui.models.MediaPreviewData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.Any
import kotlin.String
import kotlin.coroutines.resume
import kotlin.math.absoluteValue

const val DEFAULT_SCALE_TO_CLOSE_MIN_VALUE = 0.9F

enum class VerticalDragType { None, Down, UpAndDown }

@OptIn(ExperimentalFoundationApi::class)
class MediaPreviewerState(
    var scope: CoroutineScope = MainScope(),
    val pagerState: PagerState,
) {
    var verticalDragType by mutableStateOf(VerticalDragType.None)
    val videoState = VideoState()
    private var mutex = Mutex()
    internal var openCallback: (() -> Unit)? = null
    internal var closeCallback: (() -> Unit)? = null

    internal val viewerContainerVisible: Boolean
        get() = viewerContainerState?.viewerContainerAlpha?.value == 1F

    private suspend fun updateState(animating: Boolean, visible: Boolean, visibleTarget: Boolean?) {
        mutex.withLock {
            this.animating = animating
            this.visible = visible
            this.visibleTarget = visibleTarget
        }
    }

    internal val ticket = Ticket()
    internal var animateContainerVisibleState by mutableStateOf(MutableTransitionState(false))
    internal var uiAlpha = Animatable(0F)
    internal var viewerAlpha = Animatable(1F)
    internal var viewerContainerState by mutableStateOf<ViewerContainerState?>(null)

    internal val transformState: TransformContentState?
        get() = viewerContainerState?.transformState

    internal val canTransformOut: Boolean
        get() = (viewerContainerState?.openTransformJob != null) || (mediaViewerState?.mountedFlow?.value == true)

    internal suspend fun stateOpenStart() = updateState(animating = true, visible = false, visibleTarget = true)
    internal suspend fun stateOpenEnd() = updateState(animating = false, visible = true, visibleTarget = null)
    internal suspend fun stateCloseStart() = updateState(animating = true, visible = true, visibleTarget = false)
    internal suspend fun stateCloseEnd() = updateState(animating = false, visible = false, visibleTarget = null)

    internal suspend fun transformSnapToViewer(isViewer: Boolean) {
        if (isViewer && visibleTarget == false) return
        viewerContainerState?.transformSnapToViewer(isViewer)
    }

    internal fun onAnimateContainerStateChanged() {
        if (animateContainerVisibleState.currentState) {
            openCallback?.invoke()
            transformState?.setEnterState()
        } else {
            closeCallback?.invoke()
        }
    }

    var showActions by mutableStateOf(true)
    var showMediaInfo by mutableStateOf(false)
    var pagerUserScrollEnabled by mutableStateOf(true)

    var animating by mutableStateOf(false)
        private set
    var visible by mutableStateOf(false)
        internal set
    private var visibleTarget by mutableStateOf<Boolean?>(null)

    val mediaViewerState: MediaViewerState?
        get() = viewerContainerState?.viewerState

    fun getKey(index: Int): String {
        return MediaPreviewData.items.getOrNull(index)?.id ?: index.toString()
    }

    fun findTransformItem(key: String): TransformItemState? = transformItemStateMap[key]
    fun clearTransformItems() = transformItemStateMap.clear()

    suspend fun open(index: Int = 0, itemState: TransformItemState? = null) = suspendCancellableCoroutine { c ->
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

    suspend fun close() = suspendCancellableCoroutine { c ->
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
                scope.async { animateContainerVisibleState = MutableTransitionState(false) }).awaitAll()
            showActions = true
            ticket.awaitNextTicket()
            transformState?.setExitState()
        }
    }

    suspend fun openTransform(
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
            scope.async {
                uiAlpha.animateTo(1F, DEFAULT_SOFT_ANIMATION_SPEC)
            }
        ).awaitAll()
        stateOpenEnd()
        viewerContainerState?.awaitOpenTransform()
    }

    suspend fun closeTransform() {
        stateCloseStart()
        viewerContainerState?.cancelOpenTransform()
        viewerContainerState?.showLoading = false
        val itemState = findTransformItem(getKey(pagerState.currentPage))
        if (itemState != null && canTransformOut) {
            if (viewerContainerVisible) {
                transformState?.setEnterState()
                transformState?.notifyEnterChanged()
                ticket.awaitNextTicket()
                viewerContainerState?.copyViewerPosToContent(itemState)
                transformSnapToViewer(false)
            }
            ticket.awaitNextTicket()
            listOf(scope.async {
                transformState?.exitTransform(DEFAULT_SOFT_ANIMATION_SPEC)
                viewerContainerState?.transformContentAlpha?.snapTo(0F)
            }, scope.async {
                uiAlpha.animateTo(0F, DEFAULT_SOFT_ANIMATION_SPEC)
            }).awaitAll()
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

    suspend fun verticalDrag(pointerInputScope: PointerInputScope) {
        if (verticalDragType == VerticalDragType.None) return
        pointerInputScope.apply {
            awaitEachGesture {
                val firstDown = awaitFirstDown(requireUnconsumed = false)
                var vStartOffset: Offset? = null
                var vOrientationDown: Boolean? = null
                var dragActivated = false
                var directionLocked = false

                if (mediaViewerState != null) {
                    var transformItemState: TransformItemState? = null
                    findTransformItem(getKey(pagerState.currentPage))?.apply { transformItemState = this }
                    if (canTransformOut) transformState?.setEnterState() else transformState?.setExitState()
                    transformState?.itemState = transformItemState
                    if (mediaViewerState?.scale?.value == 1F) {
                        vStartOffset = firstDown.position
                        dragActivated = true
                        mediaViewerState?.allowGestureInput = false
                    }
                }

                do {
                    val event = awaitPointerEvent()
                    if (event.changes.size > 1) {
                        if (dragActivated) {
                            scope.launch { uiAlpha.animateTo(1F, DEFAULT_SOFT_ANIMATION_SPEC) }
                            scope.launch { viewerContainerState?.reset(DEFAULT_SOFT_ANIMATION_SPEC) }
                            dragActivated = false; vStartOffset = null; vOrientationDown = null
                        }
                        pagerUserScrollEnabled = true
                        mediaViewerState?.allowGestureInput = true
                        break
                    }
                    val change = event.changes.firstOrNull() ?: break
                    when {
                        dragActivated && event.type == PointerEventType.Move -> {
                            if (vStartOffset == null || viewerContainerState == null) continue
                            val dx = change.position.x - vStartOffset.x
                            val dy = change.position.y - vStartOffset.y
                            if (!directionLocked) {
                                if (dx.absoluteValue < viewConfiguration.touchSlop && dy.absoluteValue < viewConfiguration.touchSlop) continue
                                directionLocked = true
                                if (dx.absoluteValue > dy.absoluteValue) {
                                    pagerUserScrollEnabled = true; mediaViewerState?.allowGestureInput = true
                                    dragActivated = false; vStartOffset = null; continue
                                }
                                pagerUserScrollEnabled = false; vOrientationDown = dy > 0
                            }
                            if (vOrientationDown == true || verticalDragType == VerticalDragType.UpAndDown) {
                                val containerHeight = viewerContainerState!!.containerSize.height
                                val scale = (containerHeight - dy.absoluteValue).div(containerHeight)
                                scope.launch {
                                    uiAlpha.snapTo(scale); viewerContainerState?.offsetX?.snapTo(dx)
                                    viewerContainerState?.offsetY?.snapTo(dy); viewerContainerState?.scale?.snapTo(scale)
                                }
                                change.consume()
                            } else {
                                pagerUserScrollEnabled = true; mediaViewerState?.allowGestureInput = true
                                dragActivated = false; vStartOffset = null
                            }
                        }

                        dragActivated && event.type == PointerEventType.Release -> {
                            pagerUserScrollEnabled = true; mediaViewerState?.allowGestureInput = true
                            vStartOffset = null; vOrientationDown = null; dragActivated = false
                            if ((viewerContainerState?.scale?.value ?: 1F) < DEFAULT_SCALE_TO_CLOSE_MIN_VALUE) {
                                scope.launch {
                                    if (canTransformOut) {
                                        val key = getKey(pagerState.currentPage)
                                        if (findTransformItem(key) != null) {
                                            dragDownClose()
                                        } else {
                                            viewerContainerShrinkDown()
                                        }
                                    } else viewerContainerShrinkDown()
                                    uiAlpha.snapTo(1F)
                                }
                            } else {
                                scope.launch { uiAlpha.animateTo(1F, DEFAULT_SOFT_ANIMATION_SPEC) }
                                scope.launch { viewerContainerState?.reset(DEFAULT_SOFT_ANIMATION_SPEC) }
                            }
                            break
                        }
                    }
                } while (event.changes.fastAny { it.pressed })
                pagerUserScrollEnabled = true; mediaViewerState?.allowGestureInput = true
            }
        }
    }

    suspend fun viewerContainerShrinkDown() {
        stateCloseStart()
        viewerContainerState?.cancelOpenTransform()
        listOf(
            scope.async { viewerContainerState?.transformContentAlpha?.snapTo(0F) },
            scope.async { uiAlpha.animateTo(0F, DEFAULT_SOFT_ANIMATION_SPEC) },
            scope.async { animateContainerVisibleState = MutableTransitionState(false) }).awaitAll()
        ticket.awaitNextTicket()
        stateCloseEnd()
        transformState?.setExitState()
    }

    suspend fun dragDownClose() {
        stateCloseStart()
        viewerContainerState?.showLoading = false
        transformState?.setEnterState()
        transformState?.notifyEnterChanged()
        ticket.awaitNextTicket()
        viewerContainerState?.copyViewerContainerStateToTransformState()
        viewerContainerState?.resetImmediately()
        viewerContainerState?.transformSnapToViewer(false)
        ticket.awaitNextTicket()
        listOf(scope.async {
            transformState?.exitTransform(DEFAULT_SOFT_ANIMATION_SPEC)
            viewerContainerState?.transformContentAlpha?.snapTo(0F)
        }, scope.async {
            uiAlpha.animateTo(0F, DEFAULT_SOFT_ANIMATION_SPEC)
        }).awaitAll()
        ticket.awaitNextTicket()
        animateContainerVisibleState = MutableTransitionState(false)
        viewerContainerState?.showLoading = true
        showActions = true
        stateCloseEnd()
    }

    companion object {
        fun getSaver(pagerState: PagerState): Saver<MediaPreviewerState, *> {
            return mapSaver(save = {
                mapOf<String, Any>(
                    it.pagerState::currentPage.name to it.pagerState.currentPage,
                    it::animateContainerVisibleState.name to it.animateContainerVisibleState.currentState,
                    it::uiAlpha.name to it.uiAlpha.value,
                    it::visible.name to it.visible,
                )
            }, restore = {
                val previewerState = MediaPreviewerState(pagerState = pagerState)
                previewerState.animateContainerVisibleState = MutableTransitionState(it[MediaPreviewerState::animateContainerVisibleState.name] as Boolean)
                previewerState.uiAlpha = Animatable(it[MediaPreviewerState::uiAlpha.name] as Float)
                previewerState.visible = it[MediaPreviewerState::visible.name] as Boolean
                previewerState
            })
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun rememberPreviewerState(
    scope: CoroutineScope = rememberCoroutineScope(),
    verticalDragType: VerticalDragType = VerticalDragType.UpAndDown,
    @IntRange(from = 0) initialPage: Int = 0,
    pageCount: () -> Int = { MediaPreviewData.items.size }
): MediaPreviewerState {
    val pagerState = rememberPagerState(initialPage, pageCount = pageCount)
    val mediaPreviewerState = rememberSaveable(saver = MediaPreviewerState.getSaver(pagerState)) {
        MediaPreviewerState(pagerState = pagerState)
    }
    mediaPreviewerState.scope = scope
    mediaPreviewerState.verticalDragType = verticalDragType
    return mediaPreviewerState
}