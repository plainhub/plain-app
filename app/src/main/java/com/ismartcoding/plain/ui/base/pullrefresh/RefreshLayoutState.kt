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
    internal val onRefreshListener: RefreshLayoutState.() -> Unit,
) {
    internal val refreshContentState = mutableStateOf(RefreshContentState.Stop)

    internal val refreshContentOffsetState = Animatable(0f)

    internal val composePositionState = mutableStateOf(ComposePosition.Top)

    internal val refreshContentThresholdState = mutableFloatStateOf(0f)

    internal lateinit var coroutineScope: CoroutineScope

    fun getRefreshContentState(): State<RefreshContentState> = refreshContentState

    fun createRefreshContentOffsetFlow(): Flow<Float> = snapshotFlow { refreshContentOffsetState.value }

    fun getComposePositionState(): State<ComposePosition> = composePositionState

    fun getRefreshContentThreshold(): Float = refreshContentThresholdState.value

    fun getRefreshContentOffset(): Float = refreshContentOffsetState.value

    fun setRefreshState(state: RefreshContentState) {
        when (state) {
            RefreshContentState.Stop -> {
                if (refreshContentState.value == RefreshContentState.Stop) {
                    return
                }
                if (!this::coroutineScope.isInitialized) {
                    throw IllegalStateException("[RefreshLayoutState]还未初始化完成,请在[LaunchedEffect]中或composable至少组合一次后使用此方法")
                }
                coroutineScope.launch {
                    refreshContentState.value = RefreshContentState.Stop
                    delay(300)
                    refreshContentOffsetState.animateTo(0f)
                }
            }
            RefreshContentState.Refreshing -> {
                if (refreshContentState.value == RefreshContentState.Refreshing) {
                    return
                }
                if (!this::coroutineScope.isInitialized) {
                    throw IllegalStateException("[RefreshLayoutState]还未初始化完成,请在[LaunchedEffect]中或composable至少组合一次后使用此方法")
                }
                coroutineScope.launch {
                    refreshContentState.value = RefreshContentState.Refreshing
                    onRefreshListener()
                    animateToThreshold()
                }
            }
            RefreshContentState.Dragging -> throw IllegalStateException("设置为[RefreshContentStateEnum.Dragging]无意义")
        }
    }

    internal fun offsetHoming() {
        coroutineScope.launch {
            if (abs(refreshContentOffsetState.value) >= refreshContentThresholdState.value) {
                refreshContentState.value = RefreshContentState.Refreshing
                onRefreshListener()
                animateToThreshold()
            } else {
                refreshContentOffsetState.animateTo(0f)
                refreshContentState.value = RefreshContentState.Stop
            }
        }
    }

    private suspend fun animateToThreshold() {
        val composePosition = composePositionState.value
        if (composePosition == ComposePosition.Start || composePosition == ComposePosition.Top) {
            refreshContentOffsetState.animateTo(refreshContentThresholdState.value)
        } else {
            refreshContentOffsetState.animateTo(-refreshContentThresholdState.value)
        }
    }

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
fun rememberRefreshLayoutState(onRefreshListener: RefreshLayoutState.() -> Unit) = remember { RefreshLayoutState(onRefreshListener) }
