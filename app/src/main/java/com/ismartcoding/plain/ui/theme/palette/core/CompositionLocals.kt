package com.ismartcoding.plain.ui.theme.palette.core

import androidx.compose.runtime.staticCompositionLocalOf
import com.ismartcoding.plain.ui.theme.palette.colorspace.cielab.CieLab
import com.ismartcoding.plain.ui.theme.palette.colorspace.ciexyz.CieXyz
import com.ismartcoding.plain.ui.theme.palette.colorspace.rgb.RgbColorSpace
import com.ismartcoding.plain.ui.theme.palette.colorspace.zcam.Zcam
import com.ismartcoding.plain.ui.theme.palette.data.Illuminant

val LocalWhitePoint = staticCompositionLocalOf {
    Illuminant.D65
}

val LocalLuminance = staticCompositionLocalOf {
    1.0
}

val LocalRgbColorSpace = staticCompositionLocalOf {
    RgbColorSpace.Srgb
}

val LocalZcamViewingConditions = staticCompositionLocalOf {
    createZcamViewingConditions()
}

fun createZcamViewingConditions(
    whitePoint: CieXyz = Illuminant.D65,
    luminance: Double = 203.0,
    surroundFactor: Double = 0.69,
): Zcam.Companion.ViewingConditions = Zcam.Companion.ViewingConditions(
    whitePoint = whitePoint,
    luminance = luminance,
    F_s = surroundFactor,
    L_a = 0.4 * luminance,
    Y_b = CieLab(50.0, 0.0, 0.0).toXyz(whitePoint, luminance).luminance,
)
