package com.ismartcoding.plain.ui.base.pullrefresh

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * 「上次刷新」的相对时间分桶。
 *
 * 分桶是纯函数，所以能穷举；渲染那一层有 `stringResource`，只能在真机上看。
 */
class LastRefreshBucketTest {

    @Test
    fun `under a minute reads as just now`() {
        assertEquals(LastRefreshBucket.JustNow to 0L, lastRefreshBucket(0L))
        assertEquals(LastRefreshBucket.JustNow to 0L, lastRefreshBucket(59_999L))
    }

    /**
     * 边界是「取整到第 0 分钟也不显示 0 分钟」。
     *
     * 正好 60s 就进 Minutes 桶，`60_000 / 60_000 = 1` → 「1 分钟前」。
     * 若写成 `<=`，59_999ms 会算出 `59_999 / 60_000 = 0` → 「上次刷新：0 分钟前」，
     * 那读起来像坏了。
     */
    @Test
    fun `a minute boundary never renders zero minutes`() {
        assertEquals(LastRefreshBucket.Minutes to 1L, lastRefreshBucket(60_000L))
        assertEquals(LastRefreshBucket.Minutes to 1L, lastRefreshBucket(119_999L))
        assertEquals(LastRefreshBucket.Minutes to 2L, lastRefreshBucket(120_000L))
    }

    @Test
    fun `minutes roll up to hours at exactly one hour`() {
        assertEquals(LastRefreshBucket.Minutes to 59L, lastRefreshBucket(59 * 60_000L))
        assertEquals(LastRefreshBucket.Hours to 1L, lastRefreshBucket(3_600_000L))
        assertEquals(LastRefreshBucket.Hours to 23L, lastRefreshBucket(23 * 3_600_000L))
        assertEquals(LastRefreshBucket.Days to 1L, lastRefreshBucket(24 * 3_600_000L))
    }

    @Test
    fun `days keep counting past a week`() {
        assertEquals(LastRefreshBucket.Days to 7L, lastRefreshBucket(7L * 24 * 3_600_000L))
        assertEquals(LastRefreshBucket.Days to 400L, lastRefreshBucket(400L * 24 * 3_600_000L))
    }

    /**
     * 负数（设备时间被往回拨过）归到「刚刚」，不能渲染成负数分钟。
     *
     * 上游 [RefreshLayoutState.millisSinceLastRefresh] 已经 `coerceAtLeast(0)`，
     * 这里再兜一层是因为分桶本身也可能被别处直接调用。
     */
    @Test
    fun `a negative elapsed time never falls through`() {
        assertEquals(LastRefreshBucket.JustNow to 0L, lastRefreshBucket(-5_000L))
    }
}