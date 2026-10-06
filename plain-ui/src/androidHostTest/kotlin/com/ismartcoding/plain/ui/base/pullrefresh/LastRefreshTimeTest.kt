package com.ismartcoding.plain.ui.base.pullrefresh

import androidx.compose.runtime.MonotonicFrameClock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * 「上次刷新时间」的**打点时机**与**可恢复性**。
 *
 * 2026-10-06 用户要求：记录上次刷新时间，下拉时显示。这里钉住最容易做错的两点：
 * ① 打在**成功**那一刻，不是刷新开始那一刻——后者反映的是「上次尝试的时间」，
 *     用户看到会以为数据是那时候的；
 * ② 失败/异常兜底**不许**打点。
 *
 * 放 `androidHostTest`：状态转移走 `setRefreshState` → `delay(300)` →
 * `Animatable.animateTo`，帧时钟要在 commonMain 缺席的 JVM 上自己实现，
 * 见 [AdvancingFrameClock]。
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LastRefreshTimeTest {

    private fun TestScope.newState(
        clock: MonotonicFrameClock,
        onRefresh: RefreshLayoutState.() -> Unit = {},
    ) = RefreshLayoutState(onRefresh).also {
        it.coroutineScope = CoroutineScope(StandardTestDispatcher(testScheduler).plus(clock))
    }

    /** 没刷过时是 null：刷新头那行直接不显示，而不是显示「上次刷新：无」。 */
    @Test
    fun nothing_is_recorded_before_the_first_refresh() {
        val clock = AdvancingFrameClock()
        runTest(clock) {
            val state = newState(clock)
            assertNull(state.getLastRefreshTime().value)
            assertNull(state.millisSinceLastRefresh())
        }
    }

    @Test
    fun a_finished_refresh_stamps_the_moment_of_success() {
        val clock = AdvancingFrameClock()
        runTest(clock) {
            var now = 1_700_000_000_000L
            val state = newState(clock).also { it.injectClock { now } }
            state.refreshContentState.value = RefreshContentState.Refreshing

            now += 12_345L
            state.setRefreshState(RefreshContentState.Finished)
            advanceUntilIdle()

            assertEquals(
                1_700_000_012_345L,
                state.getLastRefreshTime().value,
                "打点取的是完成那一刻，不是刷新开始那一刻",
            )
            assertEquals(0L, state.millisSinceLastRefresh())
        }
    }

    /** 失败不许打点：数据没变新，说「刚刚刷新过」是撒谎。 */
    @Test
    fun a_failed_refresh_does_not_stamp() {
        val clock = AdvancingFrameClock()
        runTest(clock) {
            val state = newState(clock).also { it.injectClock { 1_700_000_000_000L } }
            state.refreshContentState.value = RefreshContentState.Refreshing

            state.setRefreshState(RefreshContentState.Failed)
            advanceUntilIdle()

            assertNull(
                state.getLastRefreshTime().value,
                "刷新失败也记了时间，等于骗用户说数据是最新的",
            )
        }
    }

    /** 异常兜底（回调炸了）同样不许打点——它连数据都没拿到。 */
    @Test
    fun the_exception_fallback_does_not_stamp() {
        val clock = AdvancingFrameClock()
        runTest(clock) {
            val state = newState(clock, onRefresh = { error("boom") }).also {
                it.injectClock { 1_700_000_000_000L }
            }
            state.refreshContentState.value = RefreshContentState.Refreshing

            state.runRefreshListener()
            advanceUntilIdle()

            assertEquals(RefreshContentState.Finished, state.refreshContentState.value)
            assertNull(state.getLastRefreshTime().value)
        }
    }

    /** 两次刷新之间时间要往前推，显示的「几分钟前」才不是永远 0。 */
    @Test
    fun the_elapsed_time_grows_with_every_refresh() {
        val clock = AdvancingFrameClock()
        runTest(clock) {
            var now = 1_000_000L
            val state = newState(clock).also { it.injectClock { now } }

            state.refreshContentState.value = RefreshContentState.Refreshing
            state.setRefreshState(RefreshContentState.Finished)
            advanceUntilIdle()
            assertEquals(0L, state.millisSinceLastRefresh())

            now += 5 * 60_000L
            assertEquals(5 * 60_000L, state.millisSinceLastRefresh())

            state.refreshContentState.value = RefreshContentState.Refreshing
            state.setRefreshState(RefreshContentState.Finished)
            advanceUntilIdle()
            assertEquals(0L, state.millisSinceLastRefresh(), "第二次刷新后应重新计时")

            now += 3 * 3_600_000L
            assertEquals(3 * 3_600_000L, state.millisSinceLastRefresh())
        }
    }

    /**
     * 调用方可以从存储灌回来（组件自己不管持久化）。
     *
     * 灌进来的时间可能比当前时刻新（时区/时钟被改过），负差必须夹成 0，
     * 否则会显示「上次刷新：-3 分钟前」。
     */
    @Test
    fun a_restored_timestamp_is_accepted_and_never_goes_negative() {
        val clock = AdvancingFrameClock()
        runTest(clock) {
            val state = newState(clock).also { it.injectClock { 1_000_000L } }

            state.setLastRefreshTime(1_000_000L - 90_000L)
            assertEquals(90_000L, state.millisSinceLastRefresh())

            state.setLastRefreshTime(1_000_000L + 60_000L)
            assertEquals(0L, state.millisSinceLastRefresh(), "未来时间被夹成了 0，没变成负数")

            state.setLastRefreshTime(null)
            assertNull(state.millisSinceLastRefresh(), "清空后刷新头那行应该重新隐藏")
        }
    }

    /** 默认时钟必须真的在走，不能是 EditHistory 那种 `{ 0L }` 占位。 */
    @Test
    fun the_default_clock_is_real_not_a_zero_placeholder() {
        val state = RefreshLayoutState {}
        state.setLastRefreshTime(0L)
        val elapsed = state.millisSinceLastRefresh() ?: -1L
        assertTrue(
            elapsed > 1_600_000_000_000L,
            "默认时钟返回 $elapsed，看着像占位的 0/小值",
        )
    }
}