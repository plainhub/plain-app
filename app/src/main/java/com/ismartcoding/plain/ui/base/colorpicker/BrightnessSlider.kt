package com.ismartcoding.plain.ui.base.colorpicker

import androidx.annotation.FloatRange
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageBitmapConfig
import androidx.compose.ui.graphics.LinearGradientShader
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.math.max

/**
 * BrightnessSlider allows you to adjust the brightness value of the selected color from color pickers.
 *
 * @param modifier [Modifier] to decorate the internal Canvas.
 * @param controller Allows you to control and interacts with color pickers and all relevant subcomponents.
 * @param borderRadius Radius of the border.
 * @param borderSize [Dp] size of the border.
 * @param borderColor [Color] of the border.
 * @param wheelImageBitmap [ImageBitmap] to draw the wheel.
 * @param wheelRadius Radius of the wheel.
 * @param wheelColor [Color] of th wheel.
 * @param wheelPaint [Paint] to draw the wheel.
 * @param initialColor [Color] of the initial state. This property works for [HsvColorPicker] and
 * it will be selected on rightmost of slider if you give null value.
 */
@Composable
public fun BrightnessSlider(
  modifier: Modifier = Modifier,
  controller: ColorPickerController,
  borderRadius: Dp = 6.dp,
  borderSize: Dp = 5.dp,
  borderColor: Color = Color.LightGray,
  wheelImageBitmap: ImageBitmap? = null,
  wheelRadius: Dp = 12.dp,
  wheelColor: Color = Color.White,
  @FloatRange(from = 0.0, to = 1.0) wheelAlpha: Float = 1.0f,
  wheelPaint: Paint = Paint().apply {
    color = wheelColor
    alpha = wheelAlpha
  },
  initialColor: Color? = null,
) {
  var backgroundBitmap: ImageBitmap? = null
  var bitmapSize = IntSize(0, 0)
  val borderPaint: Paint = Paint().apply {
    style = PaintingStyle.Stroke
    strokeWidth = with(LocalDensity.current) { borderSize.toPx() }
    color = borderColor
  }
  val colorPaint: Paint = Paint().apply {
    color = controller.pureSelectedColor.value
  }
  var isInitialized by remember { mutableStateOf(false) }

  SideEffect {
    controller.isAttachedBrightnessSlider = true
  }

  Canvas(
    modifier = modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(borderRadius))
      .onSizeChanged { newSize ->
        val size =
          newSize.takeIf { it.width != 0 && it.height != 0 } ?: return@onSizeChanged
        backgroundBitmap
          ?.asAndroidBitmap()
          ?.recycle()
        backgroundBitmap =
          ImageBitmap(size.width, size.height, ImageBitmapConfig.Argb8888).apply {
            val backgroundCanvas = Canvas(this)
            backgroundCanvas.drawRoundRect(
              left = 0f,
              top = 0f,
              right = size.width.toFloat(),
              bottom = size.height.toFloat(),
              radiusX = borderRadius.value,
              radiusY = borderRadius.value,
              paint = borderPaint,
            )
          }
        bitmapSize = size
      }
      .pointerInput(Unit) {
        detectTapGestures { offset ->
          // calculate wheel position.
          val wheelPoint = offset.x
          val position: Float = if (wheelImageBitmap == null) {
            val point = wheelPoint.coerceIn(
              minimumValue = 0f,
              maximumValue = bitmapSize.width.toFloat(),
            )
            point / bitmapSize.width
          } else {
            val point = wheelPoint.coerceIn(
              minimumValue = 0f,
              maximumValue = bitmapSize.width.toFloat(),
            )
            point / bitmapSize.width
          }
          controller.setBrightness(position.coerceIn(0f, 1f), fromUser = true)
        }
      }
      .pointerInput(Unit) {
        detectHorizontalDragGestures { change, _ ->
          // calculate wheel position.
          val wheelPoint = change.position.x
          val position: Float = if (wheelImageBitmap == null) {
            val point = wheelPoint.coerceIn(
              minimumValue = 0f,
              maximumValue = bitmapSize.width.toFloat(),
            )
            point / bitmapSize.width
          } else {
            val point = wheelPoint.coerceIn(
              minimumValue = 0f,
              maximumValue = bitmapSize.width.toFloat(),
            )
            point / bitmapSize.width
          }
          controller.setBrightness(position.coerceIn(0f, 1f), fromUser = true)
        }
      },
  ) {
    drawIntoCanvas { canvas ->
      backgroundBitmap?.let {
        // draw background bitmap.
        canvas.drawImage(it, Offset.Zero, Paint())

        // draw a linear gradient color shader.
        val startColor = controller.pureSelectedColor.value.copy(alpha = 0f)
        val endColor = controller.pureSelectedColor.value.copy(alpha = 1f)
        val shader = LinearGradientShader(
          colors = listOf(startColor, endColor),
          from = Offset.Zero,
          to = Offset(bitmapSize.width.toFloat(), bitmapSize.height.toFloat()),
          tileMode = TileMode.Clamp,
        )
        colorPaint.shader = shader
        canvas.drawRoundRect(
          left = 0f,
          top = 0f,
          right = bitmapSize.width.toFloat(),
          bottom = bitmapSize.height.toFloat(),
          radiusX = borderRadius.value,
          radiusY = borderRadius.value,
          paint = colorPaint,
        )

        // draw wheel bitmap on the canvas.
        if (wheelImageBitmap == null) {
          val position = controller.brightness.value
          val point = (bitmapSize.width * position).coerceIn(
            minimumValue = 0f,
            maximumValue = bitmapSize.width.toFloat(),
          )
          canvas.drawCircle(
            Offset(x = point, y = bitmapSize.height / 2f),
            wheelRadius.toPx(),
            wheelPaint,
          )
        } else {
          val position = controller.brightness.value
          val point = (bitmapSize.width * position).coerceIn(
            minimumValue = 0f,
            maximumValue = bitmapSize.width.toFloat(),
          )
          canvas.drawImage(
            wheelImageBitmap,
            Offset(
              x = point - (wheelImageBitmap.width / 2),
              y = bitmapSize.height / 2f - wheelImageBitmap.height / 2,
            ),
            Paint(),
          )
        }
        if (initialColor != null && !isInitialized) {
          isInitialized = true
          val brightness =
            max(max(initialColor.red, initialColor.green), initialColor.blue)
          controller.setBrightness(brightness, fromUser = false)
        }
      }
    }
  }
}
