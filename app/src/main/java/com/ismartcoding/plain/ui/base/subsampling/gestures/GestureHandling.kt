package com.ismartcoding.plain.ui.base.subsampling.gestures

import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitTouchSlopOrCancellation
import androidx.compose.foundation.gestures.forEachGesture
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventTimeoutCancellationException
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.changedToDown
import androidx.compose.ui.input.pointer.changedToDownIgnoreConsumed
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.util.fastAll
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastFirstOrNull
import androidx.compose.ui.util.fastForEach
import com.ismartcoding.lib.logcat.LogCat
import com.ismartcoding.plain.ui.base.subsampling.ComposeSubsamplingScaleImageState
import com.ismartcoding.plain.ui.base.subsampling.ScrollableContainerDirection
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue


internal suspend fun PointerInputScope.processGestures(
    state: ComposeSubsamplingScaleImageState,
    onTap: ((Offset) -> Unit)?,
    onLongTap: ((Offset) -> Unit)?,
    zoomGestureDetector: ZoomGestureDetector?,
    panGestureDetector: PanGestureDetector?,
    multiTouchGestureDetector: MultiTouchGestureDetector?
) {
    val quickZoomTimeoutMs = state.quickZoomTimeoutMs
    val debug = state.debug
    val activeDetectorJobs = arrayOfNulls<Job?>(DetectorType.values().size)
    var detectAllUpJob: Job? = null
    var hasAnyPointersDown = false

    fun stopDetectAllUpJob() {
        detectAllUpJob?.cancel()
        detectAllUpJob = null
    }

    val allDetectors = arrayOf(zoomGestureDetector, panGestureDetector, multiTouchGestureDetector)
        .filterNotNull()

    forEachGesture {
        try {
            if (debug) {
                LogCat.d("forEachGesture() start")
            }

            activeDetectorJobs.forEachIndexed { index, job ->
                job?.cancel()
                activeDetectorJobs[index] = null
            }

            stopDetectAllUpJob()

            coroutineScope {
                coroutineContext[Job]!!.invokeOnCompletion { cause ->
                    if (cause == null) {
                        return@invokeOnCompletion
                    }

                    allDetectors.fastForEach { detector -> detector.cancelAnimation() }
                }

                if (onTap != null || onLongTap != null) {
                    activeDetectorJobs[DetectorType.Tap.index] = launch {
                        try {
                            if (debug) {
                                LogCat.d("detectTapOrLongTapGestures() start")
                            }

                            detectTapOrLongTapGestures(
                                debug = state.debug,
                                coroutineScope = this,
                                onTap = onTap,
                                onLongTap = onLongTap,
                                detectorType = DetectorType.Tap,
                                cancelAnimations = { allDetectors.fastForEach { detector -> detector.cancelAnimation() } },
                                stopOtherDetectors = { detectorType -> stopOtherDetectors(activeDetectorJobs, detectorType) },
                                gesturesLocked = { allDetectors.fastAny { detector -> detector.animating } }
                            )
                        } finally {
                            activeDetectorJobs[DetectorType.Tap.index]?.cancel()
                            activeDetectorJobs[DetectorType.Tap.index] = null

                            if (checkAndCancelWholeScope(debug, activeDetectorJobs, hasAnyPointersDown)) {
                                if (debug) {
                                    LogCat.d("detectTapOrLongTapGestures() calling stopOtherDetectors()")
                                }

                                stopOtherDetectors(activeDetectorJobs)
                                stopDetectAllUpJob()
                            }

                            if (debug) {
                                LogCat.d("detectTapOrLongTapGestures() end")
                            }
                        }
                    }
                }

                activeDetectorJobs[DetectorType.Zoom.index] = launch {
                    try {
                        if (debug) {
                            LogCat.d("detectZoomGestures() start")
                        }

                        detectZoomGestures(
                            quickZoomTimeoutMs = quickZoomTimeoutMs,
                            zoomGestureDetector = zoomGestureDetector,
                            coroutineScope = this,
                            detectorType = DetectorType.Zoom,
                            cancelAnimations = { allDetectors.fastForEach { detector -> detector.cancelAnimation() } },
                            stopOtherDetectors = { detectorType -> stopOtherDetectors(activeDetectorJobs, detectorType) },
                            gesturesLocked = { allDetectors.fastAny { detector -> detector.animating } }
                        )
                    } finally {
                        activeDetectorJobs[DetectorType.Zoom.index]?.cancel()
                        activeDetectorJobs[DetectorType.Zoom.index] = null

                        if (checkAndCancelWholeScope(debug, activeDetectorJobs, hasAnyPointersDown)) {
                            if (debug) {
                                LogCat.d("detectZoomGestures() calling stopOtherDetectors()")
                            }

                            stopOtherDetectors(activeDetectorJobs)
                            stopDetectAllUpJob()
                        }

                        if (debug) {
                            LogCat.d("detectZoomGestures() end")
                        }
                    }
                }

                activeDetectorJobs[DetectorType.Pan.index] = launch {
                    try {
                        if (debug) {
                            LogCat.d("detectPanGestures() start")
                        }

                        detectPanGestures(
                            state = state,
                            panGestureDetector = panGestureDetector,
                            coroutineScope = this,
                            detectorType = DetectorType.Pan,
                            cancelAnimations = { allDetectors.fastForEach { detector -> detector.cancelAnimation() } },
                            stopOtherDetectors = { detectorType -> stopOtherDetectors(activeDetectorJobs, detectorType) },
                            gesturesLocked = { allDetectors.fastAny { detector -> detector.animating } }
                        )
                    } finally {
                        activeDetectorJobs[DetectorType.Pan.index]?.cancel()
                        activeDetectorJobs[DetectorType.Pan.index] = null

                        if (checkAndCancelWholeScope(debug, activeDetectorJobs, hasAnyPointersDown)) {
                            if (debug) {
                                LogCat.d("detectPanGestures() calling stopOtherDetectors()")
                            }

                            stopOtherDetectors(activeDetectorJobs)
                            stopDetectAllUpJob()
                        }

                        if (debug) {
                            LogCat.d("detectPanGestures() end")
                        }
                    }
                }

                activeDetectorJobs[DetectorType.MultiTouch.index] = launch {
                    try {
                        if (debug) {
                            LogCat.d("detectMultiTouchGestures() start")
                        }

                        detectMultiTouchGestures(
                            multiTouchGestureDetector = multiTouchGestureDetector,
                            coroutineScope = this,
                            detectorType = DetectorType.MultiTouch,
                            cancelAnimations = { allDetectors.fastForEach { detector -> detector.cancelAnimation() } },
                            stopOtherDetectors = { detectorType -> stopOtherDetectors(activeDetectorJobs, detectorType) },
                            gesturesLocked = { allDetectors.fastAny { detector -> detector.animating } }
                        )
                    } finally {
                        activeDetectorJobs[DetectorType.MultiTouch.index]?.cancel()
                        activeDetectorJobs[DetectorType.MultiTouch.index] = null

                        if (checkAndCancelWholeScope(debug, activeDetectorJobs, hasAnyPointersDown)) {
                            if (debug) {
                                LogCat.d("detectMultiTouchGestures() calling stopOtherDetectors()")
                            }

                            stopOtherDetectors(activeDetectorJobs)
                            stopDetectAllUpJob()
                        }

                        if (debug) {
                            LogCat.d("detectMultiTouchGestures() end")
                        }
                    }
                }

                detectAllUpJob = launch {
                    if (debug) {
                        LogCat.d("detectAllUp() start")
                    }

                    try {
                        while (isActive) {
                            awaitPointerEventScope { awaitFirstDown(requireUnconsumed = false) }
                            hasAnyPointersDown = true
                            awaitPointerEventScope { awaitAllPointersUp() }
                            hasAnyPointersDown = false

                            if (checkAndCancelWholeScope(debug, activeDetectorJobs, false)) {
                                break
                            }
                        }
                    } finally {
                        hasAnyPointersDown = false

                        stopOtherDetectors(activeDetectorJobs)

                        if (debug) {
                            LogCat.d("detectAllUp() end")
                        }
                    }
                }
            }
        } finally {
            if (debug) {
                LogCat.d("forEachGesture() end")
            }
        }
    }
}

/**
 * This function is needed to cancel the infinite loop inside of the MultiTouch gesture
 * detector. It checks whether all gesture detectors are done normally, or there is only
 * multitouch detector left and there are no pointers touching the screen. If any of those
 * is true it cancels everything so that we are ready to handle next gestures.
 * */
private fun checkAndCancelWholeScope(
    debug: Boolean,
    activeDetectorJobs: Array<Job?>,
    hasAnyPointersDown: Boolean
): Boolean {
    val activeDetectors = activeDetectorJobs.mapIndexed { index, job ->
        if (job != null) {
            return@mapIndexed DetectorType.from(index)
        }

        return@mapIndexed null
    }.filterNotNull()

    if (activeDetectors.isEmpty()) {
        if (debug) {
            LogCat.d("checkAndCancelWholeScope() no activeDetectors")
        }

        return true
    }

    val isMultiTouchActive = activeDetectors
        .firstOrNull { detectorType -> detectorType == DetectorType.MultiTouch } != null

    if (debug) {
        LogCat.d(
            "checkAndCancelWholeScope() activeDetectors=$activeDetectors, " +
                    "isMultiTouchActive=$isMultiTouchActive, " +
                    "hasAnyPointersDown=$hasAnyPointersDown"
        )
    }

    if (isMultiTouchActive && activeDetectors.size == 1 && !hasAnyPointersDown) {
        return true
    }

    return false
}

private fun stopOtherDetectors(activeDetectorJobs: Array<Job?>, exclude: DetectorType? = null) {
    for (index in activeDetectorJobs.indices) {
        if (exclude != null && index == exclude.index) {
            continue
        }

        activeDetectorJobs[index]?.cancel()
        activeDetectorJobs[index] = null
    }
}

private suspend fun PointerInputScope.detectTapOrLongTapGestures(
    debug: Boolean,
    coroutineScope: CoroutineScope,
    onTap: ((Offset) -> Unit)?,
    onLongTap: ((Offset) -> Unit)?,
    detectorType: DetectorType,
    cancelAnimations: () -> Unit,
    stopOtherDetectors: (DetectorType) -> Unit,
    gesturesLocked: () -> Boolean
) {
    awaitPointerEventScope {
        val firstDown = awaitFirstDown()

        cancelAnimations()

        if (gesturesLocked()) {
            if (debug) {
                LogCat.d("tap() Gestures locked detectorType=${detectorType}")
            }

            consumeChangesUntilAllPointersAreUp(
                pointerInputChange = firstDown,
                coroutineScope = coroutineScope,
                gesturesLocked = gesturesLocked
            )

            return@awaitPointerEventScope
        }

        if (debug) {
            LogCat.d("tap() Gestures NOT locked, detectorType=${detectorType}")
        }

        val longPressTimeout = onLongTap?.let {
            viewConfiguration.longPressTimeoutMillis
        } ?: (Long.MAX_VALUE / 2)

        var upOrCancel: PointerInputChange? = null
        try {
            upOrCancel = withTimeout(longPressTimeout) { waitForUpOrCancellation() }

            if (upOrCancel != null) {
                val touchSlop = viewConfiguration.touchSlop
                val distance = (upOrCancel.position - firstDown.position).getDistance().absoluteValue

                if (distance > touchSlop) {
                    if (debug) {
                        LogCat.d("tap() distance ($distance) exceeds touchSlop ($touchSlop), detectorType=${detectorType}")
                    }
                    // The distance between the touch start and end exceeds touchSlop so this gesture is neither
                    // the tap nor long tap.
                    return@awaitPointerEventScope
                }
            }

            upOrCancel?.consume()
        } catch (_: PointerEventTimeoutCancellationException) {
            stopOtherDetectors(detectorType)
            onLongTap?.invoke(firstDown.position)
            consumeUntilUp()
            return@awaitPointerEventScope
        }

        if (upOrCancel != null) {
            // Wait 10 additional milliseconds so that double-tap gesture detector has enough time
            // to cancel this detector in case this gesture is any kind of double-tap gesture.
            val secondDown = awaitSecondDown(
                firstUp = upOrCancel,
                additionalWaitTime = 10L
            )
            if (secondDown == null) {
                stopOtherDetectors(detectorType)
                onTap?.invoke(upOrCancel.position)
            }
        }
    }
}

private suspend fun PointerInputScope.detectPanGestures(
    state: ComposeSubsamplingScaleImageState,
    panGestureDetector: PanGestureDetector?,
    coroutineScope: CoroutineScope,
    detectorType: DetectorType,
    cancelAnimations: () -> Unit,
    stopOtherDetectors: (DetectorType) -> Unit,
    gesturesLocked: () -> Boolean
) {
    awaitPointerEventScope {
        val firstDown = awaitFirstDownOnPass(
            pass = PointerEventPass.Initial,
            requireUnconsumed = false
        )

        if (panGestureDetector == null) {
            return@awaitPointerEventScope
        }

        cancelAnimations()

        if (gesturesLocked()) {
            if (panGestureDetector.debug) {
                LogCat.d("pan() Gestures locked detectorType=${panGestureDetector.detectorType}")
            }

            consumeChangesUntilAllPointersAreUp(
                pointerInputChange = firstDown,
                coroutineScope = coroutineScope,
                gesturesLocked = gesturesLocked
            )

            return@awaitPointerEventScope
        }

        if (panGestureDetector.debug) {
            LogCat.d("pan() Gestures NOT locked, detectorType=${detectorType}")
        }

        val panInfo = state.getPanInfo()
        if (panInfo == null) {
            if (panGestureDetector.debug) {
                LogCat.d("pan() panInfo == null, detectorType=${detectorType}")
            }
            return@awaitPointerEventScope
        }

        var skipThisGesture = false
        val scrollableContainerDirection = state.scrollableContainerDirection
        val touchSlop = viewConfiguration.touchSlop

        if (scrollableContainerDirection != null) {
            val touchSlopChange = awaitTouchSlopOrCancellation(
                pointerId = firstDown.id,
                onTouchSlopReached = { change, _ ->
                    // Detect whether we are able to scroll while inside of a Horizontally/Vertically
                    // scrollable container. Check the distance between the first down event and the
                    // last touch event after touch slop was detected. Then we either consume all the
                    // events and continue with the gesture or exit from the detector thus allowing the
                    // scrollable container to scroll.
                    when (scrollableContainerDirection) {
                        ScrollableContainerDirection.Horizontal -> {
                            val deltaX = firstDown.position.x - change.position.x
                            val deltaY = firstDown.position.y - change.position.y
                            val panInfoNew = state.getPanInfo()

                            if (panInfoNew != null) {
                                // If scrolling vertically more than horizontally then we need to check whether we
                                // are currently touching either top top or bottom side of the screen
                                // (depending on the direction). If there is still space to scroll then we need to
                                // process this gesture
                                if (deltaY.absoluteValue > deltaX.absoluteValue) {
                                    if (deltaY < 0) {
                                        skipThisGesture = false
                                        change.consume()

                                        return@awaitTouchSlopOrCancellation
                                    } else if (deltaY > 0) {
                                        skipThisGesture = false
                                        change.consume()

                                        return@awaitTouchSlopOrCancellation
                                    }

                                    // fallthrough
                                }

                                if (panInfoNew.touchesLeftAndRight()) {
                                    skipThisGesture = true
                                    return@awaitTouchSlopOrCancellation
                                } else if (deltaX < -touchSlop && panInfoNew.touchesLeft()) {
                                    skipThisGesture = true
                                    return@awaitTouchSlopOrCancellation
                                } else if (deltaX > touchSlop && panInfoNew.touchesRight()) {
                                    skipThisGesture = true
                                    return@awaitTouchSlopOrCancellation
                                }
                            }

                            change.consume()
                            return@awaitTouchSlopOrCancellation
                        }

                        ScrollableContainerDirection.Vertical -> {
                            val deltaX = firstDown.position.x - change.position.x
                            val deltaY = firstDown.position.y - change.position.y
                            val panInfoNew = state.getPanInfo()

                            if (panInfoNew != null) {
                                // Same as for ScrollableContainerDirection.Horizontal but the other axis is used
                                if (deltaX.absoluteValue > deltaY.absoluteValue) {
                                    if (deltaX < 0) {
                                        skipThisGesture = false
                                        change.consume()

                                        return@awaitTouchSlopOrCancellation
                                    } else if (deltaX > 0) {
                                        skipThisGesture = false
                                        change.consume()

                                        return@awaitTouchSlopOrCancellation
                                    }

                                    // fallthrough
                                }

                                if (panInfoNew.touchesTopAndBottom()) {
                                    skipThisGesture = true
                                    return@awaitTouchSlopOrCancellation
                                } else if (deltaY < -touchSlop && panInfoNew.touchesTop()) {
                                    skipThisGesture = true
                                    return@awaitTouchSlopOrCancellation
                                } else if (deltaY > touchSlop && panInfoNew.touchesBottom()) {
                                    skipThisGesture = true
                                    return@awaitTouchSlopOrCancellation
                                }
                            }

                            change.consume()
                            return@awaitTouchSlopOrCancellation
                        }
                    }
                }
            )

            if (skipThisGesture || touchSlopChange == null) {
                if (panGestureDetector.debug) {
                    LogCat.d(
                        "pan() awaitTouchSlopOrCancellation() failed " +
                                "(skipThisGesture=$skipThisGesture, touchSlop==null=${touchSlopChange == null}), " +
                                "detectorType=${detectorType}"
                    )
                }
                return@awaitPointerEventScope
            }
        }

        var lastPointerInputChange: PointerInputChange = firstDown
        var canceled = false

        try {
            stopOtherDetectors(detectorType)
            panGestureDetector.onGestureStarted(listOf(firstDown))

            while (coroutineScope.isActive) {
                val pointerEvent = awaitPointerEvent(pass = PointerEventPass.Main)

                val pointerInputChange = pointerEvent.changes
                    .fastFirstOrNull { it.id == firstDown.id }
                    ?: break

                if (pointerInputChange.changedToUpIgnoreConsumed()) {
                    break
                }

                if (pointerInputChange.positionChanged()) {
                    panGestureDetector.onGestureUpdated(listOf(pointerInputChange))
                }

                pointerInputChange.consume()
                lastPointerInputChange = pointerInputChange
            }
        } catch (error: Throwable) {
            canceled = error is CancellationException
            throw error
        } finally {
            panGestureDetector.onGestureEnded(
                canceled = canceled,
                pointerInputChanges = listOf(lastPointerInputChange)
            )
        }
    }
}

private suspend fun PointerInputScope.detectMultiTouchGestures(
    multiTouchGestureDetector: MultiTouchGestureDetector?,
    coroutineScope: CoroutineScope,
    detectorType: DetectorType,
    cancelAnimations: () -> Unit,
    stopOtherDetectors: (DetectorType) -> Unit,
    gesturesLocked: () -> Boolean
) {
    while (coroutineScope.isActive) {
        val initialPointerEvent = awaitPointerEventScope { awaitPointerEvent(pass = PointerEventPass.Initial) }

        if (multiTouchGestureDetector == null) {
            return
        }

        if (initialPointerEvent.changes.fastAll { it.changedToUpIgnoreConsumed() }) {
            if (multiTouchGestureDetector.debug) {
                LogCat.d("multi() initialPointerEvent.changes are all up, detectorType=${detectorType}")
            }
            return
        }

        val pointersCount = initialPointerEvent.changes.count { it.pressed }
        if (pointersCount <= 0) {
            if (multiTouchGestureDetector.debug) {
                LogCat.d("multi() pointersCount <= 0 (pointersCount=$pointersCount), detectorType=${detectorType}")
            }
            return
        }

        cancelAnimations()

        if (gesturesLocked()) {
            initialPointerEvent.changes.fastForEach { it.consume() }

            awaitPointerEventScope {
                if (multiTouchGestureDetector.debug) {
                    LogCat.d("multi() Gestures locked detectorType=${multiTouchGestureDetector.detectorType}")
                }

                consumeChangesUntilAllPointersAreUp(
                    pointerInputChange = null,
                    coroutineScope = coroutineScope,
                    gesturesLocked = gesturesLocked
                )
            }

            return
        }

        if (pointersCount < 2) {
            if (multiTouchGestureDetector.debug) {
                LogCat.d("multi() pointersCount < 2 (pointersCount=$pointersCount), detectorType=${detectorType}")
            }
            continue
        }

        if (multiTouchGestureDetector.debug) {
            LogCat.d("multi() Gestures NOT locked, detectorType=${detectorType}")
        }

        val twoMostRecentEvents = initialPointerEvent.changes
            .filter { it.pressed }
            .sortedByDescending { it.uptimeMillis }
            .take(2)

        if (twoMostRecentEvents.isEmpty()) {
            if (multiTouchGestureDetector.debug) {
                LogCat.d("multi() twoMostRecentEvents is empty, detectorType=${detectorType}")
            }
            return
        }

        if (twoMostRecentEvents.size != 2) {
            if (multiTouchGestureDetector.debug) {
                LogCat.d("multi() twoMostRecentEvents.size != 2 (size=${twoMostRecentEvents.size}), detectorType=${detectorType}")
            }
            continue
        }

        var lastPointerInputChanges = twoMostRecentEvents
        var canceled = false

        try {
            multiTouchGestureDetector.onGestureStarted(twoMostRecentEvents)

            stopOtherDetectors(detectorType)
            initialPointerEvent.changes.fastForEach { it.consume() }

            var firstPointerChange = twoMostRecentEvents[0]
            var secondPointerChange = twoMostRecentEvents[1]

            val firstEventId = firstPointerChange.id
            val secondEventId = secondPointerChange.id

            awaitPointerEventScope {
                while (coroutineScope.isActive) {
                    val pointerEvent = awaitPointerEvent(pass = PointerEventPass.Main)
                    if (pointerEvent.type != PointerEventType.Move) {
                        break
                    }

                    pointerEvent.changes.fastForEach { it.consume() }

                    firstPointerChange = pointerEvent.changes.fastFirstOrNull { it.id == firstEventId } ?: break
                    secondPointerChange = pointerEvent.changes.fastFirstOrNull { it.id == secondEventId } ?: break

                    val twoInputChanges = listOf(firstPointerChange, secondPointerChange)
                    lastPointerInputChanges = twoInputChanges

                    multiTouchGestureDetector.onGestureUpdated(twoInputChanges)
                }
            }

            return
        } catch (error: Throwable) {
            canceled = error is CancellationException
            throw error
        } finally {
            multiTouchGestureDetector.onGestureEnded(
                canceled = canceled,
                pointerInputChanges = lastPointerInputChanges
            )
        }
    }
}

private suspend fun PointerInputScope.detectZoomGestures(
    quickZoomTimeoutMs: Int,
    zoomGestureDetector: ZoomGestureDetector?,
    coroutineScope: CoroutineScope,
    detectorType: DetectorType,
    cancelAnimations: () -> Unit,
    stopOtherDetectors: (DetectorType) -> Unit,
    gesturesLocked: () -> Boolean
) {
    awaitPointerEventScope {
        val firstDown = awaitFirstDownOnPass(
            pass = PointerEventPass.Initial,
            requireUnconsumed = false
        )

        if (zoomGestureDetector == null) {
            return@awaitPointerEventScope
        }

        cancelAnimations()

        if (gesturesLocked()) {
            if (zoomGestureDetector.debug) {
                LogCat.d("zoom() Gestures locked detectorType=${zoomGestureDetector.detectorType}")
            }

            consumeChangesUntilAllPointersAreUp(
                pointerInputChange = firstDown,
                coroutineScope = coroutineScope,
                gesturesLocked = gesturesLocked
            )

            return@awaitPointerEventScope
        }

        if (zoomGestureDetector.debug) {
            LogCat.d("zoom() Gestures NOT locked, detectorType=${detectorType}")
        }

        val firstUpOrCancel = waitForUpOrCancellation()
        if (firstUpOrCancel == null) {
            if (zoomGestureDetector.debug) {
                LogCat.d("zoom() waitForUpOrCancellation() failed, detectorType=${detectorType}")
            }

            return@awaitPointerEventScope
        }

        val secondDown = awaitSecondDown(firstUpOrCancel)
        if (secondDown == null) {
            if (zoomGestureDetector.debug) {
                LogCat.d("zoom() awaitSecondDown() failed, detectorType=${detectorType}")
            }

            return@awaitPointerEventScope
        }

        val timeDelta = (secondDown.uptimeMillis - firstDown.uptimeMillis).absoluteValue
        if (timeDelta > quickZoomTimeoutMs) {
            if (zoomGestureDetector.debug) {
                LogCat.d("zoom() timeDelta failed (timeDelta=$timeDelta), detectorType=${detectorType}")
            }

            // Too much time has passed between the first and the second touch events so we can't use this
            // gesture as quick zoom anymore
            return@awaitPointerEventScope
        }

        secondDown.consume()

        var lastPointerInputChange = secondDown
        var canceled = false

        try {
            stopOtherDetectors(detectorType)
            zoomGestureDetector.onGestureStarted(listOf(secondDown))

            while (coroutineScope.isActive) {
                val pointerEvent = awaitPointerEvent(pass = PointerEventPass.Main)

                val pointerInputChange = pointerEvent.changes
                    .fastFirstOrNull { it.id == secondDown.id }
                    ?: break

                if (pointerInputChange.changedToUpIgnoreConsumed()) {
                    break
                }

                if (pointerInputChange.positionChanged()) {
                    zoomGestureDetector.onGestureUpdated(listOf(pointerInputChange))
                }

                pointerInputChange.consume()
                lastPointerInputChange = pointerInputChange
            }
        } catch (error: Throwable) {
            canceled = error is CancellationException
            throw error
        } finally {
            zoomGestureDetector.onGestureEnded(
                canceled = canceled,
                pointerInputChanges = listOf(lastPointerInputChange)
            )
        }
    }
}

private suspend fun AwaitPointerEventScope.consumeUntilUp() {
    do {
        val event = awaitPointerEvent()
        event.changes.fastForEach { it.consume() }
    } while (event.changes.fastAny { it.pressed })
}

private suspend fun AwaitPointerEventScope.consumeChangesUntilAllPointersAreUp(
    pointerInputChange: PointerInputChange?,
    coroutineScope: CoroutineScope,
    gesturesLocked: () -> Boolean
) {
    pointerInputChange?.consume()

    while (coroutineScope.isActive && gesturesLocked()) {
        val event = awaitPointerEvent(pass = PointerEventPass.Main)

        if (event.changes.fastAll { it.changedToUpIgnoreConsumed() }) {
            break
        }

        event.changes.fastForEach { it.consume() }
    }
}

private suspend fun AwaitPointerEventScope.awaitFirstDownOnPass(
    pass: PointerEventPass,
    requireUnconsumed: Boolean
): PointerInputChange {
    var event: PointerEvent
    do {
        event = awaitPointerEvent(pass)
    } while (
        !event.changes.fastAll {
            if (requireUnconsumed) it.changedToDown() else it.changedToDownIgnoreConsumed()
        }
    )

    return event.changes[0]
}

private suspend fun AwaitPointerEventScope.awaitSecondDown(
    firstUp: PointerInputChange,
    additionalWaitTime: Long = 0L
): PointerInputChange? {
    return withTimeoutOrNull(viewConfiguration.doubleTapTimeoutMillis) {
        val minUptime = firstUp.uptimeMillis + viewConfiguration.doubleTapMinTimeMillis + additionalWaitTime
        var change: PointerInputChange? = null

        // The second tap doesn't count if it happens before DoubleTapMinTime of the first tap
        do {
            val pointerEvent = awaitPointerEvent(pass = PointerEventPass.Initial)

            val ourChange = pointerEvent.changes.fastFirstOrNull { it.id != firstUp.id } ?: break
            change = ourChange
        } while (ourChange.uptimeMillis < minUptime)

        return@withTimeoutOrNull change
    }
}

private fun AwaitPointerEventScope.allPointersUp(): Boolean =
    !currentEvent.changes.fastAny { it.pressed }

private suspend fun AwaitPointerEventScope.awaitAllPointersUp() {
    if (!allPointersUp()) {
        do {
            val events = awaitPointerEvent(PointerEventPass.Final)
        } while (events.changes.fastAny { it.pressed })
    }
}