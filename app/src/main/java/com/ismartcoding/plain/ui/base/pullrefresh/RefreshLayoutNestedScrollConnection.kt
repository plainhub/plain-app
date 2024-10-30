package com.ismartcoding.plain.ui.base.pullrefresh

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.unit.Velocity
import com.ismartcoding.lib.logcat.LogCat

internal class RefreshLayoutNestedScrollConnection(
    private val composePosition: ComposePosition,
    private val refreshLayoutState: RefreshLayoutState,
    private val dragEfficiency: Float,
    private val orientationIsHorizontal: Boolean,
    private val refreshingCanScroll: Boolean = false,
) : NestedScrollConnection {
    //处理子组件用不完的手势,返回消费的手势
    override fun onPostScroll(
        consumed: Offset,
        available: Offset,
        source: NestedScrollSource
    ): Offset {
        if (source == NestedScrollSource.UserInput) {
            when (composePosition) {
                ComposePosition.Start -> {
                    val value = available.x
                    if (value > 0) {
                        //过滤误差值(系统bug?)
                        if (value > 0.01f)
                            refreshLayoutState.offset(value * dragEfficiency)
                        return Offset(value, 0f)
                    }
                }
                ComposePosition.End -> {
                    val value = available.x
                    if (value < 0) {
                        if (value < -0.01f)
                            refreshLayoutState.offset(value * dragEfficiency)
                        return Offset(value, 0f)
                    }
                }
                ComposePosition.Top -> {
                    val value = available.y
                    if (value > 0) {
                        if (value > 0.01f)
                            refreshLayoutState.offset(value * dragEfficiency)
                        return Offset(0f, value)
                    }
                }
                ComposePosition.Bottom -> {
                    val value = available.y
                    if (value < 0) {
                        if (value < -0.01f)
                            refreshLayoutState.offset(value * dragEfficiency)
                        return Offset(0f, value)
                    }
                }
            }
        }
        return Offset.Zero
    }

    //预先处理手势,返回消费的手势
    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
        //如果是刷新中状态,并且刷新中不允许滚动,就拒绝对刷新区域和上下区域滚动
        if (!refreshingCanScroll && refreshLayoutState.refreshContentState.value == RefreshContentState.Refreshing) {
            return if (orientationIsHorizontal)
                Offset(available.x, 0f)
            else
                Offset(0f, available.y)
        }
        val refreshOffset = refreshLayoutState.refreshContentOffsetState.value
        if (source == NestedScrollSource.UserInput) {
            when (composePosition) {
                ComposePosition.Start -> {
                    if (available.x < 0 && refreshOffset > 0) {
                        //消费的手势
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
                        //消费的手势
                        var consumptive = available.x
                        if (-available.x > refreshOffset) {
                            consumptive = available.x - refreshOffset
                        }
                        refreshLayoutState.offset(consumptive * dragEfficiency)
                        return Offset(consumptive, 0f)
                    }
                }
                ComposePosition.Top -> {
//                    LogCat.d("ComposePosition.Top: ${available.y} ${refreshOffset}")
                    if (available.y < 0 && refreshOffset > 0) {
                        //消费的手势
                        var consumptive = available.y
                        if (-available.y > refreshOffset) {
                            consumptive = available.y - refreshOffset
                        }
                        refreshLayoutState.offset(consumptive * dragEfficiency)
                        return Offset(0f, consumptive)
                    }
                }
                ComposePosition.Bottom -> {
//                    LogCat.d("ComposePosition.Bottom: ${available.y} ${refreshOffset}")
                    if (available.y > 0 && refreshOffset < 0) {
                        //消费的手势
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

    //手势惯性滑动前回调,返回消费的速度,可以当做action_up
    override suspend fun onPreFling(available: Velocity): Velocity {
        //如果是刷新中状态,并且刷新中不允许滚动,就拒绝对刷新区域和上下区域滚动
        if (!refreshingCanScroll && refreshLayoutState.refreshContentState.value == RefreshContentState.Refreshing) {
            return available
        }
        if (refreshLayoutState.refreshContentOffsetState.value != 0f) {
            refreshLayoutState.offsetHoming()
            return available
        }
        return Velocity.Zero
    }
}
