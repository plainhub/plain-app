package com.ismartcoding.plain.ui.components.mediaviewer.previewer

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.ismartcoding.plain.ui.components.mediaviewer.MediaViewerState
import com.ismartcoding.plain.ui.components.mediaviewer.Ticket
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@OptIn(ExperimentalFoundationApi::class)
open class PreviewerTransformState(
    var scope: CoroutineScope = MainScope(),
    val pagerState: PagerState,
) {
    // 锁对象
    private var mutex = Mutex()

    // 打开回调，最外层animateVisible修改时调用
    private var openCallback: (() -> Unit)? = null

    // 关闭回调，最外层animateVisible修改时调用
    private var closeCallback: (() -> Unit)? = null

    // 是否显示viewer容器的标识
    private val viewerContainerVisible: Boolean
        get() = viewerContainerState?.viewerContainerAlpha?.value == 1F

    /**
     * 更新当前的标记状态
     * @param animating Boolean
     * @param visible Boolean
     * @param visibleTarget Boolean?
     */
    private suspend fun updateState(animating: Boolean, visible: Boolean, visibleTarget: Boolean?) {
        mutex.withLock {
            this.animating = animating
            this.visible = visible
            this.visibleTarget = visibleTarget
        }
    }

    // 等待界面刷新的ticket
    internal val ticket = Ticket()

    // 最外侧animateVisibleState
    internal var animateContainerVisibleState by mutableStateOf(MutableTransitionState(false))

    // UI透明度
    internal var uiAlpha = Animatable(0F)

    // viewer透明度
    internal var viewerAlpha = Animatable(1F)

    // 从外部传入viewer容器
    internal var viewerContainerState by mutableStateOf<ViewerContainerState?>(null)

    // 从外部提供transformContentState
    internal val transformState: TransformContentState?
        get() = viewerContainerState?.transformState

    // 判断是否允许transform结束
    internal val canTransformOut: Boolean
        get() = (viewerContainerState?.openTransformJob != null) || (mediaViewerState?.mountedFlow?.value == true)

    // 标记打开动作，执行开始
    private suspend fun stateOpenStart() =
        updateState(animating = true, visible = false, visibleTarget = true)

    // 标记打开动作，执行结束
    private suspend fun stateOpenEnd() =
        updateState(animating = false, visible = true, visibleTarget = null)

    // 标记关闭动作，执行开始
    internal suspend fun stateCloseStart() =
        updateState(animating = true, visible = true, visibleTarget = false)

    // 标记关闭动作，执行结束
    internal suspend fun stateCloseEnd() =
        updateState(animating = false, visible = false, visibleTarget = null)

    /**
     * 转换图层转viewer图层，true显示viewer，false显示转换图层
     * @param isViewer Boolean
     */
    internal suspend fun transformSnapToViewer(isViewer: Boolean) {
        if (isViewer && visibleTarget == false) return
        viewerContainerState?.transformSnapToViewer(isViewer)
    }

    /**
     * animateVisible执行完成后调用回调方法
     */
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

    // 是否正在进行动画
    var animating by mutableStateOf(false)
        internal set

    // 是否可见
    var visible by mutableStateOf(false)
        internal set

    // 是否可见的目标值
    private var visibleTarget by mutableStateOf<Boolean?>(null)

    // 是否允许执行open操作
    val canOpen: Boolean
        get() = !visible && visibleTarget == null && !animating

    // 是否允许执行close操作
    val canClose: Boolean
        get() = visible && visibleTarget == null && !animating

    // imageViewer状态对象
    val mediaViewerState: MediaViewerState?
        get() = viewerContainerState?.viewerState

    /**
     * 根据页面获取当前页码所属的key
     */
    var getKey: ((Int) -> Any)? = null

    // 查找key关联的transformItem
    fun findTransformItem(key: Any): TransformItemState? {
        return transformItemStateMap[key]
    }

    // 根据index查询key
    private fun findTransformItemByIndex(index: Int): TransformItemState? {
        val key = getKey?.invoke(index) ?: return null
        return findTransformItem(key)
    }

    // 清除全部transformItems
    fun clearTransformItems() = transformItemStateMap.clear()

    suspend fun open(
        index: Int = 0,
        itemState: TransformItemState? = null,
    ) =
        suspendCoroutine<Unit> { c ->
            // 设置转换回调
            openCallback = {
                c.resume(Unit)
                // 清除转换回调
                openCallback = null
                // 标记结束
                scope.launch {
                    stateOpenEnd()
                }
            }
            scope.launch {
                showActions = true
                // 标记开始
                stateOpenStart()
                // 开启UI
                uiAlpha.snapTo(1F)
                // container动画立即设置为关闭
                animateContainerVisibleState = MutableTransitionState(false)
                // 开启container
                animateContainerVisibleState.targetState = true
                // 跳转到index
                pagerState.scrollToPage(index)
                // 等待下一帧之后viewerContainerState才会刷新出来
                ticket.awaitNextTicket()
                // 允许显示loading
                viewerContainerState?.showLoading = true
                // 开启viewer
                viewerContainerState?.viewerContainerAlpha?.snapTo(1F)
                // 如果输入itemState，则用itemState做为背景
                if (itemState != null) {
                    scope.launch {
                        viewerContainerState?.transformContentAlpha?.snapTo(1F)
                        transformState?.awaitContainerSizeSpecifier()
                        transformState?.enterTransform(itemState, tween(0))
                    }
                }
                // 这里需要等待viewer挂载，显示loading界面
                viewerContainerState?.awaitOpenTransform()
            }
        }

    suspend fun close() = suspendCoroutine<Unit> { c ->
        // 设置退出结束的回调方法
        closeCallback = {
            c.resume(Unit)
            // 将回调设置为空
            closeCallback = null
            // 标记结束
            scope.launch {
                stateCloseEnd()
            }
        }
        scope.launch {
            // 标记开始
            stateCloseStart()
            // 关闭正在进行的开启操作
            viewerContainerState?.cancelOpenTransform()
            listOf(
                scope.async {
                    // 退出结束后隐藏content
                    viewerContainerState?.transformContentAlpha?.snapTo(0F)
                },
                scope.async {
                    // 动画隐藏UI
                    uiAlpha.animateTo(0F, DEFAULT_SOFT_ANIMATION_SPEC)
                },
                scope.async {
                    animateContainerVisibleState = MutableTransitionState(false)
                }
            ).awaitAll()
            showActions = true
            ticket.awaitNextTicket()
            transformState?.setExitState()
        }
    }

    suspend fun openTransform(
        index: Int,
        itemState: TransformItemState
    ) {
        stateOpenStart()
        uiAlpha.snapTo(0F)
        viewerAlpha.snapTo(0F)
        // 设置新的container状态立刻设置为true
        animateContainerVisibleState = MutableTransitionState(true)
        // 跳转到index页
        pagerState.scrollToPage(index)
        // 等待下一帧
        ticket.awaitNextTicket()
        // 关闭loading
        viewerContainerState?.showLoading = false
        // 关闭viewer。打开transform
        transformSnapToViewer(false)
        // 开启viewer
        viewerAlpha.snapTo(1F)
        // 这两个一起执行
        listOf(
            scope.async {
                // 开启动画
                transformState?.enterTransform(itemState, DEFAULT_SOFT_ANIMATION_SPEC)
                // 开启loading
                viewerContainerState?.showLoading = true
            },
            scope.async {
                // UI慢慢显示
                uiAlpha.animateTo(1F, DEFAULT_SOFT_ANIMATION_SPEC)
            }
        ).awaitAll()
        // 执行完成后的回调
        stateOpenEnd()
        // 这里需要等待viewer挂载，显示loading界面
        viewerContainerState?.awaitOpenTransform()
    }

    suspend fun closeTransform() {
        stateCloseStart()
        // 判断当前状态是否允许transform结束
        // 需要在cancel前获取该值
        val canNowTransformOut = canTransformOut
        // 关闭可能正在进行的open操作
        viewerContainerState?.cancelOpenTransform()
        // 关闭loading的显示
        viewerContainerState?.showLoading = false
        // 查询item是否存在
        val itemState = findTransformItemByIndex(pagerState.currentPage)
        // 如果存在，就transform退出，否则就普通退出
        if (itemState != null && canNowTransformOut) {
            // 如果viewer在显示的状态，退出时将viewer的pose复制给content
            if (viewerContainerVisible) {
                // 标记transform的开始状态，否则copy无效
                transformState?.setEnterState()
                // 更新transformState
                transformState?.notifyEnterChanged()
                // 等待刷新完毕
                ticket.awaitNextTicket()
                // 复制viewer的pos给transform
                viewerContainerState?.copyViewerPosToContent(itemState)
                // 切换为transform
                transformSnapToViewer(false)
            }
            // 等待下一帧
            ticket.awaitNextTicket()
            listOf(
                scope.async {
                    // transform动画退出
                    transformState?.exitTransform(DEFAULT_SOFT_ANIMATION_SPEC)
                    // 退出结束后隐藏content
                    viewerContainerState?.transformContentAlpha?.snapTo(0F)
                },
                scope.async {
                    // 动画隐藏UI
                    uiAlpha.animateTo(0F, DEFAULT_SOFT_ANIMATION_SPEC)
                }
            ).awaitAll()
            // 等待下一帧
            ticket.awaitNextTicket()
            // 彻底关闭container
            animateContainerVisibleState = MutableTransitionState(false)
        } else {
            // transform标记退出
            transformState?.setExitState()
            // container动画退出
            animateContainerVisibleState.targetState = false
        }

        viewerContainerState?.showLoading = true
        showActions = true

        stateCloseEnd()
    }

}