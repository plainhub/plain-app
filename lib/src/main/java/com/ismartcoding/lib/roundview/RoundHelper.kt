package com.ismartcoding.lib.roundview

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.View

interface RoundHelper {
    fun init(
        context: Context,
        attrs: AttributeSet?,
        view: View,
    )

    fun onSizeChanged(
        width: Int,
        height: Int,
    )

    fun preDraw(canvas: Canvas)

    fun drawPath(
        canvas: Canvas,
        drawableState: IntArray?,
    )

    fun setCircle(isCircle: Boolean)

    fun setRadius(radiusDp: Float)

    fun setRadius(
        radiusTopLeftDp: Float,
        radiusTopRightDp: Float,
        radiusBottomLeftDp: Float,
        radiusBottomRightDp: Float,
    )

    fun setRadiusLeft(radiusDp: Float)

    fun setRadiusRight(radiusDp: Float)

    fun setRadiusTop(radiusDp: Float)

    fun setRadiusBottom(radiusDp: Float)

    fun setRadiusTopLeft(radiusDp: Float)

    fun setRadiusTopRight(radiusDp: Float)

    fun setRadiusBottomLeft(radiusDp: Float)

    fun setRadiusBottomRight(radiusDp: Float)

    fun setStrokeWidth(widthDp: Float)

    fun setStrokeColor(color: Int)

    fun setStrokeWidthColor(
        widthDp: Float,
        color: Int,
    )
}
