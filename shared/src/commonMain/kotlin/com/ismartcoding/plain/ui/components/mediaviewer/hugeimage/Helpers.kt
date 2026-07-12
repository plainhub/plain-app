package com.ismartcoding.plain.ui.components.mediaviewer.hugeimage

import kotlin.math.absoluteValue

fun checkRectInBound(
    stX1: Float, stY1: Float, edX1: Float, edY1: Float,
    stX2: Float, stY2: Float, edX2: Float, edY2: Float,
): Boolean {
    if (edY1 < stY2) return false
    if (stY1 > edY2) return false
    if (edX1 < stX2) return false
    if (stX1 > edX2) return false
    return true
}

fun getRectDelta(delta: Float, rSize: Float, bSize: Float, offset: Float): Float {
    return delta + if (delta < 0) {
        val direction = if (rSize > bSize) -1 else 1
        (offset + (direction) * (bSize - rSize)
            .div(2).absoluteValue).absoluteValue
    } else 0F
}

fun calcLeftSize(bSize: Float, rSize: Float, offset: Float): Float {
    return if (offset.absoluteValue > (bSize - rSize).div(2).absoluteValue) {
        rSize - (offset.absoluteValue - (bSize - rSize).div(2))
    } else {
        rSize.coerceAtMost(bSize)
    }
}

fun calculateInSampleSize(
    srcWidth: Int,
    reqWidth: Int,
): Int {
    var inSampleSize = 1
    while (true) {
        val iss = inSampleSize * 2
        if (srcWidth.toFloat().div(iss) < reqWidth){
            break
        }
        inSampleSize = iss
    }

    return inSampleSize
}