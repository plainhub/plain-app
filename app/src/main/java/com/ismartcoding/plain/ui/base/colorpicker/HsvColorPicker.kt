package com.ismartcoding.plain.ui.base.colorpicker

import android.graphics.Matrix
import android.graphics.RectF
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageBitmapConfig
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

/**
 * HsvColorPicker allows you to get colors from HSV color palette by tapping on the desired color.
 *
 * @param modifier [Modifier] to decorate the internal Canvas.
 * @param controller Allows you to control and interacts with color pickers and all relevant subcomponents.
 * @param wheelImageBitmap [ImageBitmap] to draw the wheel.
 * @param drawOnPosSelected to draw anything on the canvas when [ColorPickerController.selectedPoint] changes
 * @param drawDefaultWheelIndicator should the indicator be drawn on the canvas. Defaults to false if either [wheelImageBitmap] or [drawOnPosSelected] are not null.
 * @param onColorChanged Color changed listener.
 * @param initialColor [Color] of the initial state. This property works for [HsvColorPicker] and
 * it will be selected on center if you give null value.
 */
@Composable
public fun HsvColorPicker(
  modifier: Modifier,
  controller: ColorPickerController,
  wheelImageBitmap: ImageBitmap? = null,
  drawOnPosSelected: (DrawScope.() -> Unit)? = null,
  drawDefaultWheelIndicator: Boolean = wheelImageBitmap == null && drawOnPosSelected == null,
  onColorChanged: ((colorEnvelope: ColorEnvelope) -> Unit)? = null,
  initialColor: Color? = null,
) {
  var isInitialized by remember { mutableStateOf(false) }
  val context = LocalContext.current
  var hsvBitmapDrawable: HsvBitmapDrawable? = null
  var bitmap: ImageBitmap? = null
  val coroutineScope = rememberCoroutineScope()
  DisposableEffect(key1 = controller) {
    coroutineScope.launch(Dispatchers.Main) {
      controller.isHsvColorPalette = true
      bitmap?.let { controller.setPaletteImageBitmap(it) }
      controller.setWheelImageBitmap(wheelImageBitmap)
      controller.colorChangedTick.mapNotNull { it }.collect {
        if (isInitialized) {
          onColorChanged?.invoke(it)
        }
      }
    }

    onDispose {
      controller.releaseBitmap()
    }
  }

  Canvas(
    modifier = modifier
      .fillMaxSize()
      .onSizeChanged { newSize ->
        val size =
          newSize.takeIf { it.width != 0 && it.height != 0 } ?: return@onSizeChanged
        controller.canvasSize.value = size
        bitmap
          ?.asAndroidBitmap()
          ?.recycle()
        bitmap = ImageBitmap(size.width, size.height, ImageBitmapConfig.Argb8888).also {
          hsvBitmapDrawable =
            HsvBitmapDrawable(context.resources, it.asAndroidBitmap()).apply {
              setBounds(
                0,
                0,
                size.width,
                size.height,
              )
            }

          var dx = 0f
          var dy = 0f
          val scale: Float
          val shaderMatrix = Matrix()
          val mDrawableRect = RectF(0f, 0f, size.width.toFloat(), size.height.toFloat())
          val bitmapWidth: Int = it.asAndroidBitmap().width
          val bitmapHeight: Int = it.asAndroidBitmap().height

          if (bitmapWidth * mDrawableRect.height() >
            mDrawableRect.width() * bitmapHeight
          ) {
            scale = mDrawableRect.height() / bitmapHeight.toFloat()
            dx = (mDrawableRect.width() - bitmapWidth * scale) * 0.5f
          } else {
            scale = mDrawableRect.width() / bitmapWidth.toFloat()
            dy = (mDrawableRect.height() - bitmapHeight * scale) * 0.5f
          }
          // resize the matrix to scale by sx and sy.
          shaderMatrix.setScale(scale, scale)

          // post translate the matrix with the specified translation.
          shaderMatrix.postTranslate(
            (dx + 0.5f) + mDrawableRect.left,
            (dy + 0.5f) + mDrawableRect.top,
          )

          // set the shader matrix to the controller.
          controller.imageBitmapMatrix.value = shaderMatrix
        }
      }
      .pointerInput(Unit) {
        detectTapGestures { offset ->
          controller.selectByCoordinate(offset.x, offset.y, true)
        }
      }
      .pointerInput(Unit) {
        detectDragGestures { change, _ ->
          controller.selectByCoordinate(change.position.x, change.position.y, true)
        }
      },
  ) {
    drawIntoCanvas { canvas ->
      // draw hsv bitmap on the canvas.
      hsvBitmapDrawable?.draw(canvas.nativeCanvas)

      // draw wheel bitmap on the canvas.
      val point = controller.selectedPoint.value
      val wheelBitmap = controller.wheelBitmap
      if (wheelBitmap != null) {
        canvas.drawImage(
          wheelBitmap,
          Offset(point.x - wheelBitmap.width / 2, point.y - wheelBitmap.height / 2),
          Paint(),
        )
      }

      if (drawDefaultWheelIndicator) {
        canvas.drawCircle(
          Offset(point.x, point.y),
          controller.wheelRadius.toPx(),
          controller.wheelPaint,
        )
      }

      if (drawOnPosSelected != null) {
        this.drawOnPosSelected()
      }

      val palette = controller.paletteBitmap
      if (palette != null && initialColor != null && !isInitialized) {
        val pickerRadius: Float = palette.width.coerceAtMost(palette.height) * 0.5f
        if (pickerRadius > 0) {
          isInitialized = true
          val hsv = FloatArray(3)
          android.graphics.Color.RGBToHSV(
            (initialColor.red * 255).toInt(),
            (initialColor.green * 255).toInt(),
            (initialColor.blue * 255).toInt(),
            hsv,
          )
          val angle = (Math.PI / 180f) * hsv[0] * (-1)
          val saturationVector = pickerRadius * hsv[1]
          val x = saturationVector * cos(angle) + center.x
          val y = saturationVector * sin(angle) + center.y
          controller.selectByCoordinate(x.toFloat(), y.toFloat(), false)
        }
      }
    }
    controller.reviseTick.value
  }
}
