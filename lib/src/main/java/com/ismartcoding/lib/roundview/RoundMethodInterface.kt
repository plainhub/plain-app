package com.ismartcoding.lib.roundview

interface RoundMethodInterface {
    val helper: RoundHelper
}

fun RoundMethodInterface.setRadius(radiusDp: Float) {
    helper.setRadius(radiusDp)
}

fun RoundMethodInterface.setRadius(
    radiusTopLeftDp: Float,
    radiusTopRightDp: Float,
    radiusBottomLeftDp: Float,
    radiusBottomRightDp: Float,
) {
    helper.setRadius(radiusTopLeftDp, radiusTopRightDp, radiusBottomLeftDp, radiusBottomRightDp)
}

fun RoundMethodInterface.setRadiusLeft(radiusDp: Float) {
    helper.setRadiusLeft(radiusDp)
}

fun RoundMethodInterface.setRadiusRight(radiusDp: Float) {
    helper.setRadiusRight(radiusDp)
}

fun RoundMethodInterface.setRadiusTop(radiusDp: Float) {
    helper.setRadiusTop(radiusDp)
}

fun RoundMethodInterface.setRadiusBottom(radiusDp: Float) {
    helper.setRadiusBottom(radiusDp)
}

fun RoundMethodInterface.setRadiusTopLeft(radiusDp: Float) {
    helper.setRadiusTopLeft(radiusDp)
}

fun RoundMethodInterface.setRadiusTopRight(radiusDp: Float) {
    helper.setRadiusTopRight(radiusDp)
}

fun RoundMethodInterface.setRadiusBottomLeft(radiusDp: Float) {
    helper.setRadiusBottomLeft(radiusDp)
}

fun RoundMethodInterface.setRadiusBottomRight(radiusDp: Float) {
    helper.setRadiusBottomRight(radiusDp)
}

fun RoundMethodInterface.setStrokeWidth(widthDp: Float) {
    helper.setStrokeWidth(widthDp)
}

fun RoundMethodInterface.setStrokeColor(color: Int) {
    helper.setStrokeColor(color)
}

fun RoundMethodInterface.setStrokeWidthColor(
    widthDp: Float,
    color: Int,
) {
    helper.setStrokeWidthColor(widthDp, color)
}
