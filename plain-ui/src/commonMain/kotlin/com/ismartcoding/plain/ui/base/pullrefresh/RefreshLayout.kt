package com.ismartcoding.plain.ui.base.pullrefresh

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import kotlin.math.roundToInt

@Composable
fun RefreshLayout(
    refreshContent: @Composable RefreshLayoutState.() -> Unit,
    refreshLayoutState: RefreshLayoutState,
    modifier: Modifier = Modifier,
    refreshContentThreshold: Dp? = null,
    composePosition: ComposePosition = ComposePosition.Top,
    dragEfficiency: Float = 0.5f,
    userEnable: Boolean = true,
    refreshingCanScroll: Boolean = false,
    content: @Composable () -> Unit,
) {
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()
    //更新状态
    val orientationIsHorizontal = remember(
        refreshLayoutState,
        composePosition,
        refreshContentThreshold,
        coroutineScope,
        density,
    ) {
        refreshLayoutState.composePositionState.value = composePosition
        refreshLayoutState.coroutineScope = coroutineScope
        if (refreshContentThreshold != null)
            refreshLayoutState.refreshContentThresholdState.floatValue =
                with(density) { refreshContentThreshold.toPx() }
        composePosition.isHorizontal()
    }
    val nestedScrollState = remember(composePosition) {
        RefreshLayoutNestedScrollConnection(
            composePosition, refreshLayoutState, dragEfficiency, orientationIsHorizontal, refreshingCanScroll
        )
    }

    Layout(
        content = {
            content()
            refreshLayoutState.refreshContent()
        },
        modifier = modifier
            .let {
                if (userEnable) {
                    it.nestedScroll(nestedScrollState)
                } else {
                    it
                }
            }
            .clipScrollableContainer(composePosition.orientation)
    ) { measurableList, constraints ->
        val contentPlaceable =
            measurableList[0].measure(constraints.copy(minWidth = 0, minHeight = 0))
        //宽或高不能超过content(根据方向来定)
        val refreshContentPlaceable = measurableList[1].measure(
            Constraints(
                maxWidth = if (orientationIsHorizontal) Constraints.Infinity else contentPlaceable.width,
                maxHeight = if (orientationIsHorizontal) contentPlaceable.height else Constraints.Infinity,
            )
        )
        if (refreshContentThreshold == null && refreshLayoutState.refreshContentThresholdState.floatValue == 0f) {
            refreshLayoutState.refreshContentThresholdState.floatValue =
                if (orientationIsHorizontal) {
                    refreshContentPlaceable.width.toFloat()
                } else {
                    refreshContentPlaceable.height.toFloat()
                }
        }

        layout(contentPlaceable.width, contentPlaceable.height) {
            val offset = refreshLayoutState.refreshContentOffsetState.value.roundToInt()
            when (composePosition) {
                ComposePosition.Start -> {
                    contentPlaceable.placeRelative(offset, 0)
                    refreshContentPlaceable.placeRelative(
                        (-refreshContentPlaceable.width) + offset,
                        0
                    )
                }

                ComposePosition.End -> {
                    contentPlaceable.placeRelative(offset, 0)
                    refreshContentPlaceable.placeRelative(
                        contentPlaceable.width + offset,
                        0
                    )
                }

                ComposePosition.Top -> {
                    contentPlaceable.placeRelative(0, offset)
                    refreshContentPlaceable.placeRelative(
                        0,
                        (-refreshContentPlaceable.height) + offset
                    )
                }

                ComposePosition.Bottom -> {
                    contentPlaceable.placeRelative(0, offset)
                    refreshContentPlaceable.placeRelative(
                        0,
                        contentPlaceable.height + offset
                    )
                }
            }
        }
    }
}
