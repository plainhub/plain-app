package com.ismartcoding.plain.ui.components.mediaviewer.previewer

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.ismartcoding.plain.ui.components.mediaviewer.MediaViewerState
import com.ismartcoding.plain.ui.components.mediaviewer.Ticket
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@OptIn(ExperimentalFoundationApi::class)
open class PreviewerTransformState(
    var scope: CoroutineScope = MainScope(),
    val pagerState: PagerState,
) {
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
        internal set
    var visible by mutableStateOf(false)
        internal set
    private var visibleTarget by mutableStateOf<Boolean?>(null)

    val canOpen: Boolean get() = !visible && visibleTarget == null && !animating
    val canClose: Boolean get() = visible && visibleTarget == null && !animating

    val mediaViewerState: MediaViewerState?
        get() = viewerContainerState?.viewerState

    var getKey: ((Int) -> Any)? = null

    fun findTransformItem(key: Any): TransformItemState? = transformItemStateMap[key]

    internal fun findTransformItemByIndex(index: Int): TransformItemState? {
        val key = getKey?.invoke(index) ?: return null
        return findTransformItem(key)
    }

    fun clearTransformItems() = transformItemStateMap.clear()

    suspend fun open(index: Int = 0, itemState: TransformItemState? = null) = doOpen(index, itemState)
    suspend fun close() = doClose()
    suspend fun openTransform(index: Int, itemState: TransformItemState) = doOpenTransform(index, itemState)
    suspend fun closeTransform() = doCloseTransform()
}
