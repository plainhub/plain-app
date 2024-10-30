package com.ismartcoding.plain.ui.base.colorpicker

import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.PointF
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.math.atan2
import kotlin.math.sqrt

/** Creates and remembers a [ColorPickerController] on the current composer. */
@Composable
public fun rememberColorPickerController(): ColorPickerController {
  return remember { ColorPickerController() }
}

/**
 * [ColorPickerController] allows you to control and interacts with the [ImageColorPicker], [HsvColorPicker],
 * and all relevant subcomponents. You can create and remember [ColorPickerController]
 * with the [rememberColorPickerController] extension.
 */
@Stable
public class ColorPickerController {

  /** An [ImageBitmap] to be drawn on the canvas as a palette. */
  internal var paletteBitmap: ImageBitmap? = null

  /** An [ImageBitmap] to be drawn on the canvas as a wheel. */
  internal var wheelBitmap: ImageBitmap? = null

  private val _selectedPoint: MutableState<PointF> = mutableStateOf(PointF(0f, 0f))

  /** State of [PointF], which represents the currently selected coordinate. */
  public val selectedPoint: State<PointF> = _selectedPoint

  private val _selectedColor: MutableState<Color> = mutableStateOf(Color.Transparent)

  /** State of [Color], which represents the currently selected color value with alpha and brightness. */
  public val selectedColor: State<Color> = _selectedColor

  /** State of [Color], which represents the currently selected color value without alpha and brightness. */
  internal var pureSelectedColor: MutableState<Color> = mutableStateOf(Color.Transparent)

  /** Alpha value to be applied with the selected color. */
  internal var alpha: MutableState<Float> = mutableFloatStateOf(1.0f)

  /** Brightness value to be applied with the selected color. */
  internal var brightness: MutableState<Float> = mutableFloatStateOf(1.0f)

  /** Radius to draw default wheel. */
  internal var wheelRadius: Dp = 12.dp
    private set

  /** Paint to draw default wheel. */
  internal var wheelPaint: Paint = Paint().apply { color = Color.White }
    private set

  /** Enable or not color selection. */
  private val enabled: MutableState<Boolean> = mutableStateOf(true)

  /** Decide the content scale of the palette when draws. */
  private var paletteContentScale: PaletteContentScale = PaletteContentScale.FIT

  /** Size of the measured canvas dimensions (width and height). */
  internal val canvasSize: MutableState<IntSize> = mutableStateOf(IntSize(0, 0))

  /** Matrix of the [paletteBitmap], which is used to calculate pixel positions. */
  internal val imageBitmapMatrix: MutableState<Matrix> = mutableStateOf(Matrix())

  /** Indicates if the color picker is HSV model. */
  internal var isHsvColorPalette: Boolean = false

  /** Indicates if the alpha slider has been attached. */
  internal var isAttachedAlphaSlider: Boolean = false

  /** Indicates if the brightness slider has been attached. */
  internal var isAttachedBrightnessSlider: Boolean = false

  internal var reviseTick = mutableIntStateOf(0)

  internal var colorChangedTick = MutableStateFlow<ColorEnvelope?>(null)

  private val debounceHandler = Handler(Looper.getMainLooper())

  private var debounceDuration: Long = 0L

  /** Set an [ImageBitmap] to draw on the canvas as a palette. */
  public fun setPaletteImageBitmap(imageBitmap: ImageBitmap) {
    val targetSize = canvasSize.value.takeIf { it.width != 0 && it.height != 0 }
      ?: throw IllegalAccessException(
        "Can't set an ImageBitmap before initializing the canvas",
      )
    val copiedBitmap =
      imageBitmap.asAndroidBitmap().copy(Bitmap.Config.ARGB_8888, false)
    val resized = when (paletteContentScale) {
      PaletteContentScale.FIT -> BitmapCalculator.scaleBitmap(copiedBitmap, targetSize)
      PaletteContentScale.CROP -> BitmapCalculator.cropBitmap(copiedBitmap, targetSize)
    }
    paletteBitmap = resized.asImageBitmap()
    copiedBitmap.recycle()
    selectCenter(fromUser = false)
    reviseTick.intValue++
  }

  /** Set a [PaletteContentScale] to the palette bitmap. */
  public fun setPaletteContentScale(paletteContentScale: PaletteContentScale) {
    this.paletteContentScale = paletteContentScale
  }

  /** Set an [ImageBitmap] to draw on the canvas as a wheel. */
  public fun setWheelImageBitmap(imageBitmap: ImageBitmap?) {
    wheelBitmap = imageBitmap
  }

  /** Set a radius to draw default wheel. */
  public fun setWheelRadius(radius: Dp) {
    wheelRadius = radius
    reviseTick.intValue++
  }

  /** Set a paint to draw default wheel. */
  public fun setWheelPaint(paint: Paint) {
    wheelPaint = paint
    reviseTick.intValue++
  }

  /** Set a color for the wheel. */
  public fun setWheelColor(color: Color) {
    wheelPaint.color = color
    reviseTick.intValue++
  }

  /** Set an alpha for the wheel. */
  public fun setWheelAlpha(alpha: Float) {
    wheelPaint.alpha = alpha
    reviseTick.intValue++
  }

  /** Enable or unable color selection. */
  public fun setEnabled(enabled: Boolean) {
    this.enabled.value = enabled
  }

  /** Set the debounce duration. */
  public fun setDebounceDuration(duration: Long) {
    debounceDuration = duration
  }

  /**
   * Select a specific point by coordinates and update a selected color.
   *
   * @param x x-coordinate to extract a pixel color.
   * @param y y-coordinate to extract a pixel color.
   * @param fromUser Represents this event is triggered by user or not.
   */
  public fun selectByCoordinate(x: Float, y: Float, fromUser: Boolean) {
    enabled.value.takeIf { it } ?: return
    val snapPoint = PointMapper.getColorPoint(this, PointF(x, y))
    val extractedColor = if (isHsvColorPalette) {
      extractPixelHsvColor(snapPoint.x, snapPoint.y)
    } else {
      extractPixelColor(snapPoint.x, snapPoint.y)
    }
    if (extractedColor != Color.Transparent) {
      // set the extracted color.
      pureSelectedColor.value = extractedColor
      _selectedPoint.value = PointF(snapPoint.x, snapPoint.y)
      _selectedColor.value = applyHSVFactors(extractedColor)

      // notify color changes to the listeners.
      if (fromUser && debounceDuration != 0L) {
        notifyColorChangedWithDebounce(fromUser)
      } else {
        notifyColorChanged(fromUser)
      }
    }
  }

  /**
   * Select center point of the palette.
   *
   * @param fromUser Represents this event is triggered by user or not.
   */
  public fun selectCenter(fromUser: Boolean) {
    val size = canvasSize.value
    selectByCoordinate(size.width * 0.5f, size.height * 0.5f, fromUser)
  }

  /** Notify color changes to the color picker and other subcomponents. */
  private fun notifyColorChanged(fromUser: Boolean) {
    val color = _selectedColor.value
    colorChangedTick.value = ColorEnvelope(color, color.hexCode, fromUser)
  }

  /** Notify color changes to the color picker and other subcomponents with debounce duration. */
  private fun notifyColorChangedWithDebounce(fromUser: Boolean) {
    val runnable = { notifyColorChanged(fromUser) }
    debounceHandler.removeCallbacksAndMessages(null)
    debounceHandler.postDelayed(runnable, debounceDuration)
  }

  /** Combine the alpha value to the selected pure color. */
  internal fun setAlpha(alpha: Float, fromUser: Boolean) {
    this.alpha.value = alpha
    _selectedColor.value = selectedColor.value.copy(alpha = alpha)
    notifyColorChanged(fromUser)
  }

  /** Combine the brightness value to the selected pure color. */
  internal fun setBrightness(brightness: Float, fromUser: Boolean) {
    this.brightness.value = brightness
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(pureSelectedColor.value.toArgb(), hsv)
    hsv[2] = brightness
    _selectedColor.value =
      Color(android.graphics.Color.HSVToColor((alpha.value * 255).toInt(), hsv))
    if (fromUser && debounceDuration != 0L) {
      notifyColorChangedWithDebounce(fromUser)
    } else {
      notifyColorChanged(fromUser)
    }
  }

  /** Return a [Color] that is applied with HSV color factors to the [color]. */
  private fun applyHSVFactors(color: Color): Color {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(color.toArgb(), hsv)
    if (isAttachedBrightnessSlider) {
      hsv[2] = brightness.value
    }
    return if (isAttachedAlphaSlider) {
      Color(android.graphics.Color.HSVToColor((alpha.value * 255).toInt(), hsv))
    } else {
      Color(android.graphics.Color.HSVToColor(hsv))
    }
  }

  /**
   * Extract a pixel color from the [paletteBitmap].
   *
   * @param x x-coordinate to extract a pixel color.
   * @param y y-coordinate to extract a pixel color.
   *
   * @return An extracted [Color] from the desired coordinates.
   * if fail to extract a pixel value, it will returns [Color.Transparent].
   */
  internal fun extractPixelColor(x: Float, y: Float): Color {
    val invertMatrix = Matrix()
    imageBitmapMatrix.value.invert(invertMatrix)

    val mappedPoints = floatArrayOf(x, y)
    invertMatrix.mapPoints(mappedPoints)

    val palette = paletteBitmap
    if (palette != null &&
      mappedPoints[0] >= 0 &&
      mappedPoints[1] >= 0 &&
      mappedPoints[0] < palette.width &&
      mappedPoints[1] < palette.height
    ) {
      val scaleX = mappedPoints[0] / palette.width
      val x1 = scaleX * palette.width
      val scaleY = mappedPoints[1] / palette.height
      val y1 = scaleY * palette.height
      val pixelColor = palette.asAndroidBitmap().getPixel(x1.toInt(), y1.toInt())
      return Color(pixelColor)
    }
    return Color.Transparent
  }

  private fun extractPixelHsvColor(x: Float, y: Float): Color {
    val invertMatrix = Matrix()
    imageBitmapMatrix.value.invert(invertMatrix)

    val mappedPoints = floatArrayOf(x, y)
    invertMatrix.mapPoints(mappedPoints)

    val palette = paletteBitmap
    if (palette != null &&
      mappedPoints[0] >= 0 &&
      mappedPoints[1] >= 0 &&
      mappedPoints[0] < palette.width &&
      mappedPoints[1] < palette.height
    ) {
      val x2 = x - palette.width * 0.5f
      val y2 = y - palette.height * 0.5f
      val size = canvasSize.value
      val r = sqrt((x2 * x2 + y2 * y2).toDouble())
      val radius: Float = size.width.coerceAtMost(size.height) * 0.5f
      val hsv = floatArrayOf(0f, 0f, 1f)
      (
        (
          atan2(
            y2.toDouble(),
            -x2.toDouble(),
          ) / Math.PI * 180f
          ).toFloat() + 180
        ).also { hsv[0] = it }
      hsv[1] = 0f.coerceAtLeast(1f.coerceAtMost((r / radius).toFloat()))
      return Color(android.graphics.Color.HSVToColor(hsv))
    }
    return Color.Transparent
  }

  internal fun releaseBitmap() {
    paletteBitmap?.asAndroidBitmap()?.recycle()
    wheelBitmap?.asAndroidBitmap()?.recycle()
    paletteBitmap = null
    wheelBitmap = null
  }
}
