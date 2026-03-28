package com.ismartcoding.plain.ui.components.mediaviewer

import androidx.compose.animation.core.FloatExponentialDecaySpec
import androidx.compose.animation.core.generateDecayAnimationSpec
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.util.fastForEach
import com.ismartcoding.lib.logcat.LogCat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

data class MediaViewerGestureResult(
    val rawGesture: RawGesture,
    val onSizeChange: suspend (SizeChangeContent) -> Unit,
)

@Composable
internal fun rememberMediaViewerGesture(
    state: MediaViewerState,
    gesture: GestureScope,
    scope: CoroutineScope,
): MediaViewerGestureResult {
    var centroid by remember { mutableStateOf(Offset.Zero) }
    val decay = remember { FloatExponentialDecaySpec(2f).generateDecayAnimationSpec<Float>() }
    var velocityTracker = remember { VelocityTracker() }
    var eventChangeCount by remember { mutableIntStateOf(0) }
    var lastPan by remember { mutableStateOf(Offset.Zero) }
    var boundX by remember { mutableFloatStateOf(0F) }
    var boundY by remember { mutableFloatStateOf(0F) }
    var maxScale by remember { mutableFloatStateOf(1F) }
    val maxDisplayScale by remember { derivedStateOf { maxScale * MAX_SCALE_RATE } }
    var desX by remember { mutableFloatStateOf(0F) }
    var desY by remember { mutableFloatStateOf(0F) }
    var desScale by remember { mutableFloatStateOf(1F) }
    var fromScale by remember { mutableFloatStateOf(1F) }
    var boundScale by remember { mutableFloatStateOf(1F) }
    var desRotation by remember { mutableFloatStateOf(0F) }
    var rotate by remember { mutableFloatStateOf(0F) }
    var zoom by remember { mutableFloatStateOf(1F) }
    var fingerDistanceOffset by remember { mutableStateOf(Offset.Zero) }

    fun asyncDesParams() {
        desX = state.offsetX.value; desY = state.offsetY.value
        desScale = state.scale.value; desRotation = state.rotation.value
    }
    LaunchedEffect(key1 = state.resetTimeStamp) { asyncDesParams() }

    val sizeChange: suspend (SizeChangeContent) -> Unit = { content ->
        maxScale = content.maxScale; state.defaultSize = content.defaultSize
        state.containerSize = content.containerSize; state.maxScale = content.maxScale
        if (state.fromSaver) { state.fromSaver = false; state.fixToBound() }
    }

    val rawGesture = remember {
        RawGesture(
            onTap = gesture.onTap, onDoubleTap = { gesture.onDoubleTap(it) }, onLongPress = gesture.onLongPress,
            gestureStart = {
                if (state.allowGestureInput) {
                    eventChangeCount = 0; velocityTracker = VelocityTracker()
                    scope.launch { state.offsetX.stop(); state.offsetY.stop(); state.offsetX.updateBounds(null, null); state.offsetY.updateBounds(null, null) }
                    asyncDesParams()
                }
            },
            gestureEnd = { transformOnly ->
                if (transformOnly && !state.isRunning() && state.allowGestureInput) {
                    var velocity = try { velocityTracker.calculateVelocity() } catch (e: Exception) { LogCat.e(e.toString()); null }
                    val scale = when {
                        state.scale.value < 1 -> 1F
                        state.scale.value > maxDisplayScale -> { velocity = null; maxDisplayScale }
                        else -> null
                    }
                    scope.launch {
                        if (inBound(state.offsetX.value, boundX) && velocity != null) {
                            state.offsetX.updateBounds(-boundX, boundX); state.offsetX.animateDecay(sameDirection(lastPan.x, velocity.x), decay)
                        } else {
                            val tx = if (scale != maxDisplayScale) limitToBound(state.offsetX.value, boundX)
                            else panTransformAndScale(state.offsetX.value, centroid.x, state.containerSize.width.toFloat(), state.defaultSize.width.toFloat(), state.scale.value, scale!!)
                            state.offsetX.animateTo(tx)
                        }
                    }
                    scope.launch {
                        if (inBound(state.offsetY.value, boundY) && velocity != null) {
                            state.offsetY.updateBounds(-boundY, boundY); state.offsetY.animateDecay(sameDirection(lastPan.y, velocity.y), decay)
                        } else {
                            val ty = if (scale != maxDisplayScale) limitToBound(state.offsetY.value, boundY)
                            else panTransformAndScale(state.offsetY.value, centroid.y, state.containerSize.height.toFloat(), state.defaultSize.height.toFloat(), state.scale.value, scale!!)
                            state.offsetY.animateTo(ty)
                        }
                    }
                    scale?.let { scope.launch { state.scale.animateTo(it) } }
                }
            },
        ) { center, pan, _zoom, _rotate, event ->
            if (!state.allowGestureInput) return@RawGesture true
            if (event.changes.size > eventChangeCount) eventChangeCount = event.changes.size
            if (eventChangeCount > event.changes.size) return@RawGesture false
            rotate = _rotate; zoom = _zoom
            if (event.changes.size == 2) {
                fingerDistanceOffset = event.changes[0].position - event.changes[1].position
                if (fingerDistanceOffset.x.absoluteValue < MIN_GESTURE_FINGER_DISTANCE && fingerDistanceOffset.y.absoluteValue < MIN_GESTURE_FINGER_DISTANCE) { rotate = 0F; zoom = 1F }
            }
            lastPan = pan; centroid = center; fromScale = desScale; desScale *= zoom
            if (desScale < MIN_SCALE) desScale = MIN_SCALE
            boundScale = if (desScale > maxDisplayScale) maxDisplayScale else desScale
            boundX = getBound(boundScale * state.defaultSize.width, state.containerSize.width.toFloat())
            boundY = getBound(boundScale * state.defaultSize.height, state.containerSize.height.toFloat())
            desX = panTransformAndScale(desX, center.x, state.containerSize.width.toFloat(), state.defaultSize.width.toFloat(), fromScale, desScale) + pan.x
            if (eventChangeCount == 1) desX = limitToBound(desX, boundX)
            desY = panTransformAndScale(desY, center.y, state.containerSize.height.toFloat(), state.defaultSize.height.toFloat(), fromScale, desScale) + pan.y
            if (eventChangeCount == 1) desY = limitToBound(desY, boundY)
            if (desScale < 1) desRotation += rotate
            velocityTracker.addPosition(event.changes[0].uptimeMillis, Offset(desX, desY))
            if (!state.isRunning()) scope.launch { state.scale.snapTo(desScale); state.offsetX.snapTo(desX); state.offsetY.snapTo(desY) }
            val onLeft = desX >= boundX; val onRight = desX <= -boundX
            val reachSide = !(onLeft && pan.x > 0) && !(onRight && pan.x < 0) && !(onLeft && onRight)
            if (reachSide || state.scale.value < 1) { event.changes.fastForEach { if (it.positionChanged()) it.consume() } }
            return@RawGesture true
        }
    }
    return MediaViewerGestureResult(rawGesture, sizeChange)
}
