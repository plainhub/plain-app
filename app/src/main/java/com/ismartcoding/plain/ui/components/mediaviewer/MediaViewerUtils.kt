package com.ismartcoding.plain.ui.components.mediaviewer

import kotlin.math.absoluteValue

fun sameDirection(a: Float, b: Float): Float {
    return if (a > 0) {
        if (b < 0) b.absoluteValue else b
    } else {
        if (b > 0) -b else b
    }
}

fun getBound(rw: Float, bw: Float): Float {
    return if (rw > bw) {
        var xb = (rw - bw).div(2)
        if (xb < 0) xb = 0F
        xb
    } else {
        0F
    }
}

fun inBound(offset: Float, bound: Float): Boolean {
    return if (offset > 0) offset < bound
    else if (offset < 0) offset > -bound
    else true
}

fun limitToBound(offset: Float, bound: Float): Float {
    return when {
        offset > bound -> bound
        offset < -bound -> -bound
        else -> offset
    }
}

fun panTransformAndScale(
    offset: Float,
    center: Float,
    bh: Float,
    uh: Float,
    fromScale: Float,
    toScale: Float,
): Float {
    val srcH = uh * fromScale
    val desH = uh * toScale
    val gapH = (bh - uh) / 2

    val py = when {
        uh >= bh -> {
            val upy = (uh * fromScale - uh).div(2)
            (upy - offset + center) / (fromScale * uh)
        }
        srcH > bh || bh > uh -> {
            val upy = (srcH - uh).div(2)
            (upy - gapH - offset + center) / (fromScale * uh)
        }
        else -> {
            val upy = -(bh - srcH).div(2)
            (upy - offset + center) / (fromScale * uh)
        }
    }
    return when {
        uh >= bh -> {
            val upy = (uh * toScale - uh).div(2)
            upy + center - py * toScale * uh
        }
        desH > bh -> {
            val upy = (desH - uh).div(2)
            upy - gapH + center - py * toScale * uh
        }
        else -> {
            val upy = -(bh - desH).div(2)
            upy + center - py * desH
        }
    }
}
