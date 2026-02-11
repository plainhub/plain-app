package com.ismartcoding.plain.services

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.ServiceInfo
import android.view.OrientationEventListener
import androidx.core.app.ServiceCompat
import androidx.lifecycle.LifecycleService
import com.ismartcoding.lib.channel.sendEvent
import com.ismartcoding.lib.extensions.isPortrait
import com.ismartcoding.lib.extensions.parcelable
import com.ismartcoding.lib.logcat.LogCat
import com.ismartcoding.plain.BuildConfig
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
        var resultCode = 0
        var resultData: Intent? = null

        if (intent != null) {
            resultCode = intent.getIntExtra("code", -1)
            resultData = intent.parcelable("data")
        }

        // Must start FGS with mediaProjection type BEFORE calling getMediaProjection() on Android 14+
        if (notificationId == 0) {
            notificationId = NotificationHelper.generateId()
        }
        val notification =
            NotificationHelper.createServiceNotification(
                this,
                "${BuildConfig.APPLICATION_ID}.action.stop_screen_mirror",
                getString(R.string.screen_mirror_service_is_running),
            )
        ServiceCompat.startForeground(
            this,
            notificationId,
            notification,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION,
        )

        val mMediaProjection = if (resultCode == -1 && resultData != null) {
            mediaProjectionManager.getMediaProjection(resultCode, resultData)
        } else {
            null
        }

        orientationEventListener?.enable()
        running = true

        mMediaProjection?.let {
            webRtcManager.initCapture(it)
            sendEvent(
                WebSocketEvent(
                    EventType.SCREEN_MIRRORING,
                    ""
                ),
            )
        }

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
        if (message.type == "control") {
            LogCat.d("webrtc: control message received")
            return
        }

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
