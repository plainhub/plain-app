package com.ismartcoding.plain.ui.base.colorpicker

internal class AffineMatrix {
    var scaleX: Float = 1f
    var scaleY: Float = 1f
    var translateX: Float = 0f
    var translateY: Float = 0f

    fun setScale(sx: Float, sy: Float) { scaleX = sx; scaleY = sy }
    fun postTranslate(dx: Float, dy: Float) { translateX += dx; translateY += dy }
    fun invert(result: AffineMatrix): Boolean {
        if (scaleX == 0f || scaleY == 0f) return false
        result.scaleX = 1f / scaleX
        result.scaleY = 1f / scaleY
        result.translateX = -translateX / scaleX
        result.translateY = -translateY / scaleY
        return true
    }
    fun mapPoints(points: FloatArray) {
        points[0] = points[0] * scaleX + translateX
        points[1] = points[1] * scaleY + translateY
    }
}
