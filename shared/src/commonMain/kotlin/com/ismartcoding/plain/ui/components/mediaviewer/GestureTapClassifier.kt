package com.ismartcoding.plain.ui.components.mediaviewer

/**
 * Window in which a second qualifying release is treated as a double tap.
 * A single tap is fired after this delay so a quick second tap can cancel it.
 */
internal const val DOUBLE_TAP_WINDOW_MILLIS = 272L

/**
 * Pure tap vs double-tap classifier for [detectTransformGestures].
 *
 * A release qualifies as a tap when the gesture stayed single-pointer, never
 * crossed touch slop (digitizer jitter below slop is not movement), was not
 * canceled (e.g. a sibling long-press detector consumed it) and the press was
 * shorter than the long-press timeout. All times are caller-supplied
 * monotonic event uptimes, so the classification is deterministic.
 */
internal class GestureTapClassifier(
    private val doubleTapWindowMillis: Long = DOUBLE_TAP_WINDOW_MILLIS,
) {
    enum class Decision { IGNORED, TAP, DOUBLE_TAP }

    private var lastReleaseUptimeMillis = 0L

    fun classifyRelease(
        upUptimeMillis: Long,
        pressDurationMillis: Long,
        longPressTimeoutMillis: Long,
        maxPointerCount: Int,
        passedTouchSlop: Boolean,
        canceled: Boolean,
    ): Decision {
        if (canceled || passedTouchSlop || maxPointerCount > 1) return Decision.IGNORED
        if (pressDurationMillis >= longPressTimeoutMillis) return Decision.IGNORED
        val isSecondTap = lastReleaseUptimeMillis != 0L &&
            upUptimeMillis - lastReleaseUptimeMillis < doubleTapWindowMillis
        lastReleaseUptimeMillis = if (isSecondTap) 0L else upUptimeMillis
        return if (isSecondTap) Decision.DOUBLE_TAP else Decision.TAP
    }
}
