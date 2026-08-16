package com.ismartcoding.plain.services.screenmirror

import android.content.Context
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.projection.MediaProjection
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import com.ismartcoding.plain.data.DScreenMirrorQuality
import com.ismartcoding.plain.enums.ScreenMirrorMode
import com.ismartcoding.plain.events.EventType
import com.ismartcoding.plain.events.WebSocketEvent
import com.ismartcoding.plain.lib.JsonHelper.jsonEncode
import com.ismartcoding.plain.platform.isUPlus
import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.lib.sendEvent
import com.ismartcoding.plain.services.ScreenMirrorService
import com.ismartcoding.plain.httpserver.models.ScreenMirrorVideoCodec
import com.ismartcoding.plain.platform.isRPlus

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

    /**
     * Decides whether this session should run with "low-end" encoder params
     * (reduced pixel budget, shorter I-frame intervals, etc.).
     *
     * Two-tier decision:
     *  1. **Initial guess** — [API < 30][isRPlus] → true. Prevents the first
     *     2 seconds of jank on obviously-old devices before the runtime
     *     detector has enough samples.
     *  2. **Runtime verdict** — [onEncoderOverloaded] flips [forceLowEnd] to
     *     true permanently. This catches every slow encoder regardless of
     *     API level (e.g. underpowered Android 10/11 ROMs).
     *
     * One-way latch — once low-end, never goes back. Avoids oscillation.
     */
    private inner class LowEndPolicy {
        private val initiallyLowEnd = !isRPlus()

        @Volatile private var forceLowEnd = false
        @Volatile private var didDowngrade = false

        val active: Boolean get() = forceLowEnd || initiallyLowEnd

        /** Pixel budget fed to [ScreenMirrorCaptureSize.compute]. */
        val maxPixels: Int get() = if (active) 1_500_000 else Int.MAX_VALUE

        /** [MediaFormat.KEY_I_FRAME_INTERVAL] in seconds. */
        val iFrameIntervalSec: Int get() = if (active) 2 else 10

        /** [MediaFormat.KEY_REPEAT_PREVIOUS_FRAME_AFTER] in µs. */
        val repeatPreviousFrameAfterUs: Long get() = if (active) 33_000L else 100_000L

        /** [MediaFormat.KEY_LATENCY] value, null = skip the key entirely. */
        val latency: Int? get() = if (active) null else 1

        /**
         * Called from the encoder overload detector. Returns `true` if this
         * is the first downgrade (caller should rebuild the encoder);
         * subsequent calls are no-ops to avoid rebuild loops.
         */
        fun onEncoderOverloaded(): Boolean {
            if (didDowngrade) return false
            didDowngrade = true
            forceLowEnd = true
            LogCat.d("$TAG: encoder overloaded — downgrading to low-end profile")
            return true
        }
    }

    private var videoEncoder: MediaCodecVideoEncoder? = null
    private var audioEncoder: MediaCodecAudioEncoder? = null
    private var virtualDisplay: VirtualDisplay? = null
    private val lowEndPolicy = LowEndPolicy()

    @Volatile
    private var cachedConfig: ByteArray? = null

    @Volatile
    private var cachedKeyFrame: ByteArray? = null

    @Volatile
    private var pendingConfigBroadcast: ByteArray? = null

    @Volatile
    private var videoFrameId: Int = 0

    @Volatile
    private var audioFrameId: Int = 0

    @OptIn(ExperimentalEncodingApi::class)
    fun getScreenMirrorVideoCodec(): ScreenMirrorVideoCodec? {
        val config = cachedConfig ?: return null

        return ScreenMirrorVideoCodec(
            annexB = Base64.encode(config),
            keyFrame = cachedKeyFrame?.let { Base64.encode(it) },
        )
    }

    @OptIn(ExperimentalEncodingApi::class)
    suspend fun awaitVideoCodec(timeoutMs: Long = 3000): ScreenMirrorVideoCodec? {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (cachedConfig == null) {
            if (System.currentTimeMillis() > deadline) {
                LogCat.w("$TAG: timed out waiting for codec config (${timeoutMs}ms)")
                return null
            }
            kotlinx.coroutines.delay(50)
        }
        return getScreenMirrorVideoCodec()
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

    private fun broadcastConfig() {
        sendEvent(
            WebSocketEvent(
                EventType.SCREEN_MIRROR_VIDEO_CODEC,
                jsonEncode(getScreenMirrorVideoCodec()),
            )
        )
    }

    fun requestKeyFrame() {
        videoEncoder?.requestKeyFrame()
        LogCat.d("$TAG: key frame requested by client")
    }

    private fun startEncoders(w: Int, h: Int, dpi: Int) {
        val video = createVideoEncoder(w, h)
        val displaySurface = video.getInputSurface()

        projection.registerCallback(object : MediaProjection.Callback() {
            override fun onStop() {
                LogCat.d("$TAG: MediaProjection onStop (system revoked)")
                ScreenMirrorService.instance?.stop()
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
                it.onEncoded = { opus, pts ->
                    val packet = VideoPacket.encode(audioFrameId++, pts, VideoPacket.FLAG_AUDIO, opus)
                    sendEvent(
                        WebSocketEvent(
                            EventType.SCREEN_MIRROR_AUDIO,
                            packet
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
            frameRate = 60,
            bitrateBps = computeStartBitrate(effectiveResolution),
            iFrameIntervalSec = lowEndPolicy.iFrameIntervalSec,
            repeatPreviousFrameAfterUs = lowEndPolicy.repeatPreviousFrameAfterUs,
            latency = lowEndPolicy.latency,
        ).also {
            it.onCodecConfig = { configBytes ->
                cachedConfig = configBytes
                cachedKeyFrame = null
                LogCat.d("$TAG: cached annex-B config ${configBytes.size}B")
                pendingConfigBroadcast = configBytes
            }
            it.onEncoded = onEncoded@{ nalu, isKey, pts ->
                if (isKey) {
                    cachedKeyFrame = nalu
                    if (pendingConfigBroadcast != null) {
                        broadcastConfig()
                        pendingConfigBroadcast = null
                        return@onEncoded
                    }
                }
                val flags: Byte = if (isKey) VideoPacket.FLAG_KEY_FRAME else 0
                val packet = VideoPacket.encode(videoFrameId++, pts, flags, nalu)
                sendEvent(
                    WebSocketEvent(
                        EventType.SCREEN_MIRROR_VIDEO,
                        packet
                    )
                )
            }
            it.onOverloaded = onOverloaded@{
                if (lowEndPolicy.onEncoderOverloaded()) {
                    rebuildEncoderAndResize("encoder overloaded")
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
        val caps = MediaCodecVideoEncoder.queryEncoderCaps()
        val maxW = caps?.maxWidth ?: 4096
        val maxH = caps?.maxHeight ?: 4096
        val wAlign = caps?.widthAlignment ?: 2
        val hAlign = caps?.heightAlignment ?: 2
        val (w, h) = ScreenMirrorCaptureSize.compute(
            physW, physH, shortTarget, maxW, maxH, wAlign, hAlign,
            maxPixels = lowEndPolicy.maxPixels,
        )
        LogCat.d("$TAG: captureSize phys=${physW}x${physH} target=$shortTarget lowEnd=${lowEndPolicy.active} encMax=${maxW}x${maxH} align=${wAlign}x${hAlign} → ${w}x${h}")
        return Triple(w, h, context.resources.displayMetrics.densityDpi)
    }

    // Bitrate values from scrcpy defaults (SurfaceEncoder.java createFormat()).
    // scrcpy default is 8 Mbps for 1080p, proven across Pixel/Xiaomi/Samsung.
    // Higher bitrates (e.g. 24 Mbps) cause encoder/decoder frame drops and
    // increased end-to-end latency without visible quality gain for screen
    // content.
    private fun computeStartBitrate(shortTarget: Int): Int = when {
        shortTarget >= 1080 -> 8_000_000
        shortTarget >= 720 -> 4_000_000
        else -> 2_000_000
    }
}
