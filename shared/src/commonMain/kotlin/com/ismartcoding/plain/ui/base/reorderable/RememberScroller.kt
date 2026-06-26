package com.ismartcoding.plain.ui.base.reorderable

import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState

@Composable
fun rememberScroller(
    scrollableState: ScrollableState,
    pixelAmount: Float,
    duration: Long = 100,
): Scroller {
    return rememberScroller(scrollableState, { pixelAmount }, duration)
}

/**
 * A utility to programmatically scroll a [ScrollableState].
 *
 * @param scrollableState The [ScrollableState] to scroll.
 * @param pixelPerSecond The amount of pixels to scroll per second.
 */
@Composable
fun rememberScroller(
    scrollableState: ScrollableState,
    pixelPerSecond: Float,
): Scroller {
    val scope = rememberCoroutineScope()
    val pixelPerSecondUpdated = rememberUpdatedState(pixelPerSecond)

    return remember(scrollableState, scope) {
        Scroller(
            scrollableState,
            scope,
            pixelPerSecondProvider = { pixelPerSecondUpdated.value },
        )
    }
}

/**
 * A utility to programmatically scroll a [ScrollableState].
 *
 * @param scrollableState The [ScrollableState] to scroll.
 * @param pixelAmountProvider A function that returns the amount of pixels to scroll per duration.
 * @param duration The duration of each scroll in milliseconds.
 */
@Composable
fun rememberScroller(
    scrollableState: ScrollableState,
    pixelAmountProvider: () -> Float,
    duration: Long = 100,
): Scroller {
    val scope = rememberCoroutineScope()
    val pixelAmountProviderUpdated = rememberUpdatedState(pixelAmountProvider)
    val durationUpdated = rememberUpdatedState(duration)

    return remember(scrollableState, scope, duration) {
        Scroller(
            scrollableState,
            scope,
            pixelPerSecondProvider = {
                pixelAmountProviderUpdated.value() / (durationUpdated.value / 1000f)
            },
        )
    }
}
