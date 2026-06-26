package com.ismartcoding.plain.ui.base.reorderable

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.runtime.Stable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Stable
class Scroller internal constructor(
    private val scrollableState: ScrollableState,
    private val scope: CoroutineScope,
    private val pixelPerSecondProvider: () -> Float,
) {
    companion object {
        // The maximum duration for a scroll animation in milliseconds.
        private const val MaxScrollDuration = 100L
        private const val ZeroScrollWaitDuration = 100L
    }

    internal enum class Direction {
        BACKWARD, FORWARD;

        val opposite: Direction
            get() = when (this) {
                BACKWARD -> FORWARD
                FORWARD -> BACKWARD
            }
    }

    private data class ScrollInfo(
        val direction: Direction,
        val speedMultiplier: Float,
        val maxScrollDistanceProvider: () -> Float,
        val onScroll: suspend () -> Unit,
    ) {
        companion object {
            val Null = ScrollInfo(Direction.FORWARD, 0f, { 0f }, {})
        }
    }

    private var programmaticScrollJob: Job? = null
    val isScrolling: Boolean
        get() = programmaticScrollJob?.isActive == true

    private val scrollInfoChannel = Channel<ScrollInfo>(Channel.CONFLATED)

    internal fun start(
        direction: Direction,
        speedMultiplier: Float = 1f,
        maxScrollDistanceProvider: () -> Float = { Float.MAX_VALUE },
        onScroll: suspend () -> Unit = {},
    ): Boolean {
        if (!canScroll(direction)) return false

        if (programmaticScrollJob == null) {
            programmaticScrollJob = scope.launch {
                scrollLoop()
            }
        }

        val scrollInfo =
            ScrollInfo(direction, speedMultiplier, maxScrollDistanceProvider, onScroll)

        scrollInfoChannel.trySend(scrollInfo)
        return true
    }

    private suspend fun scrollLoop() {
        var scrollInfo: ScrollInfo? = null

        while (true) {
            scrollInfo = scrollInfoChannel.tryReceive().getOrNull() ?: scrollInfo
            if (scrollInfo == null || scrollInfo == ScrollInfo.Null) break

            val (direction, speedMultiplier, maxScrollDistanceProvider, onScroll) = scrollInfo

            val pixelPerSecond = pixelPerSecondProvider() * speedMultiplier
            val pixelPerMs = pixelPerSecond / 1000f

            onScroll()

            if (!canScroll(direction)) break

            val maxScrollDistance = maxScrollDistanceProvider()
            if (maxScrollDistance <= 0f) {
                delay(ZeroScrollWaitDuration)
                continue
            }
            val maxScrollDistanceDuration = maxScrollDistance / pixelPerMs
            val duration =
                maxScrollDistanceDuration.toLong().coerceIn(1L, MaxScrollDuration)
            val scrollDistance =
                maxScrollDistance * (duration / maxScrollDistanceDuration)
            val diff = scrollDistance.let {
                when (direction) {
                    Direction.BACKWARD -> -it
                    Direction.FORWARD -> it
                }
            }

            scrollableState.animateScrollBy(
                diff, tween(durationMillis = duration.toInt(), easing = LinearEasing)
            )
        }
    }

    private fun canScroll(direction: Direction): Boolean {
        return when (direction) {
            Direction.BACKWARD -> scrollableState.canScrollBackward
            Direction.FORWARD -> scrollableState.canScrollForward
        }
    }

    internal suspend fun stop() {
        scrollInfoChannel.send(ScrollInfo.Null)
        programmaticScrollJob?.cancelAndJoin()
        programmaticScrollJob = null
    }

    internal fun tryStop() {
        scope.launch {
            stop()
        }
    }
}
