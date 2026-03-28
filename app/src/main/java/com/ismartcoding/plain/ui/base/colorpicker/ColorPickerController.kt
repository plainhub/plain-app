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

@Composable
public fun rememberColorPickerController(): ColorPickerController {
  return remember { ColorPickerController() }
}

@Stable
public class ColorPickerController {
  internal var paletteBitmap: ImageBitmap? = null
  internal var wheelBitmap: ImageBitmap? = null

  internal val _selectedPoint: MutableState<PointF> = mutableStateOf(PointF(0f, 0f))
  public val selectedPoint: State<PointF> = _selectedPoint

  internal val _selectedColor: MutableState<Color> = mutableStateOf(Color.Transparent)
  public val selectedColor: State<Color> = _selectedColor

  internal var pureSelectedColor: MutableState<Color> = mutableStateOf(Color.Transparent)
  internal var alpha: MutableState<Float> = mutableFloatStateOf(1.0f)
  internal var brightness: MutableState<Float> = mutableFloatStateOf(1.0f)

  internal var wheelRadius: Dp = 12.dp
    private set
  internal var wheelPaint: Paint = Paint().apply { color = Color.White }
    private set

  private val enabled: MutableState<Boolean> = mutableStateOf(true)
  private var paletteContentScale: PaletteContentScale = PaletteContentScale.FIT

  internal val canvasSize: MutableState<IntSize> = mutableStateOf(IntSize(0, 0))
  internal val imageBitmapMatrix: MutableState<Matrix> = mutableStateOf(Matrix())
  internal var isHsvColorPalette: Boolean = false
  internal var isAttachedAlphaSlider: Boolean = false
  internal var isAttachedBrightnessSlider: Boolean = false
  internal var reviseTick = mutableIntStateOf(0)
  internal var colorChangedTick = MutableStateFlow<ColorEnvelope?>(null)
  internal val debounceHandler = Handler(Looper.getMainLooper())
  internal var debounceDuration: Long = 0L

  public fun setPaletteImageBitmap(imageBitmap: ImageBitmap) {
    val targetSize = canvasSize.value.takeIf { it.width != 0 && it.height != 0 }
      ?: throw IllegalAccessException("Can't set an ImageBitmap before initializing the canvas")
    val copiedBitmap = imageBitmap.asAndroidBitmap().copy(Bitmap.Config.ARGB_8888, false)
    val resized = when (paletteContentScale) {
      PaletteContentScale.FIT -> BitmapCalculator.scaleBitmap(copiedBitmap, targetSize)
      PaletteContentScale.CROP -> BitmapCalculator.cropBitmap(copiedBitmap, targetSize)
    }
    paletteBitmap = resized.asImageBitmap()
    copiedBitmap.recycle()
    selectCenter(fromUser = false)
    reviseTick.intValue++
  }

  public fun setPaletteContentScale(paletteContentScale: PaletteContentScale) { this.paletteContentScale = paletteContentScale }
  public fun setWheelImageBitmap(imageBitmap: ImageBitmap?) { wheelBitmap = imageBitmap }
  public fun setWheelRadius(radius: Dp) { wheelRadius = radius; reviseTick.intValue++ }
  public fun setWheelPaint(paint: Paint) { wheelPaint = paint; reviseTick.intValue++ }
  public fun setWheelColor(color: Color) { wheelPaint.color = color; reviseTick.intValue++ }
  public fun setWheelAlpha(alpha: Float) { wheelPaint.alpha = alpha; reviseTick.intValue++ }
  public fun setEnabled(enabled: Boolean) { this.enabled.value = enabled }
  public fun setDebounceDuration(duration: Long) { debounceDuration = duration }

  public fun selectByCoordinate(x: Float, y: Float, fromUser: Boolean) {
    enabled.value.takeIf { it } ?: return
    doSelectByCoordinate(x, y, fromUser)
  }

  public fun selectCenter(fromUser: Boolean) {
    val size = canvasSize.value
    selectByCoordinate(size.width * 0.5f, size.height * 0.5f, fromUser)
  }

  internal fun setAlpha(alpha: Float, fromUser: Boolean) {
    this.alpha.value = alpha
    _selectedColor.value = selectedColor.value.copy(alpha = alpha)
    notifyColorChanged(fromUser)
  }

  internal fun setBrightness(brightness: Float, fromUser: Boolean) {
    this.brightness.value = brightness
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(pureSelectedColor.value.toArgb(), hsv)
    hsv[2] = brightness
    _selectedColor.value = Color(android.graphics.Color.HSVToColor((alpha.value * 255).toInt(), hsv))
    if (fromUser && debounceDuration != 0L) notifyColorChangedWithDebounce(fromUser) else notifyColorChanged(fromUser)
  }

  internal fun releaseBitmap() {
    paletteBitmap?.asAndroidBitmap()?.recycle()
    wheelBitmap?.asAndroidBitmap()?.recycle()
    paletteBitmap = null
    wheelBitmap = null
  }
}
