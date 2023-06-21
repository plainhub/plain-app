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
import android.util.Base64
import androidx.core.graphics.scale
import androidx.lifecycle.LifecycleService
import com.ismartcoding.lib.channel.sendEvent
import com.ismartcoding.lib.extensions.parcelable
import com.ismartcoding.lib.isQPlus
import com.ismartcoding.plain.R
import com.ismartcoding.plain.helpers.NotificationHelper
import com.ismartcoding.plain.mediaProjectionManager
import com.ismartcoding.plain.web.websocket.EventType
import com.ismartcoding.plain.web.websocket.WebSocketEvent
import java.io.ByteArrayOutputStream
import java.util.*

class ScreenMirrorService : LifecycleService() {
    private var mScreenWidth = 0
    private var mScreenHeight = 0
    private var mScreenDensity = 0
    private var mResultCode = 0
    private var mResultData: Intent? = null
    var mBitmap: Bitmap? = null

    private var mMediaProjection: MediaProjection? = null
    private var mImageReader: ImageReader? = null
    private var mImageReaderHandlerThread: HandlerThread? = null
    private var mVirtualDisplay: VirtualDisplay? = null

    override fun onCreate() {
        super.onCreate()
        instance = this
        NotificationHelper.ensureDefaultChannel()
    }

    @SuppressLint("WrongConstant")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        val notification = NotificationHelper.createServiceNotification(
            this,
            "com.ismartcoding.plain.action.stop_screen_mirror",
            getString(R.string.screen_mirror_service_is_running)
        )
        if (isQPlus()) {
            startForeground(3, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION)
        } else {
            startForeground(3, notification)
        }
        if (intent != null) {
            mResultCode = intent.getIntExtra("code", -1)
            mResultData = intent.parcelable("data")
            mScreenWidth = intent.getIntExtra("width", 720)
            mScreenHeight = intent.getIntExtra("height", 1280)
            mScreenDensity = intent.getIntExtra("density", 1)
        }
        mMediaProjection = mediaProjectionManager.getMediaProjection(mResultCode, mResultData!!)
        mImageReader = ImageReader.newInstance(mScreenWidth, mScreenHeight, PixelFormat.RGBA_8888, 2)
        mVirtualDisplay = mMediaProjection?.createVirtualDisplay(
            "ScreenMirroringService", mScreenWidth, mScreenHeight, mScreenDensity,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY or DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC, mImageReader!!.surface, null, null
        )
        mImageReaderHandlerThread = HandlerThread("ImageReader")
        mImageReaderHandlerThread?.start()
        val handler = Handler(mImageReaderHandlerThread!!.looper)
        mImageReader?.setOnImageAvailableListener({
            val image = mImageReader?.acquireLatestImage()
            if (image != null) {
                val planes = image.planes
                val pixelStride = planes[0].pixelStride
                val buffer = planes[0].buffer
                val rowStride = planes[0].rowStride
                val rowPadding = rowStride - pixelStride * mScreenWidth
                mBitmap = Bitmap.createBitmap(mScreenWidth + rowPadding / pixelStride, mScreenHeight, Bitmap.Config.ARGB_8888)
                mBitmap?.copyPixelsFromBuffer(buffer)
                if(mBitmap != null) {
                    sendEvent(WebSocketEvent(EventType.SCREEN_MIRRORING, bitmapToBase64Image(mBitmap!!), false))
                }
                image.close()
            }
        }, handler)
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        if (mVirtualDisplay != null) {
            mVirtualDisplay?.release()
            mVirtualDisplay = null
        }
        mImageReader?.setOnImageAvailableListener(null, null)
        mImageReaderHandlerThread?.quitSafely()
        if (mMediaProjection != null) {
            mMediaProjection?.stop()
            mMediaProjection = null
        }
        mBitmap = null
    }
    private fun bitmapToBase64Image(bitmap: Bitmap): String {
        val maxSize = 100 * 1024 // 100KB
        val maxWidth = 500
        var newBitmap = bitmap
        if (mScreenWidth > maxWidth) {
            val scaleRatio = maxWidth.toFloat() / mScreenWidth.toFloat()
            val newWidth = (mScreenWidth * scaleRatio).toInt()
            val newHeight = (mScreenHeight * scaleRatio).toInt()
            newBitmap = bitmap.scale(newWidth, newHeight, true)
        }

        val outputStream = ByteArrayOutputStream()
        var quality = 40
        newBitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)

        while (outputStream.toByteArray().size > maxSize && quality > 0) {
            outputStream.reset()
            quality -= 5
            newBitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
        }

        val byteArray = outputStream.toByteArray()
        val base64String = Base64.encodeToString(byteArray, Base64.NO_WRAP)
        return "data:image/jpeg;base64,$base64String"
    }

    fun stop() {
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    fun getLatestImageBase64(): String {
        if (mBitmap == null) {
            return ""
        }

        val outputStream = ByteArrayOutputStream()
        mBitmap!!.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
        val byteArray = outputStream.toByteArray()
        val base64String = Base64.encodeToString(byteArray, Base64.NO_WRAP)
        return "data:image/jpeg;base64,$base64String"
    }

    companion object {
        var instance: ScreenMirrorService? = null
    }
}