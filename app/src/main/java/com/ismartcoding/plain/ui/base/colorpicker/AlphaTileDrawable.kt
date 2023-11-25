package com.ismartcoding.plain.ui.base.colorpicker

import android.graphics.BitmapShader
import android.graphics.ColorFilter
import android.graphics.PixelFormat
import android.graphics.Shader
import android.graphics.drawable.Drawable
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageBitmapConfig
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.asAndroidBitmap

/**
 * AlphaTileDrawable displays ARGB colors including transparency with tiles on a canvas.
 *
 * @param tileSize DP size of tiles.
 * @param tileOddColor Color of the odd tiles.
 * @param tileEvenColor Color of the even tiles.
 */
public class AlphaTileDrawable constructor(
  tileSize: Float,
  tileOddColor: Color,
  tileEvenColor: Color,
) : Drawable() {

  private val androidPaint: android.graphics.Paint = android.graphics.Paint(
    android.graphics.Paint.ANTI_ALIAS_FLAG,
  )

  init {
    val size = tileSize.toInt()
    val imageBitmap = ImageBitmap(size * 2, size * 2, ImageBitmapConfig.Argb8888)
    val canvas = Canvas(imageBitmap)
    val rect = Rect(0f, 0f, tileSize, tileSize)

    val bitmapPaint = Paint().apply {
      style = PaintingStyle.Fill
      isAntiAlias = true
    }

    bitmapPaint.color = tileOddColor
    drawTile(canvas, rect, bitmapPaint, 0f, 0f)
    drawTile(canvas, rect, bitmapPaint, tileSize, tileSize)

    bitmapPaint.color = tileEvenColor
    drawTile(canvas, rect, bitmapPaint, 0f, tileSize)
    drawTile(canvas, rect, bitmapPaint, tileSize, 0f)

    androidPaint.shader = BitmapShader(
      imageBitmap.asAndroidBitmap(),
      Shader.TileMode.REPEAT,
      Shader.TileMode.REPEAT,
    )
  }

  private fun drawTile(canvas: Canvas, rect: Rect, paint: Paint, dx: Float, dy: Float) {
    val translated = rect.translate(dx, dy)
    canvas.drawRect(translated, paint)
  }

  override fun draw(canvas: android.graphics.Canvas) {
    canvas.drawPaint(androidPaint)
  }

  override fun setAlpha(alpha: Int) {
    androidPaint.alpha = alpha
  }

  override fun setColorFilter(colorFilter: ColorFilter?) {
    androidPaint.colorFilter = colorFilter
  }

  @Deprecated(
    message = "This method will be deprecated on the future Android SDK",
    replaceWith = ReplaceWith(expression = "getOpacity"),
  )
  override fun getOpacity(): Int = PixelFormat.OPAQUE
}
