package com.ismartcoding.plain.services

import com.ismartcoding.plain.features.locale.LocaleHelper
import com.ismartcoding.plain.i18n.*

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.ServiceInfo
import android.hardware.display.DisplayManager
import android.view.Display
import android.view.Surface
import android.view.OrientationEventListener
import androidx.core.app.ServiceCompat
import androidx.lifecycle.LifecycleService
import android.media.projection.MediaProjection
import com.ismartcoding.plain.lib.channel.sendEvent
import com.ismartcoding.plain.lib.extensions.parcelable
import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.Constants
import com.ismartcoding.plain.data.DScreenMirrorQuality
import com.ismartcoding.plain.events.EventType
import com.ismartcoding.plain.events.WebSocketEvent
import com.ismartcoding.plain.helpers.NotificationHelper
import com.ismartcoding.plain.mediaProjectionManager
import com.ismartcoding.plain.services.screenmirror.ScreenMirrorPipeline

class ScreenMirrorService : LifecycleService() {

    private var orientationEventListener: OrientationEventListener? = null
    private var isPortrait = true
    private var notificationId: Int = 0

    private var pipeline: ScreenMirrorPipeline? = null

    @Volatile
    private var running = false

    @SuppressLint("InlinedApi")
    override fun onCreate() {
        super.onCreate()
        instance = this
        NotificationHelper.ensureDefaultChannel()
        isPortrait = currentDisplayIsPortrait()
        orientationEventListener =
            object : OrientationEventListener(this) {
                override fun onOrientationChanged(orientation: Int) {
                    val newIsPortrait = currentDisplayIsPortrait()
                    LogCat.d("screen mirror: sensor=$orientation newIsPortrait=$newIsPortrait (was $isPortrait)")
                    if (isPortrait != newIsPortrait) {
                        isPortrait = newIsPortrait
                        PlainAccessibilityService.invalidateScreenSizeCache()
                        pipeline?.onOrientationChanged()
                    }
                }
            }
    }

    private fun currentDisplayIsPortrait(): Boolean {
        val rotation = (getSystemService(DisplayManager::class.java))
            ?.getDisplay(Display.DEFAULT_DISPLAY)?.rotation ?: Surface.ROTATION_0
        return rotation == Surface.ROTATION_0 || rotation == Surface.ROTATION_180
    }

    @SuppressLint("WrongConstant")
    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        super.onStartCommand(intent, flags, startId)

        val resultCode = intent?.getIntExtra("code", -1) ?: -1
        val resultData: Intent? = intent?.parcelable("data")

        if (notificationId == 0) {
            notificationId = NotificationHelper.generateId()
        }
        val notification =
            NotificationHelper.createServiceNotification(
                this,
                Constants.ACTION_STOP_SCREEN_MIRROR,
                LocaleHelper.getString(Res.string.screen_mirror_service_is_running),
            )

        // On AOSP/Pixel: the consent dialog already sets the project_media AppOp, so
        // startForeground succeeds immediately and we call getMediaProjection() after.
        // On Android 16 OEM devices (Honor/Oppo/Samsung/Xiaomi): the consent dialog does NOT set
        // the AppOp, so startForeground throws SecurityException. We recover by calling
        // getMediaProjection() first (which sets the AppOp) and then retrying startForeground.
        var mMediaProjection: MediaProjection? = null
        try {
            ServiceCompat.startForeground(this, notificationId, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION)
        } catch (se: SecurityException) {
            LogCat.e("screen mirror: startForeground failed (OEM AppOp fix): ${se.message}")
            if (resultCode == -1 && resultData != null) {
                mMediaProjection = runCatching { mediaProjectionManager.getMediaProjection(resultCode, resultData) }.getOrNull()
            }
            if (mMediaProjection == null) { stop(); return START_NOT_STICKY }
            try {
                ServiceCompat.startForeground(this, notificationId, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION)
            } catch (se2: SecurityException) {
                LogCat.e("screen mirror: startForeground still failed after AppOp fix: ${se2.message}")
                stop()
                return START_NOT_STICKY
            }
        }

        // AOSP/Pixel path: getMediaProjection must be called AFTER startForeground so the system
        // can bind it to the running FGS (making it the "current" projection for createVirtualDisplay).
        if (mMediaProjection == null && resultCode == -1 && resultData != null) {
            mMediaProjection = runCatching { mediaProjectionManager.getMediaProjection(resultCode, resultData) }.getOrElse {
                LogCat.e("screen mirror: getMediaProjection failed: ${it.message}")
                null
            }
        }

        if (mMediaProjection == null) {
            LogCat.e("MediaProjection is null — permission was denied or revoked by OS")
            stop()
            return START_NOT_STICKY
        }

        orientationEventListener?.enable()
        running = true

        val p = ScreenMirrorPipeline(
            context = this,
            projection = mMediaProjection,
            quality = qualityData,
            getIsPortrait = { isPortrait },
        )
        try {
            p.start()
        } catch (e: Exception) {
            LogCat.e("screen mirror: pipeline start failed: ${e.message}", e)
            stop()
            return START_NOT_STICKY
        }
        pipeline = p
        sendEvent(
            WebSocketEvent(
                EventType.SCREEN_MIRRORING,
                ""
            ),
        )

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        running = false
        pipeline?.stop()
        pipeline = null
        orientationEventListener?.disable()
        instance = null
    }

    fun isRunning(): Boolean = running

    fun getPipeline(): ScreenMirrorPipeline? = pipeline

    fun onQualityChanged() {
        pipeline?.setQuality(qualityData)
    }

    fun stop() {
        running = false
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    companion object {
        @Volatile
        var instance: ScreenMirrorService? = null
        var qualityData = DScreenMirrorQuality()
    }
}
