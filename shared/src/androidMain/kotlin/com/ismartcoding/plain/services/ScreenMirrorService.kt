package com.ismartcoding.plain.services

import com.ismartcoding.plain.rust.native.RustHostReports
import com.ismartcoding.plain.lib.coIO
import com.ismartcoding.plain.ui.i18n.LocaleHelper
import com.ismartcoding.plain.i18n.*
import android.annotation.SuppressLint
import android.content.res.Configuration
import android.content.Intent
import android.content.pm.ServiceInfo
import androidx.core.app.ServiceCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import android.media.projection.MediaProjection
import com.ismartcoding.plain.lib.extensions.parcelable
import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.app.AppIntents
import com.ismartcoding.plain.ui.platform.notifications.NotificationHelper
import com.ismartcoding.plain.mediaProjectionManager
import com.ismartcoding.plain.services.screenmirror.ScreenMirrorOrientationState
import com.ismartcoding.plain.services.screenmirror.ScreenMirrorPipeline
import com.ismartcoding.plain.Prefs
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

class ScreenMirrorService : LifecycleService() {

    private lateinit var orientationState: ScreenMirrorOrientationState
    private var notificationId: Int = 0

    private var pipeline: ScreenMirrorPipeline? = null

    @Volatile
    private var running = false

    @SuppressLint("InlinedApi")
    override fun onCreate() {
        super.onCreate()
        instance = this
        NotificationHelper.ensureDefaultChannel()
        orientationState =
            ScreenMirrorOrientationState(currentConfigurationIsPortrait()) { isPortrait ->
                LogCat.d("screen mirror: configuration changed (portrait=$isPortrait)")
                PlainAccessibilityService.invalidateScreenSizeCache()
                pipeline?.onOrientationChanged()
            }
        lifecycleScope.launch {
            try {
                Prefs.screenMirrorQuality.collect { pipeline?.setQuality(it) }
            } catch (canceled: CancellationException) {
                throw canceled
            } catch (error: Exception) {
                LogCat.e("screen mirror: pipeline quality change failed: ${error.message}", error)
                stop()
            }
        }
    }

    private fun currentConfigurationIsPortrait(): Boolean {
        return when (resources.configuration.orientation) {
            Configuration.ORIENTATION_PORTRAIT -> true
            Configuration.ORIENTATION_LANDSCAPE -> false
            else -> resources.displayMetrics.widthPixels <= resources.displayMetrics.heightPixels
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        orientationState.onConfigurationChanged(newConfig.orientation)
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
                AppIntents.ACTION_STOP_SCREEN_MIRROR,
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

        running = true

        val p = ScreenMirrorPipeline(
            context = this,
            projection = mMediaProjection,
            quality = Prefs.screenMirrorQuality.value,
            getIsPortrait = { orientationState.isPortrait },
        )
        try {
            p.start()
        } catch (e: Exception) {
            LogCat.e("screen mirror: pipeline start failed: ${e.message}", e)
            stop()
            return START_NOT_STICKY
        }
        pipeline = p
        coIO { RustHostReports.screenMirrorState("""{"running":true}""") }

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        running = false
        pipeline?.stop()
        pipeline = null
        instance = null
    }

    fun isRunning(): Boolean = running

    fun getPipeline(): ScreenMirrorPipeline? = pipeline

    fun stop() {
        if (!running) return
        running = false
        coIO { RustHostReports.screenMirrorState("""{"running":false}""") }
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    companion object {
        @Volatile
        var instance: ScreenMirrorService? = null
    }
}
