package com.ismartcoding.plain.ui.base.pullrefresh

import androidx.compose.runtime.MonotonicFrameClock

/**
 * 测试用帧时钟：每调一次 `withFrameNanos` 就往前推一帧。
 *
 * **时间必须推进**，这一点是踩过坑的：Compose 的 `AnimationScope.animate()` 靠
 * `withFrameNanos` 的返回值累加 `playTime`，拿 `isFinishedFromNanos` 决定何时结束。
 * 帧时钟如果永远返回同一个常量，`playTime` 就永远是 0，动画**永不结束**。
 *
 * 这个失败模式非常难认：它是**空转**而不是阻塞——不吃锁、不 sleep，Gradle 那边
 * 只表现为 `> Task :plain-ui:testAndroidHostTest` 一直不出结果，进程却占满一个核。
 * 2026-10-06 本模块的测试连挂 3 次（每次 900s 超时），最后 `jstack` 出来是
 * `ImmediateFrameClock.withFrameNanos` 满 CPU 自旋，栈底就是
 * `finishFromRefresh -> Animatable.animateTo(0f)`。
 *
 * 用常量时钟当"一帧就走完"的直觉是错的：它不是让动画变快，而是让动画**不结束**。
 *
 * `kotlinx-coroutines-test` 1.11 没有 `TestMonotonicFrameClock`（common 侧也没有，
 * iOS 目标编不到），所以这里自己实现。只在 androidHostTest 用，不进 commonMain。
 */
internal class AdvancingFrameClock(
    private val frameNanos: Long = 16_000_000L, // 60fps 一帧
) : MonotonicFrameClock {
    private var now = 0L

    override suspend fun <R> withFrameNanos(onFrame: (Long) -> R): R {
        now += frameNanos
        return onFrame(now)
    }
}