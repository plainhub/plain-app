package com.ismartcoding.plain.ui.base.pullrefresh

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.unit.Velocity

internal class RefreshLayoutNestedScrollConnection(
    private val composePosition: ComposePosition,
    private val refreshLayoutState: RefreshLayoutState,
    private val dragEfficiency: Float,
    private val orientationIsHorizontal: Boolean,
) : NestedScrollConnection {
    override fun onPostScroll(
        consumed: Offset,
        available: Offset,
        source: NestedScrollSource,
    ): Offset {
        if (source == NestedScrollSource.Drag) {
            when (composePosition) {
                ComposePosition.Start -> {
                    val value = available.x
                    if (value > 0) {
                        if (value > 0.01f) {
                            refreshLayoutState.offset(value * dragEfficiency)
                        }
                        return Offset(value, 0f)
                    }
                }
                ComposePosition.End -> {
                    val value = available.x
                    if (value < 0) {
                        if (value < -0.01f) {
                            refreshLayoutState.offset(value * dragEfficiency)
                        }
                        return Offset(value, 0f)
                    }
                }
                ComposePosition.Top -> {
                    val value = available.y
                    if (value > 0) {
                        if (value > 0.01f) {
                            refreshLayoutState.offset(value * dragEfficiency)
                        }
                        return Offset(0f, value)
                    }
                }
                ComposePosition.Bottom -> {
                    val value = available.y
                    if (value < 0) {
                        if (value < -0.01f) {
                            refreshLayoutState.offset(value * dragEfficiency)
                        }
                        return Offset(0f, value)
                    }
                }
            }
        }
        return Offset.Zero
    }

    // 预先处理手势,返回消费的手势
    override fun onPreScroll(
        available: Offset,
        source: NestedScrollSource,
    ): Offset {
        if (refreshLayoutState.refreshContentState.value == RefreshContentState.Refreshing) {
            return if (orientationIsHorizontal) {
                Offset(available.x, 0f)
            } else {
                Offset(0f, available.y)
            }
        }
        val refreshOffset = refreshLayoutState.refreshContentOffsetState.value
        if (source == NestedScrollSource.Drag) {
            when (composePosition) {
                ComposePosition.Start -> {
                    if (available.x < 0 && refreshOffset > 0) {
                        var consumptive = available.x
                        if (-available.x > refreshOffset) {
                            consumptive = available.x - refreshOffset
                        }
                        refreshLayoutState.offset(consumptive * dragEfficiency)
                        return Offset(consumptive, 0f)
                    }
                }
                ComposePosition.End -> {
                    if (available.x > 0 && refreshOffset < 0) {
                        var consumptive = available.x
                        if (-available.x > refreshOffset) {
                            consumptive = available.x - refreshOffset
                        }
                        refreshLayoutState.offset(consumptive * dragEfficiency)
                        return Offset(consumptive, 0f)
                    }
                }
                ComposePosition.Top -> {
                    if (available.y < 0 && refreshOffset > 0) {
                        var consumptive = available.y
                        if (-available.y > refreshOffset) {
                            consumptive = available.y - refreshOffset
                        }
                        refreshLayoutState.offset(consumptive * dragEfficiency)
                        return Offset(0f, consumptive)
                    }
                }
                ComposePosition.Bottom -> {
                    if (available.y > 0 && refreshOffset < 0) {
                        // 消费的手势
                        var consumptive = available.y
                        if (-available.y < refreshOffset) {
                            consumptive = available.y - refreshOffset
                        }
                        refreshLayoutState.offset(consumptive * dragEfficiency)
                        return Offset(0f, consumptive)
                    }
                }
            }
        }
        return Offset.Zero
    }

    override suspend fun onPreFling(available: Velocity): Velocity {
        if (refreshLayoutState.refreshContentState.value == RefreshContentState.Refreshing) {
            return available
        }
        if (refreshLayoutState.refreshContentOffsetState.value != 0f) {
            refreshLayoutState.offsetHoming()
            return available
        }
        return Velocity.Zero
    }
}

@Composable
internal fun rememberRefreshLayoutNestedScrollConnection(
    composePosition: ComposePosition,
    refreshLayoutState: RefreshLayoutState,
    dragEfficiency: Float,
    orientationIsHorizontal: Boolean,
) = remember(composePosition) {
    RefreshLayoutNestedScrollConnection(
        composePosition,
        refreshLayoutState,
        dragEfficiency,
        orientationIsHorizontal,
    )
}
