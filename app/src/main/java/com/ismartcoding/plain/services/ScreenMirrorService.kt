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
import com.ismartcoding.lib.extensions.compress
import com.ismartcoding.lib.extensions.isPortrait
import com.ismartcoding.lib.extensions.parcelable
import com.ismartcoding.lib.logcat.LogCat
import com.ismartcoding.plain.BuildConfig
import com.ismartcoding.plain.R
import com.ismartcoding.plain.data.DScreenMirrorQuality
import com.ismartcoding.plain.helpers.NotificationHelper
import com.ismartcoding.plain.mediaProjectionManager
import com.ismartcoding.plain.events.EventType
import com.ismartcoding.plain.events.WebSocketEvent
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
    private var mImageReaderLandscape: ImageReader? = null
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
        val maxWidth = qualityData.resolution
        var newWidth = width
        var newHeight = height
        val longSide = maxOf(width, height)
        val shortSide = minOf(width, height)
        val scale = shortSide.toFloat() / longSide.toFloat()

        if (shortSide < maxWidth || longSide < maxWidth) {
        } else {
            if (width < height) {
                newWidth = maxWidth
                newHeight = (maxWidth / scale).toInt()
            } else {
                newWidth = (maxWidth / scale).toInt()
                newHeight = maxWidth
            }
        }

        val newBitmap = if (newWidth >= width && newHeight >= height) bitmap else bitmap.scale(newWidth, newHeight, true)

        val outputStream = ByteArrayOutputStream()
        val quality = qualityData.quality
        newBitmap.compress(quality, outputStream)
        val size = outputStream.size()
//        while (size > qualityData.maxSize && quality > 20) {
//            outputStream.reset()
//            quality -= 10
//            newBitmap.compress(quality, outputStream)
//            size = outputStream.size()
//        }
        LogCat.d("quality: $quality, size: $size, $newWidth x $newHeight")

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

        mVirtualDisplay?.surface = if (isPortrait) mImageReaderPortrait!!.surface else mImageReaderLandscape!!.surface
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
        mImageReaderLandscape = ImageReader.newInstance(widthLandscape, heightLandscape, PixelFormat.RGBA_8888, 2)
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
                if (isPortrait) mImageReaderPortrait!!.surface else mImageReaderLandscape!!.surface,
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

        mImageReaderLandscape?.setOnImageAvailableListener({
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
        mImageReaderLandscape?.setOnImageAvailableListener(null, null)
        mImageReaderPortrait = null
        mImageReaderLandscape = null
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
        mBitmap!!.compress(qualityData.quality, outputStream)
        return outputStream.toByteArray()
    }

    companion object {
        var instance: ScreenMirrorService? = null
        var qualityData: DScreenMirrorQuality = DScreenMirrorQuality()
    }
}
