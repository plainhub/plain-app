package com.ismartcoding.plain.ui.base.pullrefresh

import androidx.compose.animation.core.Animatable
import androidx.compose.runtime.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlin.math.abs

@Stable
class RefreshLayoutState(
    internal val onRefreshListener: RefreshLayoutState.() -> Unit
) {
    //刷新布局内容区域的组件状态
    internal val refreshContentState = mutableStateOf(RefreshContentState.Finished)

    //刷新布局内容区域的Offset(位移)的状态和子内容区域的Offset(位移)的状态,如果contentIsMove==false,则一直为0,单位px
    internal val refreshContentOffsetState = Animatable(0f)

    //composePosition的状态,参考[RefreshLayout]的参数
    internal val composePositionState = mutableStateOf(ComposePosition.Top)

    //刷新布局拖动的阈值,单位px
    internal val refreshContentThresholdState = mutableFloatStateOf(0f)

    //协程作用域
    internal lateinit var coroutineScope: CoroutineScope

    //是否可以触发[onRefreshListener]
    var canCallRefreshListener = true

    /**
     * 获取刷新布局内容区域的组件状态
     * Get the [State] of the refresh content
     */
    fun getRefreshContentState(): State<RefreshContentState> = refreshContentState

    /**
     * 创建刷新布局内容区域的Offset(位移)的flow
     * Create the [Flow] of the offset
     */
    fun createRefreshContentOffsetFlow(): Flow<Float> =
        snapshotFlow { refreshContentOffsetState.value }

    /**
     * 获取composePosition的状态,参考[RefreshLayout]的参数
     * Get the [State] of the [ComposePosition]
     */
    fun getComposePositionState(): State<ComposePosition> = composePositionState

    /**
     * 获取刷新布局拖动的阈值,单位px
     * Get threshold of the refresh content
     */
    fun getRefreshContentThreshold(): Float = refreshContentThresholdState.floatValue

    /**
     * 刷新布局内容区域的Offset的值,单位px
     * Get the offset of content area
     */
    fun getRefreshContentOffset(): Float = refreshContentOffsetState.value

    /**
     * 设置刷新布局的状态
     * Set the state of refresh content
     */
    fun setRefreshState(state: RefreshContentState) {
        when (state) {
            RefreshContentState.Failed -> {
                if (refreshContentState.value == RefreshContentState.Failed)
                    return
                if (!this::coroutineScope.isInitialized)
                    throw IllegalStateException("[RefreshLayoutState]还未初始化完成,请在[LaunchedEffect]中或composable至少组合一次后使用此方法")
                coroutineScope.launch {
                    refreshContentState.value = RefreshContentState.Failed
                    delay(300)
                    refreshContentOffsetState.animateTo(0f)
                }
            }
            RefreshContentState.Finished -> {
                if (refreshContentState.value == RefreshContentState.Finished)
                    return
                if (!this::coroutineScope.isInitialized)
                    throw IllegalStateException("[RefreshLayoutState]还未初始化完成,请在[LaunchedEffect]中或composable至少组合一次后使用此方法")
                coroutineScope.launch {
                    refreshContentState.value = RefreshContentState.Finished
                    delay(300)
                    refreshContentOffsetState.animateTo(0f)
                }
            }

            RefreshContentState.Refreshing -> {
                if (refreshContentState.value == RefreshContentState.Refreshing)
                    return
                if (!this::coroutineScope.isInitialized)
                    throw IllegalStateException("[RefreshLayoutState]还未初始化完成,请在[LaunchedEffect]中或composable至少组合一次后使用此方法")
                coroutineScope.launch {
                    refreshContentState.value = RefreshContentState.Refreshing
                    if (canCallRefreshListener)
                        onRefreshListener()
                    else
                        setRefreshState(RefreshContentState.Finished)
                    animateToThreshold()
                }
            }

            RefreshContentState.Dragging -> throw IllegalStateException("设置为[RefreshContentState.Dragging]无意义")
        }
    }

    //偏移量归位,并检查是否超过了刷新阈值,如果超过了执行刷新逻辑
    internal fun offsetHoming() {
        coroutineScope.launch {
            //检查是否进入了刷新状态
            if (abs(refreshContentOffsetState.value) >= refreshContentThresholdState.floatValue) {
                refreshContentState.value = RefreshContentState.Refreshing
                if (canCallRefreshListener)
                    onRefreshListener()
                else
                    setRefreshState(RefreshContentState.Finished)
                animateToThreshold()
            } else {
                refreshContentOffsetState.animateTo(0f)
                refreshContentState.value = RefreshContentState.Finished
            }
        }
    }

    //动画滑动至阈值处
    private suspend fun animateToThreshold() {
        val composePosition = composePositionState.value
        if (composePosition == ComposePosition.Start || composePosition == ComposePosition.Top)
            refreshContentOffsetState.animateTo(refreshContentThresholdState.floatValue)
        else
            refreshContentOffsetState.animateTo(-refreshContentThresholdState.floatValue)
    }

    //增加偏移量
    internal fun offset(refreshContentOffset: Float) {
        coroutineScope.launch {
            val targetValue = refreshContentOffsetState.value + refreshContentOffset
            if (refreshContentState.value != RefreshContentState.Dragging && targetValue != 0f) {
                refreshContentState.value = RefreshContentState.Dragging
            }
            refreshContentOffsetState.snapTo(targetValue)
        }
    }
}

@Composable
fun rememberRefreshLayoutState(onRefreshListener: RefreshLayoutState.() -> Unit) =
    remember { RefreshLayoutState(onRefreshListener) }