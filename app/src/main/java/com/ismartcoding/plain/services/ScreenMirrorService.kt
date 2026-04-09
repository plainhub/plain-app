package com.ismartcoding.plain.services

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.ServiceInfo
import android.view.OrientationEventListener
import androidx.core.app.ServiceCompat
import androidx.lifecycle.LifecycleService
import android.media.projection.MediaProjection
import com.ismartcoding.lib.channel.sendEvent
import com.ismartcoding.lib.extensions.isPortrait
import com.ismartcoding.lib.extensions.parcelable
import com.ismartcoding.lib.logcat.LogCat
import com.ismartcoding.plain.Constants
import com.ismartcoding.plain.R
import com.ismartcoding.plain.data.DScreenMirrorQuality
import com.ismartcoding.plain.events.EventType
import com.ismartcoding.plain.events.WebSocketEvent
import com.ismartcoding.plain.helpers.NotificationHelper
import com.ismartcoding.plain.mediaProjectionManager
import com.ismartcoding.plain.services.webrtc.ScreenMirrorWebRtcManager
import com.ismartcoding.plain.web.websocket.WebRtcSignalingMessage

class ScreenMirrorService : LifecycleService() {

    private var orientationEventListener: OrientationEventListener? = null
    private var isPortrait = true
    private var notificationId: Int = 0

    private lateinit var webRtcManager: ScreenMirrorWebRtcManager

    @Volatile
    private var running = false

    @SuppressLint("InlinedApi")
    override fun onCreate() {
        super.onCreate()
        instance = this
        webRtcManager = ScreenMirrorWebRtcManager(
            context = this,
            getQuality = { qualityData },
            getIsPortrait = { isPortrait },
        )
        NotificationHelper.ensureDefaultChannel()
        isPortrait = isPortrait()
        orientationEventListener =
            object : OrientationEventListener(this) {
                override fun onOrientationChanged(orientation: Int) {
                    val newIsPortrait = isPortrait()
                    if (isPortrait != newIsPortrait) {
                        isPortrait = newIsPortrait
                        webRtcManager.onOrientationChanged()
                    }
                }
            }
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
                getString(R.string.screen_mirror_service_is_running),
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

        val captureStarted = webRtcManager.initCapture(mMediaProjection)
        if (!captureStarted) {
            LogCat.e("screen mirror: initCapture failed (VirtualDisplay could not be created), stopping service")
            stop()
            return START_NOT_STICKY
        }
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
        webRtcManager.releaseAll()
        orientationEventListener?.disable()
        instance = null
    }

    fun isRunning(): Boolean = running

    fun handleWebRtcSignaling(clientId: String, message: WebRtcSignalingMessage) {
        webRtcManager.handleSignaling(clientId, message)
    }

    fun onQualityChanged() {
        webRtcManager.onQualityChanged()
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
