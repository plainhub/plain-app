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
import kotlin.test.assertTrue

/**
 * 刷新状态**不能永久卡死**，而且收尾要收干净。
 *
 * 2026-10-06 用户报「下拉刷新一直卡在正在刷新」。这里钉两个一起出现的坑：
 *
 * ① 回调抛异常时，外层收尾不执行，状态永远停在 `Refreshing`；
 * ② 就算兜底把状态收成了 `Finished`，调用方还会**再**把刷新头顶回 threshold，
 *    状态说「已完成」而头挂在半空，页面再也对不齐、也拖不动。
 *
 * 放 `androidHostTest` 而不是 `commonTest`：兜底路径要真跑
 * `Animatable.animateTo`，而帧时钟要在 commonMain 缺席的 JVM 上自己实现，
 * 见 [AdvancingFrameClock]。
 *
 * **读文件的测试不存在，本文件只测行为**；读文件类的守卫都在 androidHostTest 的
 * 其它文件里。
 */
@OptIn(ExperimentalCoroutinesApi::class)
class RefreshRecoveryTest {

    /**
     * 造一个能在 `runTest` 里自洽跑起来的 state。
     *
     * 帧时钟要挂两处，缺一不可：
     * ① `state.coroutineScope` —— [offsetHoming] / [setRefreshState] 都是
     *    `launch` 到它上面跑的，`Animatable` 在那个协程里解析帧时钟；
     * ② `runTest(clock)` 的 context —— 本文件里也有直接 `suspend` 调
     *    [runRefreshListener] 的用例，那条路的动画跑在**测试体协程**上。
     * 只挂一处，动画就会因为找不到帧时钟而抛异常，或者被 `advanceUntilIdle` 干等。
     */
    private fun TestScope.newState(
        clock: MonotonicFrameClock,
        onRefresh: RefreshLayoutState.() -> Unit,
        canCallRefreshListener: Boolean = true,
    ) = RefreshLayoutState(onRefresh).also {
        it.canCallRefreshListener = canCallRefreshListener
        it.coroutineScope = CoroutineScope(StandardTestDispatcher(testScheduler).plus(clock))
        it.refreshContentThresholdState.floatValue = THRESHOLD
    }

    @Test
    fun a_throwing_refresh_callback_does_not_wedge_the_state() {
        val clock = AdvancingFrameClock()
        runTest(clock) {
            var called = false
            val state = newState(clock, onRefresh = {
                called = true
                error("模拟回调炸了")
            })
            state.refreshContentState.value = RefreshContentState.Refreshing

            state.runRefreshListener()
            advanceUntilIdle()

            assertTrue(called, "回调根本没被调用，测的就不是这条路径")
            assertEquals(
                RefreshContentState.Finished,
                state.refreshContentState.value,
                "回调抛异常后状态没收尾，这一页会永久卡在正在刷新",
            )
            assertEquals(
                0f,
                state.refreshContentOffsetState.value,
                "刷新头没收回去（offset 没归零）",
            )
        }
    }

    /**
     * 同一条兜底路径：`canCallRefreshListener = false`（例如已加载完）也要收尾，
     * **而且刷新头必须留在 0**。
     *
     * 这条当初只断言了状态，于是漏掉了「兜底收完之后又被 `animateToThreshold()`
     * 顶回 threshold」那个 bug——状态确实回到了 Finished，但头挂在那儿，
     * 肉眼看到的就是「还是拉不动 / 对不齐」。断言必须连 offset 一起钉。
     */
    @Test
    fun a_disabled_listener_finishes_without_invoking_it() {
        val clock = AdvancingFrameClock()
        runTest(clock) {
            var called = false
            val state = newState(clock, onRefresh = { called = true }, canCallRefreshListener = false)
            state.refreshContentState.value = RefreshContentState.Refreshing

            state.runRefreshListener()
            advanceUntilIdle()

            assertTrue(!called, "canCallRefreshListener = false 时不该回调")
            assertEquals(RefreshContentState.Finished, state.refreshContentState.value)
            assertEquals(
                0f,
                state.refreshContentOffsetState.value,
                "兜底已经把刷新头收回 0 了，调用方不该再把它顶回 threshold",
            )
        }
    }

    /**
     * 端到端：真的走 [RefreshLayoutState.offsetHoming]（松手触发刷新的那条路），
     * 回调炸了之后状态必须回到 Finished、刷新头必须回到 0。
     *
     * 这是唯一能盖住「`runRefreshListener` 兜底收完 → `offsetHoming` 又
     * `animateToThreshold()`」那条控制流的用例：直接调 `runRefreshListener`
     * 的用例走不到调用方那一步。
     */
    @Test
    fun offset_homing_recovers_from_a_throwing_callback() {
        val clock = AdvancingFrameClock()
        runTest(clock) {
            val state = newState(clock, onRefresh = { error("模拟回调炸了") })
            state.refreshContentOffsetState.snapTo(THRESHOLD + 30f)

            state.offsetHoming()
            advanceUntilIdle()

            assertEquals(
                RefreshContentState.Finished,
                state.refreshContentState.value,
                "松手触发刷新后回调抛异常，状态没收尾",
            )
            assertEquals(
                0f,
                state.refreshContentOffsetState.value,
                "状态都收成 Finished 了，刷新头还挂在 threshold 上，页面再也对不齐",
            )
        }
    }

    /**
     * 端到端的另一条入口：[setRefreshState] 显式请求刷新时同样不能把头顶回去。
     *
     * 和上面那条是两段独立的调用方代码，各写各的，很容易只修一处。
     */
    @Test
    fun requesting_refreshing_does_not_re_raise_the_header_after_recovery() {
        val clock = AdvancingFrameClock()
        runTest(clock) {
            val state = newState(clock, onRefresh = { error("模拟回调炸了") })

            state.setRefreshState(RefreshContentState.Refreshing)
            advanceUntilIdle()

            assertEquals(RefreshContentState.Finished, state.refreshContentState.value)
            assertEquals(
                0f,
                state.refreshContentOffsetState.value,
                "setRefreshState(Refreshing) 那条路也把已收好的刷新头顶回去了",
            )
        }
    }

    /** 对照：回调正常时状态保持 Refreshing，刷新头停在 threshold 上交给页面自己收尾。 */
    @Test
    fun a_successful_callback_leaves_the_state_to_the_caller() {
        val clock = AdvancingFrameClock()
        runTest(clock) {
            val state = newState(clock, onRefresh = { })
            state.refreshContentState.value = RefreshContentState.Refreshing

            state.runRefreshListener()
            advanceUntilIdle()

            assertEquals(
                RefreshContentState.Refreshing,
                state.refreshContentState.value,
                "回调没出错时组件不该替页面改状态——页面要能表达 Finished/Failed",
            )
        }
    }

    /**
     * 对照的另一半：回调正常时头**要**停在 threshold 上。
     *
     * 有了上面那条「兜底时别顶」的守卫，很容易顺手把正常路径的停留也一起干掉，
     * 那样刷新头就会在刷新过程中缩回去、「正在刷新」看不见了。这条钉住它。
     */
    @Test
    fun a_successful_callback_still_parks_the_header_at_the_threshold() {
        val clock = AdvancingFrameClock()
        runTest(clock) {
            val state = newState(clock, onRefresh = { })

            state.setRefreshState(RefreshContentState.Refreshing)
            advanceUntilIdle()

            assertEquals(RefreshContentState.Refreshing, state.refreshContentState.value)
            assertEquals(
                THRESHOLD,
                state.refreshContentOffsetState.value,
                "正常刷新时刷新头不该缩回去，缩了就看不到「正在刷新」了",
            )
        }
    }

    private companion object {
        const val THRESHOLD = 120f
    }
}