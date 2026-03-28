package com.ismartcoding.plain.ui.base.colorpicker

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
        bitmap?.asAndroidBitmap()?.recycle()
        val (newBitmap, drawable) = createHsvBitmap(size, context, controller)
        bitmap = newBitmap
        hsvBitmapDrawable = drawable
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
      hsvBitmapDrawable?.draw(canvas.nativeCanvas)

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

      if (initialColor != null && !isInitialized) {
        drawInitialColor(initialColor, controller, center.x, center.y) {
          isInitialized = true
        }
      }
    }
    controller.reviseTick.value
  }
}
