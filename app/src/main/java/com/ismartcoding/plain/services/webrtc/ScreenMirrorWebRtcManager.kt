package com.ismartcoding.plain.services.webrtc

import android.content.Context
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.projection.MediaProjection
import android.view.Surface
import com.ismartcoding.lib.logcat.LogCat
import com.ismartcoding.plain.data.DScreenMirrorQuality
import com.ismartcoding.plain.enums.ScreenMirrorMode
import com.ismartcoding.plain.web.websocket.WebRtcSignalingMessage
import org.webrtc.DefaultVideoDecoderFactory
import org.webrtc.DefaultVideoEncoderFactory
import org.webrtc.EglBase
import org.webrtc.PeerConnectionFactory
import org.webrtc.SurfaceTextureHelper
import org.webrtc.VideoSource
import org.webrtc.VideoTrack
import kotlin.math.max
import kotlin.math.min

/**
 * Manages the shared screen-capture resources (MediaProjection, VirtualDisplay,
 * VideoSource, VideoTrack) and a set of [WebRtcPeerSession]s — one per connected
 * web client.
 *
 * [initCapture] is called exactly once from `ScreenMirrorService.onStartCommand()`
 * with the [MediaProjection] obtained from the one-time-use permission intent.
 * Subsequent orientation or quality changes are handled by [VirtualDisplay.resize],
 * which avoids re-creating the MediaProjection.
 */
class ScreenMirrorWebRtcManager(
    private val context: Context,
    private val getQuality: () -> DScreenMirrorQuality,
    private val getIsPortrait: () -> Boolean,
) {
    // ── Shared capture resources ──────────────────────────────────────────
    private var peerConnectionFactory: PeerConnectionFactory? = null
    private var videoSource: VideoSource? = null
    private var videoTrack: VideoTrack? = null
    private var surfaceTextureHelper: SurfaceTextureHelper? = null
    private var eglBase: EglBase? = null

    // ── MediaProjection + VirtualDisplay (created once, resized as needed) ─
    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var displaySurface: Surface? = null

    // ── Per-client peer sessions ──────────────────────────────────────────
    private val peerSessions = mutableMapOf<String, WebRtcPeerSession>()

    // ── Adaptive quality state (AUTO mode) ────────────────────────────────
    private var adaptiveResolution: Int = 1080
    private var statsHandler: android.os.Handler? = null
    private val statsIntervalMs = 3000L

    // ── Public API ────────────────────────────────────────────────────────

    /**
     * Initialise capture using the [MediaProjection] obtained from the system.
     * Creates a [VirtualDisplay] that renders screen content into a WebRTC
     * [VideoTrack].  Must be called exactly once.
     */
    fun initCapture(projection: MediaProjection) {
        if (virtualDisplay != null) {
            LogCat.d("webrtc: capture already initialised, skipping")
            return
        }

        ensurePeerConnectionFactory()

        mediaProjection = projection
        projection.registerCallback(object : MediaProjection.Callback() {
            override fun onStop() {
                LogCat.d("webrtc: MediaProjection stopped")
            }
        }, null)

        val egl = eglBase ?: return
        val factory = peerConnectionFactory ?: return

        surfaceTextureHelper = SurfaceTextureHelper.create("ScreenCaptureThread", egl.eglBaseContext)
        videoSource = factory.createVideoSource(/* isScreencast = */ true)
        videoTrack = factory.createVideoTrack("screen_video", videoSource)

        // Create VirtualDisplay → Surface(SurfaceTexture) → SurfaceTextureHelper → VideoSource
        val (width, height) = computeCaptureSize()
        val dpi = context.resources.displayMetrics.densityDpi

        surfaceTextureHelper!!.setTextureSize(width, height)
        displaySurface = Surface(surfaceTextureHelper!!.surfaceTexture)

        virtualDisplay = projection.createVirtualDisplay(
            "WebRTC_ScreenCapture",
            width, height, dpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            displaySurface,
            null, null,
        )

        // Start forwarding frames: SurfaceTextureHelper → VideoSource
        surfaceTextureHelper!!.startListening { frame ->
            videoSource!!.capturerObserver.onFrameCaptured(frame)
        }
        videoSource!!.capturerObserver.onCapturerStarted(true)

        LogCat.d("webrtc: VirtualDisplay created ${width}x${height} dpi=$dpi")
    }

    /**
     * Route an incoming signaling message to the appropriate [WebRtcPeerSession].
     * A `"ready"` message creates (or re-creates) a session for [clientId].
     */
    fun handleSignaling(clientId: String, message: WebRtcSignalingMessage) {
        when (message.type) {
            "ready" -> {
                LogCat.d("webrtc: ready from $clientId")
                val factory = peerConnectionFactory
                val track = videoTrack
                if (factory == null || track == null) {
                    LogCat.e("webrtc: capturer not initialised, ignoring ready")
                    return
                }

                // Tear down any previous session for this client (re-negotiation).
                peerSessions.remove(clientId)?.release()

                val session = WebRtcPeerSession(clientId, factory, track) { computeTargetBitrateKbps() }
                peerSessions[clientId] = session
                session.createPeerConnectionAndOffer()

                if (getQuality().mode == ScreenMirrorMode.AUTO) {
                    startStatsMonitoring()
                }
            }

            "answer" -> {
                if (!message.sdp.isNullOrBlank()) {
                    peerSessions[clientId]?.handleAnswer(message.sdp)
                }
            }

            "ice_candidate" -> {
                if (!message.candidate.isNullOrBlank()) {
                    peerSessions[clientId]?.handleIceCandidate(message)
                }
            }

            else -> {
                LogCat.d("webrtc: ignore signaling type=${message.type}")
            }
        }
    }

    fun onQualityChanged() {
        val quality = getQuality()
        if (quality.mode == ScreenMirrorMode.AUTO) {
            adaptiveResolution = 1080
            startStatsMonitoring()
        } else {
            stopStatsMonitoring()
        }
        resizeVirtualDisplay()
        peerSessions.values.forEach { it.updateVideoBitrate() }
    }

    fun onOrientationChanged() {
        resizeVirtualDisplay()
    }

    fun removeClient(clientId: String) {
        peerSessions.remove(clientId)?.release()
    }

    fun releaseAll() {
        stopStatsMonitoring()
        peerSessions.values.forEach { it.release() }
        peerSessions.clear()

        virtualDisplay?.release()
        virtualDisplay = null

        displaySurface?.release()
        displaySurface = null

        surfaceTextureHelper?.stopListening()
        videoSource?.capturerObserver?.onCapturerStopped()

        mediaProjection?.stop()
        mediaProjection = null

        surfaceTextureHelper?.dispose()
        surfaceTextureHelper = null

        videoTrack = null
        videoSource?.dispose()
        videoSource = null

        peerConnectionFactory?.dispose()
        peerConnectionFactory = null

        eglBase?.release()
        eglBase = null
    }

    // ── Private helpers ───────────────────────────────────────────────────

    private fun ensurePeerConnectionFactory() {
        if (peerConnectionFactory != null) return

        if (!webrtcInitialized) {
            PeerConnectionFactory.initialize(
                PeerConnectionFactory.InitializationOptions.builder(context)
                    .setEnableInternalTracer(false)
                    .createInitializationOptions(),
            )
            webrtcInitialized = true
        }

        eglBase = EglBase.create()
        val encoderFactory = DefaultVideoEncoderFactory(eglBase!!.eglBaseContext, true, true)
        val decoderFactory = DefaultVideoDecoderFactory(eglBase!!.eglBaseContext)
        peerConnectionFactory = PeerConnectionFactory.builder()
            .setVideoEncoderFactory(encoderFactory)
            .setVideoDecoderFactory(decoderFactory)
            .createPeerConnectionFactory()
    }

    /**
     * Resize the existing [VirtualDisplay] to match the current quality / orientation.
     * No need to recreate the MediaProjection or VirtualDisplay.
     */
    private fun resizeVirtualDisplay() {
        val vd = virtualDisplay ?: return
        val (width, height) = computeCaptureSize()
        val dpi = context.resources.displayMetrics.densityDpi

        surfaceTextureHelper?.setTextureSize(width, height)
        vd.resize(width, height, dpi)

        LogCat.d("webrtc: VirtualDisplay resized ${width}x${height} dpi=$dpi")
    }

    private fun computeCaptureSize(): Pair<Int, Int> {
        val metrics = context.resources.displayMetrics
        val width = metrics.widthPixels
        val height = metrics.heightPixels

        val shortSide = min(width, height)
        val targetShort = getEffectiveResolution()
        val scale = min(1f, targetShort.toFloat() / shortSide.toFloat())

        val targetWidth = makeEven(max(2, (width * scale).toInt()))
        val targetHeight = makeEven(max(2, (height * scale).toInt()))

        getIsPortrait()

        return Pair(targetWidth, targetHeight)
    }

    private fun getEffectiveResolution(): Int {
        val quality = getQuality()
        return when (quality.mode) {
            ScreenMirrorMode.AUTO -> adaptiveResolution
            ScreenMirrorMode.HD -> 1080
            ScreenMirrorMode.SMOOTH -> 720
        }
    }

    private fun computeTargetBitrateKbps(): Int {
        val resolution = getEffectiveResolution()
        // LAN bitrates for screen content (sharp text/UI edges)
        // resolution = short side, so 1080p on a 20:9 phone ≈ 1080×2400
        return when {
            resolution >= 1080 -> 20000
            resolution >= 720 -> 10000
            else -> 4000
        }
    }

    // ── Adaptive stats monitoring (AUTO mode) ─────────────────────────────

    private fun startStatsMonitoring() {
        stopStatsMonitoring()
        if (getQuality().mode != ScreenMirrorMode.AUTO) return

        statsHandler = android.os.Handler(android.os.Looper.getMainLooper())
        statsHandler?.postDelayed(object : Runnable {
            override fun run() {
                if (getQuality().mode != ScreenMirrorMode.AUTO) return
                pollStatsAndAdapt()
                statsHandler?.postDelayed(this, statsIntervalMs)
            }
        }, statsIntervalMs)
    }

    private fun stopStatsMonitoring() {
        statsHandler?.removeCallbacksAndMessages(null)
        statsHandler = null
    }

    private fun pollStatsAndAdapt() {
        val session = peerSessions.values.firstOrNull() ?: return
        session.getStats { availableBitrateKbps, packetLossPercent, rttMs ->
            val oldResolution = adaptiveResolution

            // Downgrade: high packet loss or low available bitrate
            val shouldDowngrade = packetLossPercent > 5.0 || rttMs > 150 ||
                    (availableBitrateKbps in 1 until 8000)
            // Upgrade: plenty of bandwidth and good network
            val shouldUpgrade = availableBitrateKbps > 15000 && packetLossPercent < 1.0 && rttMs < 50

            if (shouldDowngrade && adaptiveResolution > 720) {
                adaptiveResolution = 720
            } else if (shouldUpgrade && adaptiveResolution < 1080) {
                adaptiveResolution = 1080
            }

            if (oldResolution != adaptiveResolution) {
                LogCat.d("webrtc: adaptive resolution $oldResolution → $adaptiveResolution " +
                    "(bw=${availableBitrateKbps}kbps loss=${String.format("%.1f", packetLossPercent)}% rtt=${String.format("%.0f", rttMs)}ms)")
                resizeVirtualDisplay()
                peerSessions.values.forEach { it.updateVideoBitrate() }
            }
        }
    }

    private fun makeEven(value: Int): Int = if (value % 2 == 0) value else value - 1

    companion object {
        private var webrtcInitialized = false
    }
}
