package com.ismartcoding.plain.ui.base.subsampling

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Resources
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PointF
import android.os.SystemClock
import android.util.Log
import androidx.compose.runtime.RememberObserver
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.unit.IntSize
import com.ismartcoding.lib.logcat.LogCat
import com.ismartcoding.plain.ui.base.subsampling.gestures.GestureAnimationEasing
import com.ismartcoding.plain.ui.base.subsampling.helpers.BackgroundUtils
import com.ismartcoding.plain.ui.base.subsampling.helpers.Try
import com.ismartcoding.plain.ui.base.subsampling.helpers.asLog
import com.ismartcoding.plain.ui.base.subsampling.helpers.errorMessageOrClassName
import com.ismartcoding.plain.ui.base.subsampling.helpers.exceptionOrThrow
import com.ismartcoding.plain.ui.base.subsampling.helpers.power
import com.ismartcoding.plain.ui.base.subsampling.helpers.unwrap
import java.io.InputStream
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.withContext

internal val maximumBitmapSizeState = mutableStateOf<IntSize?>(null)

class ComposeSubsamplingScaleImageState internal constructor(
    val context: Context,
    val maxTileSize: MaxTileSize,
    val minimumScaleType: MinimumScaleType,
    val minScaleParam: Float?,
    val maxScaleParam: Float?,
    val doubleTapZoom: Float?,
    internal val imageDecoderProvider: ImageDecoderProvider,
    internal val decoderDispatcherLazy: Lazy<CoroutineDispatcher>,
    val debug: Boolean,
    val minFlingMoveDistPx: Int,
    val minFlingVelocityPxPerSecond: Int,
    val quickZoomTimeoutMs: Int,
    val animationUpdateIntervalMs: Int,
    val zoomAnimationDurationMs: Int,
    val flingAnimationDurationMs: Int,
    val minDpi: Int,
    val scrollableContainerDirection: ScrollableContainerDirection?,
) : RememberObserver {
    private val decoderDispatcher by decoderDispatcherLazy
    private lateinit var coroutineScope: CoroutineScope

    private val defaultMaxScale = 2f
    private val defaultDoubleTapZoom = 1f

    internal val tileMap = LinkedHashMap<Int, MutableList<Tile>>()

    private val initialMinScale by lazy { calculateMinScale() }
    private val initialMaxScale by lazy { calculateMaxScale(minDpi) }

    private var updatedMinScale: Float? = null
    private var updatedMaxScale: Float? = null

    val minScale: Float
        get() = updatedMinScale ?: initialMinScale
    val maxScale: Float
        get() = updatedMaxScale ?: initialMaxScale

    val doubleTapZoomScale by lazy { calculateDoubleTapZoomScale(doubleTapZoom) }

    private var satTemp = ScaleAndTranslate()
    private var needInitScreenTranslate = true
    private var lastInvalidateTime = 0L
    private var pendingImageSaveableState: ImageSaveableState? = null

    internal var _vTranslate = PointfMut()
    val vTranslate: PointF
        get() = PointF(_vTranslate.x, _vTranslate.y)

    var currentScale = 0f
        internal set

    var debugKey: String? = null
        private set
    var sourceImageDimensions: IntSize? = null
        private set

    internal val bitmapPaint by lazy {
        Paint().apply {
            isAntiAlias = true
            isFilterBitmap = true
            isDither = true
        }
    }
    internal val bitmapMatrix by lazy { Matrix() }
    internal val srcArray = FloatArray(8)
    internal val dstArray = FloatArray(8)

    private val subsamplingImageDecoder = AtomicReference<ComposeSubsamplingScaleImageDecoder?>(null)

    internal val initializationState = mutableStateOf<InitializationState>(InitializationState.Uninitialized)
    internal val fullImageSampleSizeState = mutableStateOf(0)
    internal val availableDimensions = mutableStateOf(IntSize.Zero)

    // Tile are loaded asynchronously.
    // invalidate value is incremented every time we decode a new tile.
    // It's needed to notify the composition to redraw current tileMap.
    private val _invalidate = mutableStateOf(0)
    internal val invalidate: State<Int>
        get() = _invalidate

    // Width of the composable
    val viewWidth: Int
        get() = availableDimensions.value.width

    // Height of the composable
    val viewHeight: Int
        get() = availableDimensions.value.height

    // Width of the source image
    val sWidth: Int
        get() = requireNotNull(sourceImageDimensions?.width) { "sourceImageDimensions is null!" }

    // Height the source image
    val sHeight: Int
        get() = requireNotNull(sourceImageDimensions?.height) { "sourceImageDimensions is null!" }

    val isReady: Boolean
        get() = initializationState.value is InitializationState.Success
    val isReadyForGestures: Boolean
        get() = isReady && viewWidth > 0 && viewHeight > 0 && sourceImageDimensions != null
    val isReadyForInitialization: Boolean
        get() = viewWidth > 0 && viewHeight > 0

    override fun onRemembered() {
        coroutineScope = CoroutineScope(decoderDispatcher)
    }

    override fun onForgotten() {
        reset()
    }

    override fun onAbandoned() {
    }

    fun imageSaveableState(): ImageSaveableState? {
        if (!isReady || sourceImageDimensions == null || viewWidth <= 0 || viewHeight <= 0) {
            return null
        }

        return ImageSaveableState(
            scale = currentScale,
            center = getCenter()
        )
    }

    fun applyImageSaveableState(imageSaveableState: ImageSaveableState) {
        if (pendingImageSaveableState == imageSaveableState) {
            return
        }

        pendingImageSaveableState = imageSaveableState
        requestInvalidate(forced = true)
    }

    fun viewToSourceX(vx: Float): Float {
        return (vx - _vTranslate.x) / currentScale
    }

    fun viewToSourceY(vy: Float): Float {
        return (vy - _vTranslate.y) / currentScale
    }

    fun sourceToViewX(sx: Float): Float {
        return (sx * currentScale) + _vTranslate.x
    }

    fun sourceToViewY(sy: Float): Float {
        return (sy * currentScale) + _vTranslate.y
    }

    fun limitedScale(targetScale: Float): Float {
        var resultScale = targetScale
        resultScale = Math.max(minScale, resultScale)
        resultScale = Math.min(maxScale, resultScale)
        return resultScale
    }

    fun sourceToViewRect(source: RectMut, target: RectMut) {
        target.set(
            sourceToViewX(source.left.toFloat()).toInt(),
            sourceToViewY(source.top.toFloat()).toInt(),
            sourceToViewX(source.right.toFloat()).toInt(),
            sourceToViewY(source.bottom.toFloat()).toInt()
        )
    }

    fun getCenter(): PointF {
        val mX: Int = viewWidth / 2
        val mY: Int = viewHeight / 2
        return viewToSourceCoord(mX.toFloat(), mY.toFloat())
    }

    fun viewToSourceCoord(vx: Float, vy: Float): PointF {
        return viewToSourceCoord(vx, vy, PointF())
    }

    fun viewToSourceCoord(vxy: PointF): PointF {
        return viewToSourceCoord(vxy.x, vxy.y, PointF())
    }

    fun viewToSourceCoord(vx: Float, vy: Float, sTarget: PointF): PointF {
        sTarget.set(viewToSourceX(vx), viewToSourceY(vy))
        return sTarget
    }

    fun sourceToViewCoord(sxy: PointF): PointF {
        return sourceToViewCoord(sxy.x, sxy.y, PointF())
    }

    fun sourceToViewCoord(sx: Float, sy: Float): PointF {
        return sourceToViewCoord(sx, sy, PointF())
    }

    fun sourceToViewCoord(sx: Float, sy: Float, vTarget: PointF): PointF {
        vTarget.set(sourceToViewX(sx), sourceToViewY(sy))
        return vTarget
    }

    fun getPanInfo(
        horizontalTolerance: Float = PanInfo.DEFAULT_TOLERANCE,
        verticalTolerance: Float = PanInfo.DEFAULT_TOLERANCE
    ): PanInfo? {
        if (!isReady) {
            return null
        }

        val scaleWidth: Float = currentScale * sWidth
        val scaleHeight: Float = currentScale * sHeight

        return PanInfo(
            top = Math.max(0f, -_vTranslate.y),
            left = Math.max(0f, -_vTranslate.x),
            bottom = Math.max(0f, scaleHeight + _vTranslate.y - viewHeight),
            right = Math.max(0f, scaleWidth + _vTranslate.x - viewWidth),
            horizontalTolerance = horizontalTolerance,
            verticalTolerance = verticalTolerance
        )
    }

    internal fun requestInvalidate(forced: Boolean = false) {
        if (!forced && (SystemClock.elapsedRealtime() - lastInvalidateTime < animationUpdateIntervalMs)) {
            return
        }

        _invalidate.value = _invalidate.value + 1
        lastInvalidateTime = SystemClock.elapsedRealtime()
    }

    private fun reset() {
        initializationState.value = InitializationState.Uninitialized
        coroutineScope.cancel()

        tileMap.entries.forEach { (_, tiles) -> tiles.forEach { tile -> tile.recycle() } }
        tileMap.clear()

        debugKey = null
        _vTranslate.set(0f, 0f)
        satTemp.reset()
        bitmapMatrix.reset()
        sourceImageDimensions = null
        currentScale = 0f
        fullImageSampleSizeState.value = 0
        availableDimensions.value = IntSize.Zero
        srcArray.fill(0f)
        dstArray.fill(0f)
        subsamplingImageDecoder.getAndSet(null)?.recycle()
        needInitScreenTranslate = true
    }

    private fun calculateDoubleTapZoomScale(doubleTapZoom: Float?): Float {
        if (doubleTapZoom != null && doubleTapZoom > 0f) {
            return doubleTapZoom
        }

        return defaultDoubleTapZoom
    }

    internal suspend fun initialize(
        imageSourceProvider: ImageSourceProvider,
        eventListener: ComposeSubsamplingScaleImageEventListener?
    ): InitializationState {
        BackgroundUtils.ensureMainThread()

        return coroutineScope {
            try {
                if (!isReadyForInitialization) {
                    reset()
                    return@coroutineScope InitializationState.Uninitialized
                }

                if (debug) {
                    LogCat.d("initialize() called")
                }

                if (subsamplingImageDecoder.get() == null) {
                    val decoder = imageDecoderProvider.provide()

                    val success = subsamplingImageDecoder.compareAndSet(null, decoder)
                    if (!success) {
                        val exception = IllegalStateException("Decoder was already initialized!")
                        eventListener?.onFailedToDecodeImageInfo(exception)

                        return@coroutineScope InitializationState.Error(exception)
                    } else {
                        LogCat.d("initialize() using ${decoder.javaClass.simpleName} decoder")
                    }
                }

                val provideSourceResult = withContext(coroutineScope.coroutineContext) {
                    imageSourceProvider.provide()
                }

                ensureActive()

                val imageSource = if (provideSourceResult.isFailure) {
                    val error = provideSourceResult.exceptionOrThrow()

                    LogCat.e(
                        "initialize() imageSourceProvider.provide() Failure!\n" +
                                "sourceDebugKey=${debugKey}\n" +
                                "error=${error.asLog()}"
                    )

                    eventListener?.onFailedToProvideSource(error)
                    reset()
                    return@coroutineScope InitializationState.Error(error)
                } else {
                    provideSourceResult.getOrThrow()
                }

                ensureActive()

                if (debug) {
                    LogCat.d("initialize() got image source")
                }

                val imageDimensionsInfoResult = withContext(coroutineScope.coroutineContext) {
                    this@ComposeSubsamplingScaleImageState.debugKey = imageSource.debugKey

                    return@withContext imageSource
                        .inputStream
                        .use { inputStream -> decodeImageDimensions(inputStream) }
                }

                ensureActive()

                val imageDimensions = if (imageDimensionsInfoResult.isFailure) {
                    val error = imageDimensionsInfoResult.exceptionOrThrow()
                    LogCat.e(
                        "initialize() decodeImageDimensions() Failure!\n" +
                                "sourceDebugKey=${debugKey}\n" +
                                "error=${error.asLog()}"
                    )

                    eventListener?.onFailedToDecodeImageInfo(error)
                    reset()
                    return@coroutineScope InitializationState.Error(error)
                } else {
                    imageDimensionsInfoResult.getOrThrow()
                }

                ensureActive()

                if (!isReadyForInitialization) {
                    reset()
                    return@coroutineScope InitializationState.Uninitialized
                }

                if (debug) {
                    LogCat.d("initialize() got image dimensions: $imageDimensions")
                }

                eventListener?.onImageInfoDecoded(imageDimensions)
                sourceImageDimensions = imageDimensions

                if (debug) {
                    LogCat.d("initialize() decodeImageDimensions() Success! imageDimensions=$imageDimensions")
                }

                val result = reloadTilesAndFitBounds(imageDimensions)
                if (result.isFailure) {
                    if (debug) {
                        LogCat.e("initialize() failure! ${result.exceptionOrThrow().errorMessageOrClassName()}")
                    }

                    return@coroutineScope InitializationState.Error(result.exceptionOrThrow())
                }

                if (debug) {
                    LogCat.d("initialize() success!")
                }

                return@coroutineScope InitializationState.Success
            } catch (error: CancellationException) {
                if (debug) {
                    LogCat.e("initialize() canceled")
                }

                eventListener?.onInitializationCanceled()
                throw error
            }
        }
    }

    private suspend fun reloadTilesAndFitBounds(imageDimensions: IntSize): Result<Unit> {
        satTemp.reset()
        fitToBounds(true, satTemp)

        fullImageSampleSizeState.value = calculateInSampleSize(
            sourceWidth = imageDimensions.width,
            sourceHeight = imageDimensions.height,
            scale = satTemp.scale
        )

        if (fullImageSampleSizeState.value > 1) {
            fullImageSampleSizeState.value /= 2
        }

        tileMap.clear()

        initialiseTileMap(
            sourceWidth = imageDimensions.width,
            sourceHeight = imageDimensions.height,
            maxTileWidth = maxTileSize.width,
            maxTileHeight = maxTileSize.height,
            availableWidth = viewWidth,
            availableHeight = viewHeight,
            fullImageSampleSize = fullImageSampleSizeState.value,
            inTileMap = tileMap
        )

        if (debug) {
            tileMap.entries.forEach { (sampleSize, tiles) ->
                LogCat.d("initialiseTileMap sampleSize=$sampleSize, tilesCount=${tiles.size}")
            }
        }

        val currentSampleSize = fullImageSampleSizeState.value
        val baseGrid = tileMap[currentSampleSize]!!

        val loadTilesResult = loadTiles(
            currentSampleSize = currentSampleSize,
            tilesToLoad = baseGrid,
            eventListener = null
        )

        if (loadTilesResult.isFailure) {
            return loadTilesResult
        }

        fitToBounds(false)

        return refreshRequiredTilesInternal(
            load = true,
            sourceWidth = imageDimensions.width,
            sourceHeight = imageDimensions.height,
            fullImageSampleSize = fullImageSampleSizeState.value,
            scale = currentScale
        )
    }

    private suspend fun decodeImageDimensions(
        inputStream: InputStream
    ): Result<IntSize> {
        BackgroundUtils.ensureBackgroundThread()

        return Result.Try {
            val decoder = subsamplingImageDecoder.get()
                ?: error("Decoder is not initialized!")

            return@Try runInterruptible { decoder.init(context, inputStream).unwrap() }
        }
    }

    private suspend fun loadTiles(
        currentSampleSize: Int,
        tilesToLoad: List<Tile>,
        eventListener: ComposeSubsamplingScaleImageEventListener?
    ): Result<Unit> {
        if (tilesToLoad.isEmpty()) {
            eventListener?.onFullImageLoaded()
            return Result.success(Unit)
        }

        if (debug) {
            LogCat.d("tilesToLoadCount=${tilesToLoad.size}")
        }

        val decoder = subsamplingImageDecoder.get()
        if (decoder == null) {
            val exception = IllegalStateException("Decoder is not initialized!")
            eventListener?.onFailedToLoadFullImage(exception)
            return Result.failure(exception)
        }

        val totalTilesCount = tilesToLoad.size
        val remaining = AtomicInteger(totalTilesCount)

        coroutineScope {
            val totalCount = tilesToLoad.size

            tilesToLoad.forEachIndexed { index, tile ->
                coroutineScope.launch {
                    BackgroundUtils.ensureBackgroundThread()
                    val threadName = Thread.currentThread().name

                    try {
                        if (!tile.updateStateAsLoading()) {
                            // Skip already loaded
                            if (debug) {
                                LogCat.d(
                                    "loadTiles($index/$totalCount, @$currentSampleSize) " +
                                            "skipping already loaded tile at ${tile.xy}"
                                )
                            }

                            return@launch
                        }

                        if (debug) {
                            LogCat.d(
                                "loadTiles($index/$totalCount, @$currentSampleSize, ${threadName}) " +
                                        "decoding tile at ${tile.xy}"
                            )
                        }

                        ensureActive()

                        val decodedTileBitmap = runInterruptible {
                            decoder.decodeRegion(
                                sRect = tile.fileSourceRect.toAndroidRect(),
                                sampleSize = tile.sampleSize
                            ).unwrap()
                        }

                        eventListener?.onTileDecoded(index + 1, totalTilesCount)
                        tile.onTileLoaded(decodedTileBitmap)
                    } catch (error: Throwable) {
                        if (debug) {
                            LogCat.e(
                                "loadTiles($index/$totalCount, @$currentSampleSize, ${threadName}) " +
                                        "Failed to decode tile at ${tile.xy}, error: ${error.errorMessageOrClassName()}"
                            )
                        }

                        eventListener?.onFailedToDecodeTile(index + 1, totalTilesCount, error)
                        tile.onTileLoadError(error)

                        // Consume all non CancellationException errors
                        if (error is CancellationException) {
                            throw error
                        }
                    } finally {
                        val allProcessed = remaining.addAndGet(-1) == 0
                        if (allProcessed) {
                            eventListener?.onFullImageLoaded()
                        }

                        // Force redraw the whole image when a new tile is loaded
                        requestInvalidate(forced = true)
                    }
                }
            }
        }

        return Result.success(Unit)
    }

    internal fun refreshRequiredTiles(load: Boolean) {
        BackgroundUtils.ensureMainThread()

        coroutineScope.launch(Dispatchers.Main.immediate) {
            if (!isReady) {
                return@launch
            }

            val refreshTilesResult = refreshRequiredTilesInternal(
                load = load,
                sourceWidth = sWidth,
                sourceHeight = sHeight,
                fullImageSampleSize = fullImageSampleSizeState.value,
                scale = currentScale
            )

            if (refreshTilesResult.isFailure && debug) {
                val errorMessage = refreshTilesResult.exceptionOrThrow().errorMessageOrClassName()
                LogCat.e(
                    "refreshRequiredTilesInternal() error: $errorMessage"
                )
            }
        }
    }

    private suspend fun refreshRequiredTilesInternal(
        load: Boolean,
        sourceWidth: Int,
        sourceHeight: Int,
        fullImageSampleSize: Int,
        scale: Float
    ): Result<Unit> {
        BackgroundUtils.ensureMainThread()

        val currentSampleSize = Math.min(
            fullImageSampleSize,
            calculateInSampleSize(
                sourceWidth = sourceWidth,
                sourceHeight = sourceHeight,
                scale = scale
            )
        )

        val tilesToLoad = mutableListOf<Tile>()

        // Load tiles of the correct sample size that are on screen. Discard tiles off screen,
        // and those that are higher resolution than required, or lower res than required but
        // not the base layer, so the base layer is always present.
        for ((_, tiles) in tileMap) {
            for (tile in tiles) {
                val tileSampleSize = tile.sampleSize

                if (tileSampleSize != currentSampleSize && tileSampleSize != fullImageSampleSize) {
                    tile.visible = false
                    tile.recycle()
                }

                if (tileSampleSize == currentSampleSize) {
                    if (tileVisible(tile)) {
                        tile.visible = true

                        if (load && tile.canLoad) {
                            tilesToLoad += tile
                        }
                    } else if (tileSampleSize != fullImageSampleSize) {
                        tile.visible = false
                        tile.recycle()
                    }
                } else if (tileSampleSize == fullImageSampleSize) {
                    tile.visible = true
                }
            }
        }

        val loadTilesResult = loadTiles(
            currentSampleSize = currentSampleSize,
            tilesToLoad = tilesToLoad,
            eventListener = null
        )

        if (loadTilesResult.isFailure) {
            tilesToLoad.forEach { tile -> tile.onTileLoadError(loadTilesResult.exceptionOrThrow()) }
        }

        return loadTilesResult
    }

    private fun tileVisible(tile: Tile): Boolean {
        val sVisLeft = viewToSourceX(0f)
        val sVisRight = viewToSourceX(viewWidth.toFloat())
        val sVisTop = viewToSourceY(0f)
        val sVisBottom = viewToSourceY(viewHeight.toFloat())

        return !(sVisLeft > tile.sourceRect.right
                || tile.sourceRect.left > sVisRight
                || sVisTop > tile.sourceRect.bottom
                || tile.sourceRect.top > sVisBottom
                )
    }

    internal fun calculateInSampleSize(sourceWidth: Int, sourceHeight: Int, scale: Float): Int {
        val reqWidth = (sourceWidth * scale).toInt()
        val reqHeight = (sourceHeight * scale).toInt()

        var inSampleSize = 1
        if (reqWidth == 0 || reqHeight == 0) {
            return 32
        }

        if (sourceHeight > reqHeight || sourceWidth > reqWidth) {
            val heightRatio = Math.round(sourceHeight.toFloat() / reqHeight.toFloat())
            val widthRatio = Math.round(sourceWidth.toFloat() / reqWidth.toFloat())

            inSampleSize = if (heightRatio < widthRatio) heightRatio else widthRatio
        }

        return inSampleSize.power()
    }

    private fun initialiseTileMap(
        sourceWidth: Int,
        sourceHeight: Int,
        maxTileWidth: Int,
        maxTileHeight: Int,
        availableWidth: Int,
        availableHeight: Int,
        fullImageSampleSize: Int,
        inTileMap: LinkedHashMap<Int, MutableList<Tile>>
    ) {
        if (debug) {
            LogCat.d(
                "initialiseTileMap() sourceWidth=${sourceWidth}, sourceHeight=${sourceHeight}, " +
                        "maxTileWidth=${maxTileWidth}, maxTileHeight=${maxTileHeight}, " +
                        "availableWidth=${availableWidth}, availableHeight=${availableHeight}, " +
                        "fullImageSampleSize=${fullImageSampleSize}"
            )
        }

        var sampleSize = fullImageSampleSize
        var xTiles = 1
        var yTiles = 1

        while (true) {
            var sTileWidth: Int = sourceWidth / xTiles
            var sTileHeight: Int = sourceHeight / yTiles
            var subTileWidth = sTileWidth / sampleSize
            var subTileHeight = sTileHeight / sampleSize

            while (
                subTileWidth + xTiles + 1 > maxTileWidth ||
                (subTileWidth > availableWidth * 1.25 && sampleSize < fullImageSampleSize)
            ) {
                xTiles += 1
                sTileWidth = sourceWidth / xTiles
                subTileWidth = sTileWidth / sampleSize
            }

            while (
                subTileHeight + yTiles + 1 > maxTileHeight ||
                (subTileHeight > availableHeight * 1.25 && sampleSize < fullImageSampleSize)
            ) {
                yTiles += 1
                sTileHeight = sourceHeight / yTiles
                subTileHeight = sTileHeight / sampleSize
            }

            val tileGrid = ArrayList<Tile>(xTiles * yTiles)

            for (x in 0 until xTiles) {
                for (y in 0 until yTiles) {
                    val tile = Tile(x, y)
                    tile.sampleSize = sampleSize
                    tile.visible = sampleSize == fullImageSampleSize

                    tile.sourceRect.set(
                        left = x * sTileWidth,
                        top = y * sTileHeight,
                        right = if (x == xTiles - 1) sourceWidth else (x + 1) * sTileWidth,
                        bottom = if (y == yTiles - 1) sourceHeight else (y + 1) * sTileHeight
                    )

                    tile.screenRect.set(0, 0, 0, 0)
                    tile.fileSourceRect.set(tile.sourceRect)

                    tileGrid.add(tile)
                }
            }

            inTileMap[sampleSize] = tileGrid

            if (sampleSize == 1) {
                break
            }

            sampleSize /= 2
        }

        if (debug) {
            inTileMap.entries.forEach { (sampleSize, tileLayer) ->
                LogCat.d("initialiseTileMap() sampleSize=$sampleSize, tileLayerSize=${tileLayer.size}")
            }
        }
    }

    internal fun fitToBounds(center: Boolean) {
        satTemp.scale = currentScale
        satTemp.vTranslate.set(_vTranslate.x, _vTranslate.y)

        fitToBounds(center, satTemp)

        currentScale = satTemp.scale
        _vTranslate.set(satTemp.vTranslate.x, satTemp.vTranslate.y)

        if (needInitScreenTranslate) {
            needInitScreenTranslate = false

            _vTranslate.set(
                vTranslateForSCenter(
                    sCenterX = (sWidth / 2).toFloat(),
                    sCenterY = (sHeight / 2).toFloat(),
                    scale = currentScale
                )
            )
        }
    }

    internal fun fitToBounds(shouldCenter: Boolean, sat: ScaleAndTranslate) {
        var center = shouldCenter
//    if (panLimit == PAN_LIMIT_OUTSIDE && isReady()) {
//      center = false
//    }

        check(viewWidth > 0) { "Bad availableWidth" }
        check(viewHeight > 0) { "Bad availableHeight" }

        val vTranslate = sat.vTranslate
        val scale: Float = limitedScale(sat.scale)
        val scaleWidth: Float = scale * sWidth
        val scaleHeight: Float = scale * sHeight

        /*if (panLimit == PAN_LIMIT_CENTER && isReady()) {
          vTranslate.x = Math.max(vTranslate.x, availableWidth / 2 - scaleWidth)
          vTranslate.y = Math.max(vTranslate.y, availableHeight / 2 - scaleHeight)
        } else */
        if (center) {
            vTranslate.x = Math.max(vTranslate.x, viewWidth - scaleWidth)
            vTranslate.y = Math.max(vTranslate.y, viewHeight - scaleHeight)
        } else {
            vTranslate.x = Math.max(vTranslate.x, -scaleWidth)
            vTranslate.y = Math.max(vTranslate.y, -scaleHeight)
        }

        // Asymmetric padding adjustments
//    val xPaddingRatio = if (getPaddingLeft() > 0 || getPaddingRight() > 0) {
//      getPaddingLeft() / (getPaddingLeft() + getPaddingRight()) as Float
//    } else {
//      0.5f
//    }
//    val yPaddingRatio = if (getPaddingTop() > 0 || getPaddingBottom() > 0) {
//      getPaddingTop() / (getPaddingTop() + getPaddingBottom()) as Float
//    } else {
//      0.5f
//    }

        val xPaddingRatio = 0.5f
        val yPaddingRatio = 0.5f

        val maxTx: Float
        val maxTy: Float

        /*if (panLimit == PAN_LIMIT_CENTER && isReady()) {
          maxTx = Math.max(0, availableWidth / 2).toFloat()
          maxTy = Math.max(0, availableHeight / 2).toFloat()
        } else */
        if (center) {
            maxTx = Math.max(0f, (viewWidth - scaleWidth) * xPaddingRatio)
            maxTy = Math.max(0f, (viewHeight - scaleHeight) * yPaddingRatio)
        } else {
            maxTx = Math.max(0, viewWidth).toFloat()
            maxTy = Math.max(0, viewHeight).toFloat()
        }
        vTranslate.x = Math.min(vTranslate.x, maxTx)
        vTranslate.y = Math.min(vTranslate.y, maxTy)
        sat.scale = scale
    }

    private fun calculateMaxScale(minDpi: Int): Float {
        if (maxScaleParam != null) {
            return maxScaleParam
        }

        if (minDpi <= 0) {
            return defaultMaxScale
        }

        val metrics = getResources().displayMetrics
        val averageDpi = (metrics.xdpi + metrics.ydpi) / 2
        return averageDpi / minDpi
    }

    internal fun calculateMinScale(): Float {
        // TODO(KurobaEx): paddings
        val hPadding = 0
        val vPadding = 0

        check(viewWidth > 0) { "availableWidth is zero" }
        check(viewHeight > 0) { "availableHeight is zero" }
        check(sWidth > 0) { "sourceWidth is zero" }
        check(sHeight > 0) { "sourceHeight is zero" }

        if (minimumScaleType == MinimumScaleType.ScaleTypeCenterInside) {
            return Math.min(
                (viewWidth - hPadding) / sWidth.toFloat(),
                (viewHeight - vPadding) / sHeight.toFloat()
            )
        } else if (minimumScaleType == MinimumScaleType.ScaleTypeCenterCrop) {
            return Math.max(
                (viewWidth - hPadding) / sWidth.toFloat(),
                (viewHeight - vPadding) / sHeight.toFloat()
            )
        } else if (minimumScaleType == MinimumScaleType.ScaleTypeFitWidth) {
            return (viewWidth - hPadding) / sWidth.toFloat()
        } else if (minimumScaleType == MinimumScaleType.ScaleTypeFitHeight) {
            return (viewHeight - vPadding) / sHeight.toFloat()
        } else if (minimumScaleType == MinimumScaleType.ScaleTypeOriginalSize) {
            return 1f
        } else if (minimumScaleType == MinimumScaleType.ScaleTypeSmartFit) {
            return if (sHeight > sWidth) {
                // Fit to width
                (viewWidth - hPadding) / sWidth.toFloat()
            } else {
                // Fit to height
                (viewHeight - vPadding) / sHeight.toFloat()
            }
        } else if (minimumScaleType is MinimumScaleType.ScaleTypeCustom && (minScaleParam != null && minScaleParam > 0f)) {
            return minScaleParam
        } else {
            return Math.min(
                (viewWidth - hPadding) / sWidth.toFloat(),
                (viewHeight - vPadding) / sHeight.toFloat()
            )
        }
    }

    internal fun limitedSCenter(
        sCenterX: Float,
        sCenterY: Float,
        scale: Float,
        sTarget: PointF
    ): PointF {
        val vTranslate = vTranslateForSCenter(sCenterX, sCenterY, scale)

        val vxCenter: Int = viewWidth / 2
        val vyCenter: Int = viewHeight / 2

        val sx = (vxCenter - vTranslate.x) / scale
        val sy = (vyCenter - vTranslate.y) / scale
        sTarget.set(sx, sy)
        return sTarget
    }

    internal fun vTranslateForSCenter(sCenterX: Float, sCenterY: Float, scale: Float): PointfMut {
        val vxCenter: Int = viewWidth / 2
        val vyCenter: Int = viewHeight / 2

        satTemp.scale = scale
        satTemp.vTranslate.set(vxCenter - sCenterX * scale, vyCenter - sCenterY * scale)

        fitToBounds(true, satTemp)
        return satTemp.vTranslate
    }

    internal fun ease(
        gestureAnimationEasing: GestureAnimationEasing,
        time: Long,
        from: Float,
        change: Float,
        duration: Long
    ): Float {
        return when (gestureAnimationEasing) {
            GestureAnimationEasing.EaseInOutQuad -> easeInOutQuad(time, from, change, duration)
            GestureAnimationEasing.EaseOutQuad -> easeOutQuad(time, from, change, duration)
        }
    }

    private fun easeOutQuad(time: Long, from: Float, change: Float, duration: Long): Float {
        val progress = time.toFloat() / duration.toFloat()
        return -change * progress * (progress - 2) + from
    }

    private fun easeInOutQuad(time: Long, from: Float, change: Float, duration: Long): Float {
        var timeF = time / (duration / 2f)

        if (timeF < 1) {
            return change / 2f * timeF * timeF + from
        }

        timeF--
        return -change / 2f * (timeF * (timeF - 2) - 1) + from
    }

    internal suspend fun onSizeChanged(prevWidth: Int, prevHeight: Int) {
        if (debug) {
            LogCat.d("onSizeChanged() previous: ${prevWidth}x${prevHeight} current: ${viewWidth}x${viewHeight}")
        }

        val imageDimensions = sourceImageDimensions
            ?: return

        updatedMinScale = calculateMinScale()
        updatedMaxScale = calculateMaxScale(minDpi)

        val result = reloadTilesAndFitBounds(imageDimensions)
        if (result.isFailure) {
            LogCat.e("reloadTilesAndFitBounds() error: ${result.exceptionOrThrow().errorMessageOrClassName()}")
        }

        requestInvalidate(forced = true)
    }

    internal fun preDraw() {
        if (!isReady || sourceImageDimensions == null || viewWidth <= 0 || viewHeight <= 0) {
            return
        }

        pendingImageSaveableState?.let { imageSaveableState ->
            val pendingScale = imageSaveableState.scale
            val sPendingCenterX = imageSaveableState.center.x
            val sPendingCenterY = imageSaveableState.center.y

            currentScale = limitedScale(pendingScale)
            _vTranslate.x = (viewWidth / 2) - (currentScale * sPendingCenterX)
            _vTranslate.y = (viewHeight / 2) - (currentScale * sPendingCenterY)

            fitToBounds(true)
            refreshRequiredTiles(true)
        }

        pendingImageSaveableState = null
    }

    private fun getResources(): Resources = context.resources
}