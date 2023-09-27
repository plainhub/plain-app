package com.ismartcoding.plain.ui.theme.palette.colorspace.oklab

import com.ismartcoding.plain.ui.theme.palette.colorspace.rgb.Rgb
import com.ismartcoding.plain.ui.theme.palette.colorspace.rgb.Rgb.Companion.toRgb
import com.ismartcoding.plain.ui.theme.palette.colorspace.rgb.RgbColorSpace
import com.ismartcoding.plain.ui.theme.palette.util.square
import com.ismartcoding.plain.ui.theme.palette.util.toDegrees
import com.ismartcoding.plain.ui.theme.palette.util.toRadians
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class Oklch(
    val L: Double,
    val C: Double,
    val h: Double,
) {
    fun toOklab(): Oklab {
        val hRad = h.toRadians()
        return Oklab(
            L = L,
            a = C * cos(hRad),
            b = C * sin(hRad),
        )
    }

    fun clampToRgb(colorSpace: RgbColorSpace): Rgb =
        toOklab().toXyz().toRgb(1.0, colorSpace).takeIf { it.isInGamut() } ?: copy(
            C =
                findChromaBoundaryInRgb(
                    colorSpace,
                    0.001,
                ),
        ).toOklab().toXyz().toRgb(1.0, colorSpace).clamp()

    private fun findChromaBoundaryInRgb(
        colorSpace: RgbColorSpace,
        error: Double,
    ): Double =
        chromaBoundary.getOrPut(Triple(colorSpace.hashCode(), h, L)) {
            var low = 0.0
            var high = C
            var current = this
            while (high - low >= error) {
                val mid = (low + high) / 2.0
                current = copy(C = mid)
                if (!current.toOklab().toXyz().toRgb(1.0, colorSpace).isInGamut()) {
                    high = mid
                } else {
                    val next = current.copy(C = mid + error).toOklab().toXyz().toRgb(1.0, colorSpace)
                    if (next.isInGamut()) {
                        low = mid
                    } else {
                        break
                    }
                }
            }
            current.C
        }

    companion object {
        fun Oklab.toOklch(): Oklch =
            Oklch(
                L = L,
                C = sqrt(square(a) + square(b)),
                h = atan2(b, a).toDegrees().mod(360.0),
            )

        private val chromaBoundary: MutableMap<Triple<Int, Double, Double>, Double> = mutableMapOf()
    }
}
