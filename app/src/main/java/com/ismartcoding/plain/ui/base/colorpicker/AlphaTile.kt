package com.ismartcoding.plain.ui.base.colorpicker

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageBitmapConfig
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp

/**
 * AlphaTile allows you to display ARGB colors including transparency with tiles.
 *
 * @param modifier [Modifier] to decorate the internal Canvas.
 * @param controller Allows you to control and interacts with color pickers and all relevant subcomponents.
 * @param selectedColor Color to be displayed over the tiles if the [controller] is not registered.
 * @param tileOddColor Color of the odd tiles.
 * @param tileEvenColor Color of the even tiles.
 * @param tileSize DP size of tiles.
 */
@Composable
public fun AlphaTile(
  modifier: Modifier,
  controller: ColorPickerController? = null,
  selectedColor: Color = Color.Transparent,
  tileOddColor: Color = defaultTileOddColor,
  tileEvenColor: Color = defaultTileEvenColor,
  tileSize: Dp = 12.dp,
) {
  val density = LocalDensity.current
  var backgroundBitmap: ImageBitmap? = null
  var bitmapSize = IntSize(0, 0)
  val colorPaint: Paint = Paint().apply {
    this.color = controller?.selectedColor?.value ?: selectedColor
  }

  Canvas(
    modifier = modifier
      .fillMaxSize()
      .onSizeChanged { newSize ->
        val size =
          newSize.takeIf { it.width != 0 && it.height != 0 } ?: return@onSizeChanged
        val drawable =
          AlphaTileDrawable(
            with(density) { tileSize.toPx() },
            tileOddColor,
            tileEvenColor,
          )
        backgroundBitmap
          ?.asAndroidBitmap()
          ?.recycle()
        backgroundBitmap =
          ImageBitmap(size.width, size.height, ImageBitmapConfig.Argb8888).apply {
            val backgroundCanvas = Canvas(this)
            drawable.setBounds(
              0,
              0,
              backgroundCanvas.nativeCanvas.width,
              backgroundCanvas.nativeCanvas.height,
            )
            drawable.draw(backgroundCanvas.nativeCanvas)
          }
        bitmapSize = size
      },
  ) {
    drawIntoCanvas { canvas ->
      backgroundBitmap?.let {
        canvas.drawImage(it, Offset.Zero, Paint())
        canvas.drawRect(
          0f,
          0f,
          bitmapSize.width.toFloat(),
          bitmapSize.height.toFloat(),
          colorPaint,
        )
      }
    }
  }
}
