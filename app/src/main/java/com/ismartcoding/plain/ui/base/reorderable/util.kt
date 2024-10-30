package com.ismartcoding.plain.ui.base.reorderable

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.sync.Mutex

internal val Orientation.opposite
    get() = when (this) {
        Orientation.Vertical -> Orientation.Horizontal
        Orientation.Horizontal -> Orientation.Vertical
    }

internal fun Offset.getAxis(orientation: Orientation) = when (orientation) {
    Orientation.Vertical -> y
    Orientation.Horizontal -> x
}

internal fun Size.getAxis(orientation: Orientation) = when (orientation) {
    Orientation.Vertical -> height
    Orientation.Horizontal -> width
}

internal fun IntOffset.getAxis(orientation: Orientation) = when (orientation) {
    Orientation.Vertical -> y
    Orientation.Horizontal -> x
}

internal fun IntSize.getAxis(orientation: Orientation) = when (orientation) {
    Orientation.Vertical -> height
    Orientation.Horizontal -> width
}

internal fun Offset.Companion.fromAxis(orientation: Orientation, value: Float) =
    when (orientation) {
        Orientation.Vertical -> Offset(0f, value)
        Orientation.Horizontal -> Offset(value, 0f)
    }

internal fun Offset.onlyAxis(orientation: Orientation) =
    when (orientation) {
        Orientation.Vertical -> Offset(0f, y)
        Orientation.Horizontal -> Offset(x, 0f)
    }

internal fun Offset.reverseAxis(orientation: Orientation) =
    when (orientation) {
        Orientation.Vertical -> Offset(x, -y)
        Orientation.Horizontal -> Offset(-x, y)
    }

internal fun IntSize.Companion.fromAxis(orientation: Orientation, value: Float) =
    when (orientation) {
        Orientation.Vertical -> Size(0f, value)
        Orientation.Horizontal -> Size(value, 0f)
    }

internal fun IntOffset.Companion.fromAxis(orientation: Orientation, value: Int) =
    when (orientation) {
        Orientation.Vertical -> IntOffset(0, value)
        Orientation.Horizontal -> IntOffset(value, 0)
    }

internal fun IntSize.Companion.fromAxis(orientation: Orientation, value: Int) =
    when (orientation) {
        Orientation.Vertical -> IntSize(0, value)
        Orientation.Horizontal -> IntSize(value, 0)
    }

internal operator fun Offset.plus(size: Size) = Offset(x + size.width, y + size.height)
internal operator fun Offset.minus(size: Size) = Offset(x - size.width, y - size.height)

internal operator fun IntOffset.plus(size: IntSize) = IntOffset(x + size.width, y + size.height)
internal operator fun IntOffset.minus(size: IntSize) = IntOffset(x - size.width, y - size.height)


internal fun Mutex.withTryLock(block: () -> Unit): Boolean {
    return if (tryLock()) {
        block()
        unlock()
        true
    } else {
        false
    }
}
