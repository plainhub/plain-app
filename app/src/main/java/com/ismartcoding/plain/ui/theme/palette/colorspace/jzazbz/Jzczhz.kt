package com.ismartcoding.plain.ui.theme.palette.colorspace.jzazbz

import com.ismartcoding.plain.ui.theme.palette.util.square
import com.ismartcoding.plain.ui.theme.palette.util.toDegrees
import com.ismartcoding.plain.ui.theme.palette.util.toRadians
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class Jzczhz(
    val Jz: Double,
    val Cz: Double,
    val hz: Double,
) {

    fun toJzazbz(): Jzazbz {
        val hRad = hz.toRadians()
        return Jzazbz(
            Jz = Jz,
            az = Cz * cos(hRad),
            bz = Cz * sin(hRad),
        )
    }

    fun dE(other: Jzczhz): Double =
        sqrt(square(Jz - other.Jz) + square(Cz - other.Cz) + 4.0 * Cz * other.Cz * square(sin((hz - other.hz) / 2.0)))

    companion object {

        fun Jzazbz.toJzczhz(): Jzczhz = Jzczhz(
            Jz = Jz,
            Cz = sqrt(square(az) + square(bz)),
            hz = atan2(bz, az).toDegrees().mod(360.0),
        )
    }
}
