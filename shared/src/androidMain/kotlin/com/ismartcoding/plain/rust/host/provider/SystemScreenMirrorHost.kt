package com.ismartcoding.plain.rust.host.provider

import android.content.Intent
import android.provider.Settings
import com.ismartcoding.plain.MainActivity
import com.ismartcoding.plain.appContext
import com.ismartcoding.plain.mainActivity
import com.ismartcoding.plain.startScreenMirror
import com.ismartcoding.plain.requestScreenMirrorAudio
import com.ismartcoding.plain.lib.JsonHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.ismartcoding.plain.platform.media.dispatchScreenMirrorControls
import com.ismartcoding.plain.platform.media.getScreenMirrorVideoCodec
import com.ismartcoding.plain.platform.media.isScreenMirrorControlEnabled
import com.ismartcoding.plain.platform.media.isScreenMirrorRunning
import com.ismartcoding.plain.platform.media.requestScreenMirrorKeyFrame
import com.ismartcoding.plain.platform.media.resetScreenMirrorTouchStream
import com.ismartcoding.plain.platform.media.stopScreenMirror
import com.ismartcoding.plain.platform.permissions.Permission
import com.ismartcoding.plain.platform.permissions.isGranted
import com.ismartcoding.plain.rust.host.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*

internal object SystemScreenMirrorHost {
    internal suspend fun handle(method: String, params: JsonObject): HostResult = when (method) {
        "systemOpenAccessibilitySettings" -> withContext(Dispatchers.Main.immediate) {
            appContext.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
            hostResult(true)
        }
        "systemScreenMirrorControls" -> {
            dispatchScreenMirrorControls(params)
            hostResult()
        }
        "systemScreenMirrorResetTouch" -> {
            resetScreenMirrorTouchStream()
            hostResult()
        }
        "systemScreenMirrorState" -> {
            val codec = getScreenMirrorVideoCodec()
            hostResult(ScreenMirrorStateFacts(
                running = isScreenMirrorRunning(),
                controlEnabled = isScreenMirrorControlEnabled(),
                codec = codec,
            ))
        }
        "systemStartScreenMirror" -> {
            val request = JsonHelper.jsonDecodeFromElement<StartScreenMirrorRequest>(params)
            withContext(Dispatchers.Main.immediate) { activity().startScreenMirror(request.audio) }
            hostResult(true)
        }
        "systemStopScreenMirror" -> {
            stopScreenMirror()
            hostResult(true)
        }
        "systemRequestScreenMirrorAudio" -> {
            val granted = Permission.RECORD_AUDIO.isGranted()
            if (!granted) {
                withContext(Dispatchers.Main.immediate) { activity().requestScreenMirrorAudio() }
            }
            hostResult(granted)
        }
        "systemRequestScreenMirrorKeyFrame" -> {
            requestScreenMirrorKeyFrame()
            hostResult(true)
        }
        else -> error("Unsupported provider operation")
    }

    private fun activity(): MainActivity = (mainActivity as? MainActivity)
        ?.takeUnless { it.isDestroyed || it.isFinishing }
        ?: error("Screen mirroring requires an active main activity")
}

@Serializable
private data class StartScreenMirrorRequest(val audio: Boolean)
