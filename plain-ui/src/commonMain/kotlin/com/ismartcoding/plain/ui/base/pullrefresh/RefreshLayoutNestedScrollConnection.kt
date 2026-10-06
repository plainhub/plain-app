package com.ismartcoding.plain.ui.base.pullrefresh

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.unit.Velocity
import com.ismartcoding.plain.lib.logcat.LogCat

internal class RefreshLayoutNestedScrollConnection(
    private val composePosition: ComposePosition,
    private val refreshLayoutState: RefreshLayoutState,
    private val dragEfficiency: Float,
    private val orientationIsHorizontal: Boolean,
    private val refreshingCanScroll: Boolean = false,
) : NestedScrollConnection {

    /**
     * 刷新中、且调用方不要「边刷边滚」。
     *
     * 注意这个标志**只是「别让刷新头跟着手指动」的锁，不是滚动锁**。
     * 之前实现反了：`onPreScroll` / `onPreFling` 在这个状态下把整个手势和整个速度
     * 全量消费掉，子列表一个像素都收不到——刷新期间整页变成一块石头，要等刷新
     * 结束才能滚。刷新请求经常要好几秒（超时/重试），用户就被钉在那儿干等。
     *
     * 正确语义：刷新中内容照常滚，刷新头**钉在 threshold 上不动**。
     */
    private val headerPinnedDuringRefresh: Boolean
        get() =
            !refreshingCanScroll &&
                refreshLayoutState.refreshContentState.value == RefreshContentState.Refreshing

    //处理子组件用不完的手势,返回消费的手势
    override fun onPostScroll(
        consumed: Offset,
        available: Offset,
        source: NestedScrollSource
    ): Offset {
        // 刷新头钉住时**不碰 offset**：一旦碰了，offset() 会把状态从 Refreshing
        // 打回 Dragging，刷新头就被手指越拽越长。
        if (headerPinnedDuringRefresh) return Offset.Zero
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
        // 刷新中:返回 Zero —— 子列表照常滚,刷新头保持钉在 threshold 上。
        // 这里原来 return 整个 available,等于把整页手势吞掉。
        if (headerPinnedDuringRefresh) return Offset.Zero
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
        // 刷新中:Zero —— 让子列表自己飞。
        // 这里原来 return available(整段速度都吞掉),松手后列表不会滑。
        // 也不能走到下面的 offsetHoming():刷新头本来就停在 threshold 上,
        // 那是刷新流程的地盘,不是手指的。
        if (headerPinnedDuringRefresh) return Velocity.Zero
        if (refreshLayoutState.refreshContentOffsetState.value != 0f) {
            refreshLayoutState.offsetHoming()
            return available
        }
        return Velocity.Zero
    }
}
