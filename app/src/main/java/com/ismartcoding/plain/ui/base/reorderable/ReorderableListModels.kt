package com.ismartcoding.plain.ui.base.reorderable

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring

internal data class ItemInterval(val start: Float = 0f, val size: Int = 0) {
    val center: Float get() = start + size / 2
    val end: Float get() = start + size
}

internal inline fun <T> List<T>.firstIndexOfIndexed(predicate: (Int, T) -> Boolean): Int? {
    forEachIndexed { i, e -> if (predicate(i, e)) return i }
    return null
}

internal inline fun <T> List<T>.lastIndexOfIndexed(predicate: (Int, T) -> Boolean): Int? {
    for (i in lastIndex downTo 0) { if (predicate(i, this[i])) return i }
    return null
}

internal val reorderableAnimationSpec = spring<Float>(stiffness = Spring.StiffnessMediumLow)
