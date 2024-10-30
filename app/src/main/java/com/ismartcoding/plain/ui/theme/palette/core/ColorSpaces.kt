package com.ismartcoding.plain.ui.theme.palette.core

import androidx.compose.runtime.Composable
import com.ismartcoding.plain.ui.theme.palette.colorspace.cielab.CieLab
import com.ismartcoding.plain.ui.theme.palette.colorspace.cielab.CieLab.Companion.toCieLab
import com.ismartcoding.plain.ui.theme.palette.colorspace.ciexyz.CieXyz
import com.ismartcoding.plain.ui.theme.palette.colorspace.oklab.Oklch
import com.ismartcoding.plain.ui.theme.palette.colorspace.rgb.Rgb
import com.ismartcoding.plain.ui.theme.palette.colorspace.rgb.Rgb.Companion.toRgb
import com.ismartcoding.plain.ui.theme.palette.colorspace.zcam.Izazbz
import com.ismartcoding.plain.ui.theme.palette.colorspace.zcam.Izazbz.Companion.toIzazbz
import com.ismartcoding.plain.ui.theme.palette.colorspace.zcam.Zcam
import com.ismartcoding.plain.ui.theme.palette.colorspace.zcam.Zcam.Companion.toZcam

@Composable
fun zcamLch(
    L: Double,
    C: Double,
    h: Double,
): Zcam = Zcam(
    hz = h,
    Jz = L,
    Cz = C,
    cond = LocalZcamViewingConditions.current,
)

@Composable
fun zcam(
    hue: Double = Double.NaN,
    brightness: Double = Double.NaN,
    lightness: Double = Double.NaN,
    colorfulness: Double = Double.NaN,
    chroma: Double = Double.NaN,
    saturation: Double = Double.NaN,
    vividness: Double = Double.NaN,
    blackness: Double = Double.NaN,
    whiteness: Double = Double.NaN,
): Zcam = Zcam(
    hz = hue,
    Qz = brightness,
    Jz = lightness,
    Mz = colorfulness,
    Cz = chroma,
    Sz = saturation,
    Vz = vividness,
    Kz = blackness,
    Wz = whiteness,
    cond = LocalZcamViewingConditions.current,
)

@Composable
fun CieXyz.toRgb(): Rgb = toRgb(LocalLuminance.current, LocalRgbColorSpace.current)

@Composable
fun CieLab.toXyz(): CieXyz = toXyz(LocalWhitePoint.current, LocalLuminance.current)

@Composable
fun CieXyz.toCieLab(): CieLab = toCieLab(LocalWhitePoint.current, LocalLuminance.current)

@Composable
fun Rgb.toXyz(): CieXyz = toXyz(LocalLuminance.current)

@Composable
fun Rgb.toZcam(): Zcam = toXyz().toIzazbz().toZcam()

@Composable
fun Oklch.clampToRgb(): Rgb = clampToRgb(LocalRgbColorSpace.current)

@Composable
fun Izazbz.toZcam(): Zcam = toZcam(LocalZcamViewingConditions.current)

@Composable
fun Zcam.toRgb(): Rgb =
    toIzazbz().toXyz()
        .toRgb(LocalZcamViewingConditions.current.luminance, LocalRgbColorSpace.current)

@Composable
fun Zcam.clampToRgb(): Rgb = clampToRgb(LocalRgbColorSpace.current)
