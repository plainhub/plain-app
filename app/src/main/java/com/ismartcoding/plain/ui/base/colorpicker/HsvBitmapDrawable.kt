package com.ismartcoding.plain.ui.base.colorpicker

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.RadialGradient
import android.graphics.Shader
import android.graphics.SweepGradient
import android.graphics.drawable.BitmapDrawable
import kotlin.math.min

/**
 * HsvBitmapDrawable draws hsv color gradient with hue and saturation on a canvas.
 *
 * @param resources [Resources] to initialize [BitmapDrawable].
 * @param bitmap [Bitmap] to draw on the canvas.
 */
internal class HsvBitmapDrawable constructor(
  resources: Resources,
  bitmap: Bitmap,
) : BitmapDrawable(resources, bitmap) {

  private val huePaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)
  private val saturationPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)

  override fun draw(canvas: Canvas) {
    val width = bounds.width()
    val height = bounds.height()
    val centerX = width * 0.5f
    val centerY = height * 0.5f
    val radius = min(width, height) * 0.5f

    val sweepShader = SweepGradient(
      centerX,
      centerY,
      intArrayOf(
        Color.RED,
        Color.MAGENTA,
        Color.BLUE,
        Color.CYAN,
        Color.GREEN,
        Color.YELLOW,
        Color.RED,
      ),
      floatArrayOf(0.000f, 0.166f, 0.333f, 0.499f, 0.666f, 0.833f, 0.999f),
    )
    huePaint.shader = sweepShader

    val saturationShader = RadialGradient(
      centerX,
      centerY,
      radius,
      Color.WHITE,
      0x00FFFFFF,
      Shader.TileMode.CLAMP,
    )
    saturationPaint.shader = saturationShader

    canvas.drawCircle(centerX, centerY, radius, huePaint)
    canvas.drawCircle(centerX, centerY, radius, saturationPaint)
  }

  override fun setAlpha(alpha: Int) {
    huePaint.alpha = alpha
  }

  override fun setColorFilter(colorFilter: ColorFilter?) {
    huePaint.colorFilter = colorFilter
  }

  @Deprecated(
    message = "This method will be deprecated on the future Android SDK",
    replaceWith = ReplaceWith(expression = "getOpacity"),
  )
  override fun getOpacity(): Int {
    return PixelFormat.OPAQUE
  }
}
