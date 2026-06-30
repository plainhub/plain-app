package com.ismartcoding.plain.services.screenmirror

import android.content.Context
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.projection.MediaProjection
import android.util.Base64
import com.ismartcoding.plain.data.DScreenMirrorQuality
import com.ismartcoding.plain.enums.ScreenMirrorMode
import com.ismartcoding.plain.events.EventType
import com.ismartcoding.plain.events.WebSocketEvent
import com.ismartcoding.plain.helpers.JsonHelper.jsonEncode
import com.ismartcoding.plain.lib.channel.sendEvent
import com.ismartcoding.plain.lib.isUPlus
import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.web.models.ScreenMirrorVideoCodec

/**
 * Owns MediaProjection + VirtualDisplay + the two encoders. Pushes H.264 NAL
 * units and Opus packets to all connected ws clients.
 */
class ScreenMirrorPipeline(
    private val context: Context,
    private val projection: MediaProjection,
    private var quality: DScreenMirrorQuality,
    private val getIsPortrait: () -> Boolean = { true },
) {
    companion object {
        private const val TAG = "MirrorPipeline"
        private const val VD_NAME = "PlainMirrorVD"
    }

    private var videoEncoder: MediaCodecVideoEncoder? = null
    private var audioEncoder: MediaCodecAudioEncoder? = null
    private var virtualDisplay: VirtualDisplay? = null

    @Volatile
    private var cachedConfig: ByteArray? = null

    @Volatile
    private var cachedKeyFrame: ByteArray? = null

    @Volatile
    private var pendingConfigBroadcast: ByteArray? = null

    fun getScreenMirrorVideoCodec(): ScreenMirrorVideoCodec? {
        if (cachedConfig == null) {
            return null
        }

        return ScreenMirrorVideoCodec(
            annexB = Base64.encodeToString(cachedConfig, Base64.NO_WRAP),
            keyFrame = cachedKeyFrame?.let { Base64.encodeToString(it, Base64.NO_WRAP) },
        )
    }

    val effectiveResolution: Int
        get() = when (quality.mode) {
            ScreenMirrorMode.SMOOTH -> 720
            ScreenMirrorMode.HD -> 1080
        }

    fun start() {
        val (w, h, dpi) = computeCaptureSize(effectiveResolution)
        startEncoders(w, h, dpi)
    }

    fun onOrientationChanged() {
        rebuildEncoderAndResize("orientation changed (portrait=${getIsPortrait()})")
    }

    fun setQuality(quality: DScreenMirrorQuality) {
        this.quality = quality
        rebuildEncoderAndResize("quality=${quality.mode}")
    }

    private fun rebuildEncoderAndResize(reason: String) {
        val (w, h, dpi) = computeCaptureSize(effectiveResolution)
        LogCat.d("$TAG: $reason, encoder at ${w}x${h}")
        if (videoEncoder == null || virtualDisplay == null) return
        val oldEncoder = videoEncoder
        val video = createVideoEncoder(w, h)
        try {
            virtualDisplay?.surface = video.getInputSurface()
        } catch (e: Exception) {
            LogCat.e("$TAG: setSurface failed: ${e.message}")
            video.stop()
            videoEncoder = oldEncoder
            return
        }
        videoEncoder = video
        try {
            oldEncoder?.stop()
        } catch (_: Exception) {
        }
        try {
            virtualDisplay?.resize(w, h, dpi)
        } catch (e: Exception) {
            LogCat.e("$TAG: resize failed: ${e.message}")
        }
    }

    private fun broadcastConfig(keyFrame: ByteArray? = null) {
        sendEvent(
            WebSocketEvent(
                EventType.SCREEN_MIRROR_VIDEO_CODEC,
                jsonEncode(getScreenMirrorVideoCodec()),
            )
        )
    }

    private fun startEncoders(w: Int, h: Int, dpi: Int) {
        val video = createVideoEncoder(w, h)
        val displaySurface = video.getInputSurface()

        projection.registerCallback(object : MediaProjection.Callback() {
            override fun onStop() {
                LogCat.d("$TAG: MediaProjection onStop (system revoked)")
                stop()
            }
        }, null)

        val vdFlags = if (isUPlus()) 0 else DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR
        val vd = projection.createVirtualDisplay(VD_NAME, w, h, dpi, vdFlags, displaySurface, null, null)
        if (vd == null) {
            LogCat.e("$TAG: createVirtualDisplay returned null")
            video.stop()
            videoEncoder = null
            return
        }
        virtualDisplay = vd
        LogCat.d("$TAG: started ${w}x${h} dpi=$dpi")

        if (audioEncoder == null) {
            val audio = MediaCodecAudioEncoder(context, projection).also {
                it.onEncoded = { opus, _ ->
                    sendEvent(
                        WebSocketEvent(
                            EventType.SCREEN_MIRROR_AUDIO,
                            opus
                        )
                    )
                }
                it.start()
            }
            audioEncoder = audio
        }
    }

    private fun createVideoEncoder(w: Int, h: Int): MediaCodecVideoEncoder {
        val video = MediaCodecVideoEncoder(
            width = w, height = h,
            frameRate = 30,
            bitrateBps = computeStartBitrate(effectiveResolution),
        ).also {
            it.onCodecConfig = { configBytes ->
                cachedConfig = configBytes
                cachedKeyFrame = null
                LogCat.d("$TAG: cached annex-B config ${configBytes.size}B")
                pendingConfigBroadcast = configBytes
            }
            it.onEncoded = { nalu, isKey, _ ->
                val bundled = if (isKey) {
                    cachedKeyFrame = nalu
                    val pending = pendingConfigBroadcast
                    if (pending != null) {
                        broadcastConfig(keyFrame = nalu)
                        pendingConfigBroadcast = null
                        true
                    } else {
                        false
                    }
                } else {
                    false
                }
                if (!bundled) {
                    sendEvent(
                        WebSocketEvent(
                            EventType.SCREEN_MIRROR_VIDEO,
                            nalu
                        )
                    )
                }
            }
            it.start()
        }
        videoEncoder = video
        return video
    }

    fun stop() {
        try {
            virtualDisplay?.release()
        } catch (_: Exception) {
        }
        virtualDisplay = null
        try {
            audioEncoder?.stop()
        } catch (_: Exception) {
        }
        audioEncoder = null
        try {
            videoEncoder?.stop()
        } catch (_: Exception) {
        }
        videoEncoder = null
        try {
            projection.stop()
        } catch (_: Exception) {
        }
        LogCat.d("$TAG: stopped")
    }

    private fun computeCaptureSize(shortTarget: Int): Triple<Int, Int, Int> {
        val realSize = getRealScreenSize(context)
        val physW = realSize.x
        val physH = realSize.y
        val scale = minOf(1f, shortTarget.toFloat() / minOf(physW, physH).toFloat())
        val w = makeEven((physW * scale).toInt().coerceAtLeast(2))
        val h = makeEven((physH * scale).toInt().coerceAtLeast(2))
        return Triple(w, h, context.resources.displayMetrics.densityDpi)
    }

    private fun computeStartBitrate(shortTarget: Int): Int = when {
        shortTarget >= 1080 -> 4_000_000
        shortTarget >= 720 -> 2_000_000
        else -> 1_000_000
    }

    private fun makeEven(v: Int): Int = if (v % 2 == 0) v else v - 1
}
