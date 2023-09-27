package com.ismartcoding.plain.ui.theme.palette.colorspace.cielab

import com.ismartcoding.plain.ui.theme.palette.util.square
import com.ismartcoding.plain.ui.theme.palette.util.toDegrees
import com.ismartcoding.plain.ui.theme.palette.util.toRadians
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class CieLch(
    val L: Double,
    val C: Double,
    val h: Double,
) {
    fun toCieLab(): CieLab {
        val hRad = h.toRadians()
        return CieLab(
            L = L,
            a = C * cos(hRad),
            b = C * sin(hRad),
        )
    }

    companion object {
        fun CieLab.toCieLch(): CieLch =
            CieLch(
                L = L,
                C = sqrt(square(a) + square(b)),
                h = atan2(b, a).toDegrees().mod(360.0),
            )
    }
}
