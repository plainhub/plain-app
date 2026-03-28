package com.ismartcoding.plain.ui.base.pullrefresh

import androidx.compose.animation.core.Animatable
import androidx.compose.runtime.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.abs

@Stable
class RefreshLayoutState(
    internal val onRefreshListener: RefreshLayoutState.() -> Unit
) {
    internal val refreshContentState = mutableStateOf(RefreshContentState.Finished)
    internal val refreshContentOffsetState = Animatable(0f)
    internal val composePositionState = mutableStateOf(ComposePosition.Top)
    internal val refreshContentThresholdState = mutableFloatStateOf(0f)
    internal lateinit var coroutineScope: CoroutineScope
    var canCallRefreshListener = true

    internal val isScopeInitialized: Boolean get() = ::coroutineScope.isInitialized

    fun getRefreshContentState(): State<RefreshContentState> = refreshContentState

    fun createRefreshContentOffsetFlow(): kotlinx.coroutines.flow.Flow<Float> =
        snapshotFlow { refreshContentOffsetState.value }

    fun getComposePositionState(): State<ComposePosition> = composePositionState

    fun getRefreshContentThreshold(): Float = refreshContentThresholdState.floatValue

    fun getRefreshContentOffset(): Float = refreshContentOffsetState.value

    internal fun offsetHoming() {
        coroutineScope.launch {
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

    internal suspend fun animateToThreshold() {
        val composePosition = composePositionState.value
        if (composePosition == ComposePosition.Start || composePosition == ComposePosition.Top)
            refreshContentOffsetState.animateTo(refreshContentThresholdState.floatValue)
        else
            refreshContentOffsetState.animateTo(-refreshContentThresholdState.floatValue)
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
fun rememberRefreshLayoutState(onRefreshListener: RefreshLayoutState.() -> Unit) =
    remember { RefreshLayoutState(onRefreshListener) }