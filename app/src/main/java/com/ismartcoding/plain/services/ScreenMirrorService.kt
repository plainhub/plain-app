package com.ismartcoding.plain.services

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.os.Handler
import android.os.HandlerThread
import android.view.OrientationEventListener
import androidx.core.app.ServiceCompat
import androidx.core.graphics.scale
import androidx.lifecycle.LifecycleService
import com.ismartcoding.lib.channel.sendEvent
import com.ismartcoding.lib.extensions.isPortrait
import com.ismartcoding.lib.extensions.parcelable
import com.ismartcoding.lib.isRPlus
import com.ismartcoding.lib.logcat.LogCat
import com.ismartcoding.plain.BuildConfig
import com.ismartcoding.plain.R
import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.data.ScreenMirrorQuality
import com.ismartcoding.plain.helpers.NotificationHelper
import com.ismartcoding.plain.mediaProjectionManager
import com.ismartcoding.plain.web.websocket.EventType
import com.ismartcoding.plain.web.websocket.WebSocketEvent
import java.io.ByteArrayOutputStream

class ScreenMirrorService : LifecycleService() {
    private var widthPortrait = 720
    private var heightPortrait = 1280
    private var widthLandscape = 1280
    private var heightLandscape = 720
    private var mScreenDensity = 0
    private var mResultCode = 0
    private var mResultData: Intent? = null
    private var mBitmap: Bitmap? = null
    private lateinit var orientationEventListener: OrientationEventListener
    private var isPortrait = true

    private var mMediaProjection: MediaProjection? = null
    private var mImageReaderPortrait: ImageReader? = null
    private var mImageReaderLanscape: ImageReader? = null
    private var mImageReaderHandlerThread: HandlerThread? = null
    private var mVirtualDisplay: VirtualDisplay? = null
    private var handler: Handler? = null

    @SuppressLint("InlinedApi")
    override fun onCreate() {
        super.onCreate()
        instance = this
        NotificationHelper.ensureDefaultChannel()
        isPortrait = isPortrait()
        val metrics = resources.displayMetrics
        if (isPortrait) {
            widthPortrait = metrics.widthPixels
            heightPortrait = metrics.heightPixels
        } else {
            widthLandscape = metrics.heightPixels
            heightLandscape = metrics.widthPixels
        }
        mScreenDensity = metrics.densityDpi
        orientationEventListener =
            object : OrientationEventListener(this) {
                override fun onOrientationChanged(orientation: Int) {
                    val newIsPortrait = isPortrait()
                    if (isPortrait != newIsPortrait) {
                        isPortrait = newIsPortrait
                        resize()
                    }
                }
            }
        val notification =
            NotificationHelper.createServiceNotification(
                this,
                "${BuildConfig.APPLICATION_ID}.action.stop_screen_mirror",
                getString(R.string.screen_mirror_service_is_running),
            )
        val id = NotificationHelper.generateId()
        ServiceCompat.startForeground(this, id, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION)
    }

    @SuppressLint("WrongConstant")
    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        super.onStartCommand(intent, flags, startId)
        if (intent != null) {
            mResultCode = intent.getIntExtra("code", -1)
            mResultData = intent.parcelable("data")
        }

        mImageReaderHandlerThread = HandlerThread("ImageReader")
        mImageReaderHandlerThread?.start()
        handler = Handler(mImageReaderHandlerThread!!.looper)
        orientationEventListener.enable()
        doMirror()

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        release()
        mImageReaderHandlerThread?.quitSafely()
        mBitmap = null
        orientationEventListener.disable()
    }

    private fun bitmapToByteArray(
        bitmap: Bitmap,
        width: Int,
        height: Int,
    ): ByteArray {
        val qualityData = TempData.screenMirrorQualityType.getQuality()
        val maxWidth = qualityData.maxWidth
        val needCompress =
            if (isPortrait) {
                width > maxWidth
            } else {
                height > maxWidth
            }

        var newBitmap = bitmap
        if (needCompress) {
            val scaleRatio = if (isPortrait) maxWidth.toFloat() / width.toFloat() else maxWidth.toFloat() / height.toFloat()
            val newWidth = (width * scaleRatio).toInt()
            val newHeight = (height * scaleRatio).toInt()
            newBitmap = bitmap.scale(newWidth, newHeight, true)
        }

        val outputStream = ByteArrayOutputStream()
        var quality = qualityData.quality
        if (isRPlus()) {
            newBitmap.compress(Bitmap.CompressFormat.WEBP_LOSSY, quality, outputStream)
        } else {
            newBitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
        }
        while (outputStream.size() > qualityData.maxSize && quality > 0) {
            outputStream.reset()
            quality -= 10
            if (isRPlus()) {
                newBitmap.compress(Bitmap.CompressFormat.WEBP_LOSSY, quality, outputStream)
            } else {
                newBitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
            }
            LogCat.d("quality: $quality, size: ${outputStream.size()}")
        }

        return outputStream.toByteArray()
    }

    private fun resize() {
        val width =
            if (isPortrait) {
                widthPortrait
            } else {
                widthLandscape
            }
        val height =
            if (isPortrait) {
                heightPortrait
            } else {
                heightLandscape
            }

        mVirtualDisplay?.surface = if (isPortrait) mImageReaderPortrait!!.surface else mImageReaderLanscape!!.surface
        mVirtualDisplay?.resize(width, height, mScreenDensity)
    }

    private fun doMirror() {
        mMediaProjection = mediaProjectionManager.getMediaProjection(mResultCode, mResultData!!)
        val width =
            if (isPortrait) {
                widthPortrait
            } else {
                widthLandscape
            }
        val height =
            if (isPortrait) {
                heightPortrait
            } else {
                heightLandscape
            }
        mImageReaderPortrait = ImageReader.newInstance(widthPortrait, heightPortrait, PixelFormat.RGBA_8888, 2)
        mImageReaderLanscape = ImageReader.newInstance(widthLandscape, heightLandscape, PixelFormat.RGBA_8888, 2)
        mMediaProjection?.registerCallback(
            object : MediaProjection.Callback() {
                override fun onStop() {
                }
            },
            null,
        )
        mVirtualDisplay =
            mMediaProjection?.createVirtualDisplay(
                "ScreenMirroringService", width, height, mScreenDensity,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY or DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC,
                if (isPortrait) mImageReaderPortrait!!.surface else mImageReaderLanscape!!.surface,
                object : VirtualDisplay.Callback() {
                },
                null,
            )

        mImageReaderPortrait?.setOnImageAvailableListener({
            try {
                val image = it.acquireLatestImage()
                if (image != null) {
                    val planes = image.planes
                    val pixelStride = planes[0].pixelStride
                    val buffer = planes[0].buffer
                    val rowStride = planes[0].rowStride
                    val newWidth = rowStride / pixelStride

                    mBitmap = Bitmap.createBitmap(newWidth, heightPortrait, Bitmap.Config.ARGB_8888)
                    mBitmap?.copyPixelsFromBuffer(buffer)
                    if (mBitmap != null && instance != null && isPortrait) {
                        sendEvent(
                            WebSocketEvent(EventType.SCREEN_MIRRORING, bitmapToByteArray(mBitmap!!, newWidth, heightPortrait)),
                        )
                    }
                    image.close()
                }
            } catch (ex: Exception) {
                LogCat.e(ex)
            }
        }, handler!!)

        mImageReaderLanscape?.setOnImageAvailableListener({
            try {
                val image = it.acquireLatestImage()
                if (image != null) {
                    val planes = image.planes
                    val pixelStride = planes[0].pixelStride
                    val buffer = planes[0].buffer
                    val rowStride = planes[0].rowStride
                    val newWidth = rowStride / pixelStride

                    mBitmap = Bitmap.createBitmap(newWidth, heightLandscape, Bitmap.Config.ARGB_8888)
                    mBitmap?.copyPixelsFromBuffer(buffer)
                    if (mBitmap != null && instance != null && !isPortrait) {
                        sendEvent(
                            WebSocketEvent(EventType.SCREEN_MIRRORING, bitmapToByteArray(mBitmap!!, newWidth, heightLandscape)),
                        )
                    }
                    image.close()
                }
            } catch (ex: Exception) {
                LogCat.e(ex)
            }
        }, handler!!)
    }

    private fun release() {
        if (mVirtualDisplay != null) {
            mVirtualDisplay?.release()
            mVirtualDisplay = null
        }
        mImageReaderPortrait?.setOnImageAvailableListener(null, null)
        mImageReaderLanscape?.setOnImageAvailableListener(null, null)
        mImageReaderPortrait = null
        mImageReaderLanscape = null
        if (mMediaProjection != null) {
            mMediaProjection?.stop()
            mMediaProjection = null
        }
    }

    fun stop() {
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    fun getLatestImage(): ByteArray? {
        if (mBitmap == null) {
            return null
        }

        val outputStream = ByteArrayOutputStream()
        val quality = ScreenMirrorQuality.HIGH.quality
        if (isRPlus()) {
            mBitmap!!.compress(Bitmap.CompressFormat.WEBP_LOSSY, quality, outputStream)
        } else {
            mBitmap!!.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
        }
        return outputStream.toByteArray()
    }

    companion object {
        var instance: ScreenMirrorService? = null
    }
}
