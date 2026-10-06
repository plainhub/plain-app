package com.ismartcoding.plain.ui.base.pullrefresh

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.unit.Velocity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * 刷新期间**内容必须还能滚**。
 *
 * 2026-10-06 用户报：首页下拉刷新后「一直卡在正在刷新，只能等刷新结束后
 * 才能正常上下滚动」。真机复现坐实：`Refreshing` 状态下 `onPreScroll` 返回整个
 * `available`、`onPreFling` 返回整个 `available`，子列表一个像素都收不到，整页变成
 * 一块石头。刷新请求常常要好几秒（超时/重试），用户就被钉住干等。
 *
 * 正确语义：**刷新的只是刷新头，不是整页手势**。刷新中内容照常滚，刷新头钉在
 * threshold 上不动。所以这里断言的是「消费量」而不是「刷新头被移到哪」——后者由真机看。
 *
 * 这几条都不触发 `Animatable.animateTo`（刷新中 `offset()` 根本不会被调用），
 * 所以能留在 commonTest 让 iOS 目标也编一遍。需要真跑动画的状态机兜底测试在
 * `RefreshRecoveryTest`（androidHostTest，只有 JVM 版 coroutines-test 才有帧时钟）。
 */
class RefreshScrollLockTest {

    /**
     * `RefreshLayoutState.coroutineScope` 是 `lateinit`，真机上绑的是
     * `rememberCoroutineScope()`。这里给一个 `Dispatchers.Unconfined` 的替身：
     * 消费手势的两条路径会 `launch` 到它上面去跑 `snapTo`，而 `snapTo`
     * 只做一次赋值、不碰帧时钟，所以不需要 `runTest` 的调度器。
     */
    private suspend fun stateIn(state: RefreshContentState) =
        RefreshLayoutState {}.also {
            it.coroutineScope = CoroutineScope(Dispatchers.Unconfined)
            it.refreshContentState.value = state
            it.refreshContentOffsetState.snapTo(THRESHOLD)
        }

    private fun connection(state: RefreshLayoutState, refreshingCanScroll: Boolean = false) =
        RefreshLayoutNestedScrollConnection(
            composePosition = ComposePosition.Top,
            refreshLayoutState = state,
            dragEfficiency = 0.5f,
            orientationIsHorizontal = false,
            refreshingCanScroll = refreshingCanScroll,
        )

    /** 刷新中：预滚**不得**消费，滚动要能到达子列表。 */
    @Test
    fun pre_scroll_is_not_swallowed_while_refreshing() = runTest {
        val conn = connection(stateIn(RefreshContentState.Refreshing))

        assertEquals(
            Offset.Zero,
            conn.onPreScroll(Offset(0f, -320f), NestedScrollSource.UserInput),
            "刷新中把 onPreScroll 的手势全消费了，整页滚不动",
        )
        assertEquals(
            Offset.Zero,
            conn.onPreScroll(Offset(0f, 320f), NestedScrollSource.UserInput),
            "刷新中把反向手势也消费了",
        )
    }

    /**
     * 刷新中：`onPostScroll` **不得**消费余量，也**不得**再去动 offset。
     *
     * 动了 offset 有第二个后果：[RefreshLayoutState.offset] 会把状态从 `Refreshing`
     * 打回 `Dragging`，刷新头于是被手指越拽越长，「正在刷新」直接消失。
     */
    @Test
    fun post_scroll_leaves_the_header_pinned_while_refreshing() = runTest {
        val state = stateIn(RefreshContentState.Refreshing)
        val conn = connection(state)

        assertEquals(
            Offset.Zero,
            conn.onPostScroll(Offset.Zero, Offset(0f, 260f), NestedScrollSource.UserInput),
            "刷新中把余量也消费了，列表滑到边界后手指就没反应",
        )
        assertEquals(THRESHOLD, state.refreshContentOffsetState.value)
        assertEquals(
            RefreshContentState.Refreshing,
            state.refreshContentState.value,
            "刷新中被 offset() 打回 Dragging 了，刷新头会越拽越长",
        )
    }

    /** 刷新中：惯性速度**不得**被吞，子列表自己该滑多远滑多远。 */
    @Test
    fun pre_fling_does_not_swallow_the_velocity_while_refreshing() = runTest {
        val state = stateIn(RefreshContentState.Refreshing)
        val conn = connection(state)

        assertEquals(
            Velocity.Zero,
            conn.onPreFling(Velocity(0f, -2400f)),
            "刷新中把整段惯性速度吞了，松手后列表不滑",
        )
        // 也不能走到 offsetHoming()：刷新头停在 threshold 是刷新流程的地盘。
        assertEquals(RefreshContentState.Refreshing, state.refreshContentState.value)
    }

    /**
     * 反面对照：**不在刷新中**时该拦还是要拦。
     *
     * 没有这条，上面三条就可能退化成「永远不消费」——那下拉刷新本身就不工作了。
     */
    @Test
    fun dragging_still_consumes_so_pull_to_refresh_keeps_working() = runTest {
        val conn = connection(stateIn(RefreshContentState.Dragging))

        assertEquals(
            -40f,
            conn.onPreScroll(Offset(0f, -40f), NestedScrollSource.UserInput).y,
            "拖拽中把刷新头拉回去的那段手势没被消费，下拉刷新会失效",
        )
    }

    /**
     * `refreshingCanScroll = true` 时连刷新头也照常跟手——`VerticalRefreshableLayout`
     * 的加载更多那条链路就是它，这个开关不能被上面的改动顺手废掉。
     */
    @Test
    fun opting_out_of_the_pin_restores_the_old_drag_behaviour() = runTest {
        val conn = connection(stateIn(RefreshContentState.Refreshing), refreshingCanScroll = true)

        assertEquals(
            -40f,
            conn.onPreScroll(Offset(0f, -40f), NestedScrollSource.UserInput).y,
            "refreshingCanScroll = true 时不该再钉住刷新头",
        )
    }

    private companion object {
        const val THRESHOLD = 120f
    }
}
