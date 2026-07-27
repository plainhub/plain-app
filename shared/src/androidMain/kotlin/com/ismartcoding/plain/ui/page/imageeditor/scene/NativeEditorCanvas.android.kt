package com.ismartcoding.plain.ui.page.imageeditor.scene

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.SurfaceTexture
import android.os.Handler
import android.os.HandlerThread
import android.view.TextureView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

@Composable
actual fun NativeEditorCanvas(
    state: SceneState,
    modifier: Modifier,
    onReady: () -> Unit,
) {
    val context = LocalContext.current
    val renderer = remember { SceneRenderer(context) { onReady() } }

    LaunchedEffect(state.sourceImagePath) {
        renderer.setSourcePath(state.sourceImagePath)
    }

    LaunchedEffect(state) {
        renderer.setState(state)
    }

    DisposableEffect(Unit) {
        onDispose { renderer.release() }
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            TextureView(ctx).apply {
                isOpaque = false
                renderer.attach(this)
            }
        },
        update = { _ ->
            renderer.requestRender()
        },
    )
}

internal class SceneRenderer(
    private val context: android.content.Context,
    private val onReady: () -> Unit,
) {
    private val rasterizer = LayerRasterizer()
    private val stateRef = AtomicReference<SceneState?>(null)
    private val sourceBitmapRef = AtomicReference<Bitmap?>(null)
    private val renderRequested = AtomicBoolean(false)
    private val readyFlag = AtomicBoolean(false)
    private var textureView: TextureView? = null

    private val renderThread = HandlerThread("ImageEditor-Render").apply { start() }
    private val renderHandler = Handler(renderThread.looper)

    private val renderRunnable = Runnable { renderFrame() }

    fun attach(view: TextureView) {
        textureView = view
        view.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
                if (readyFlag.compareAndSet(false, true)) onReady()
                requestRender()
            }

            override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
                requestRender()
            }

            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean = true

            override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}
        }
    }

    fun setState(state: SceneState) {
        stateRef.set(state)
        requestRender()
    }

    fun setSourcePath(path: String?) {
        sourceBitmapRef.getAndSet(null)?.recycle()
        if (path.isNullOrEmpty()) {
            requestRender()
            return
        }
        renderHandler.post {
            runCatching {
                val opts = BitmapFactory.Options().apply { inPreferredConfig = Bitmap.Config.ARGB_8888 }
                val bmp = if (path.startsWith("/")) {
                    BitmapFactory.decodeFile(path, opts)
                } else {
                    context.openFileInput(path).use { BitmapFactory.decodeStream(it, null, opts) }
                }
                sourceBitmapRef.set(bmp)
                requestRender()
            }
        }
    }

    fun requestRender() {
        if (textureView?.isAvailable != true) return
        if (renderRequested.compareAndSet(false, true)) {
            renderHandler.removeCallbacks(renderRunnable)
            renderHandler.post(renderRunnable)
        }
    }

    private fun renderFrame() {
        renderRequested.set(false)
        val tv = textureView ?: return
        if (!tv.isAvailable) return
        val state = stateRef.get() ?: return
        val canvas = try {
            tv.lockCanvas()
        } catch (e: Throwable) {
            return
        } ?: return
        try {
            drawScene(canvas, state, sourceBitmapRef.get())
        } finally {
            runCatching { tv.unlockCanvasAndPost(canvas) }
        }
    }

    private fun drawScene(
        canvas: Canvas,
        state: SceneState,
        sourceBitmap: Bitmap?,
    ) {
        val viewW = canvas.width.toFloat()
        val viewH = canvas.height.toFloat()
        val bgPaint = Paint()
        bgPaint.color = Color.parseColor("#1E1E1E")
        bgPaint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, viewW, viewH, bgPaint)

        val cw = state.canvasSize.width.toFloat()
        val ch = state.canvasSize.height.toFloat()
        if (cw <= 0f || ch <= 0f) return

        val viewport = state.viewport
        val worldMatrix = Matrix().apply {
            setTranslate(viewport.panX + viewW / 2f - cw * viewport.scale / 2f, viewport.panY + viewH / 2f - ch * viewport.scale / 2f)
            postScale(viewport.scale, viewport.scale)
        }

        canvas.save()
        canvas.concat(worldMatrix)

        drawCheckerboardOnCanvas(canvas, cw, ch)

        val bgColor = state.bgColor
        if (!bgColor.isNullOrBlank() && !bgColor.equals("transparent", ignoreCase = true)) {
            val bgPaint2 = Paint(Paint.ANTI_ALIAS_FLAG)
            if (isGradientBg(bgColor)) {
                val shader = makeGradientShader(bgColor, cw, ch)
                if (shader != null) {
                    bgPaint2.shader = shader
                    canvas.drawRect(0f, 0f, cw, ch, bgPaint2)
                }
            } else {
                bgPaint2.color = parseSceneBgColor(bgColor)
                canvas.drawRect(0f, 0f, cw, ch, bgPaint2)
            }
        }

        if (sourceBitmap != null) {
            val imgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                alpha = ((state.imgAlpha.coerceIn(0.0, 100.0) / 100.0) * 255).toInt()
            }
            val src = RectF(0f, 0f, sourceBitmap.width.toFloat(), sourceBitmap.height.toFloat())
            val dst = RectF(
                state.imgOffsetX.toFloat(),
                state.imgOffsetY.toFloat(),
                state.imgOffsetX.toFloat() + cw,
                state.imgOffsetY.toFloat() + ch,
            )
            canvas.drawBitmap(sourceBitmap, null, dst, imgPaint)
        }

        val hideId = state.hideLayerId
        for (layer in state.layers) {
            if (!layer.visible) continue
            if (layer.id == hideId) continue
            val entry = rasterizer.getOrRasterize(layer, state.images, sourceBitmap) ?: continue
            if (entry.bitmap != null) {
                canvas.drawBitmap(entry.bitmap, entry.offsetX, entry.offsetY, null)
            } else {
                canvas.save()
                canvas.translate(entry.offsetX, entry.offsetY)
                canvas.drawPicture(entry.picture)
                canvas.restore()
            }
        }

        canvas.restore()
    }

    fun release() {
        renderHandler.removeCallbacks(renderRunnable)
        renderThread.quitSafely()
        rasterizer.clear()
        sourceBitmapRef.getAndSet(null)?.recycle()
        textureView = null
    }
}
