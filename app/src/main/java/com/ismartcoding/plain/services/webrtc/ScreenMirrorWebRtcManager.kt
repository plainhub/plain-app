package com.ismartcoding.plain.services.webrtc

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Point
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioPlaybackCaptureConfiguration
import android.media.AudioRecord
import android.media.projection.MediaProjection
import android.os.Build
import android.view.Surface
import android.view.WindowManager
import com.ismartcoding.lib.isSPlus
import com.ismartcoding.lib.logcat.LogCat
import com.ismartcoding.plain.data.DScreenMirrorQuality
import com.ismartcoding.plain.enums.AppFeatureType
import com.ismartcoding.plain.enums.ScreenMirrorMode
import com.ismartcoding.plain.web.websocket.WebRtcSignalingMessage
import org.webrtc.AudioSource
import org.webrtc.AudioTrack
import org.webrtc.DefaultVideoDecoderFactory
import org.webrtc.DefaultVideoEncoderFactory
import org.webrtc.EglBase
import org.webrtc.MediaConstraints
import org.webrtc.PeerConnectionFactory
import org.webrtc.SurfaceTextureHelper
import org.webrtc.VideoSource
import org.webrtc.VideoTrack
import org.webrtc.audio.JavaAudioDeviceModule
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
    private var audioSource: AudioSource? = null
    private var audioTrack: AudioTrack? = null
    private var audioDeviceModule: JavaAudioDeviceModule? = null
    @Volatile
    private var audioSwapped = false
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

    // ── Frame-rate limiter (isScreencast=true disables adaptOutputFormat) ─
    private var lastFrameTimeNs = 0L
    private var targetFps = 20
    private var minFrameIntervalNs = 1_000_000_000L / targetFps

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

        mediaProjection = projection
        ensurePeerConnectionFactory(projection)
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
        val (width, height, dpi) = computeCaptureSize()

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
        // Cap at targetFps to prevent encoder overload and latency build-up.
        // (adaptOutputFormat is a no-op when isScreencast=true, so we drop frames manually.)
        surfaceTextureHelper!!.startListening { frame ->
            val now = System.nanoTime()
            if (now - lastFrameTimeNs >= minFrameIntervalNs) {
                lastFrameTimeNs = now
                videoSource!!.capturerObserver.onFrameCaptured(frame)
            }
            // Skipped frames are automatically released by SurfaceTextureHelper
        }
        videoSource!!.capturerObserver.onCapturerStarted(true)

        LogCat.d("webrtc: VirtualDisplay created ${width}x${height} dpi=$dpi")

        // Create audio source and track (JavaAudioDeviceModule handles actual capture)
        audioSource = factory.createAudioSource(MediaConstraints())
        audioTrack = factory.createAudioTrack("screen_audio", audioSource)
        audioTrack?.setEnabled(true)
        LogCat.d("webrtc: audio track created, enabled=${audioTrack?.enabled()}")
    }

    /**
     * Swap the internal mic-based AudioRecord inside WebRtcAudioRecord (via reflection)
     * with one that captures system audio using AudioPlaybackCaptureConfiguration.
     * Called on the WebRTC audio recording thread from AudioRecordStateCallback.
     */
    @SuppressLint("MissingPermission")
    private fun swapToPlaybackCapture(projection: MediaProjection) {
        if (!AppFeatureType.MIRROR_AUDIO.has()) {
            LogCat.d("webrtc: audio swap skipped, API < Q")
            return
        }
        if (audioSwapped) {
            LogCat.d("webrtc: audio swap already done")
            return
        }

        // Check RECORD_AUDIO permission at runtime (required for AudioPlaybackCapture)
        if (context.checkSelfPermission(android.Manifest.permission.RECORD_AUDIO)
            != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            LogCat.e("webrtc: RECORD_AUDIO permission not granted, cannot capture audio")
            return
        }

        try {
            val adm = audioDeviceModule ?: run {
                LogCat.e("webrtc: audioDeviceModule is null")
                return
            }

            // Access JavaAudioDeviceModule.audioInput (WebRtcAudioRecord)
            LogCat.d("webrtc: audio swap step 1 - accessing audioInput field")
            val audioInputField = adm.javaClass.getDeclaredField("audioInput")
            audioInputField.isAccessible = true
            val audioInput = audioInputField.get(adm) ?: run {
                LogCat.e("webrtc: audioInput is null")
                return
            }

            // Access WebRtcAudioRecord.audioRecord (android.media.AudioRecord)
            LogCat.d("webrtc: audio swap step 2 - accessing audioRecord field from ${audioInput.javaClass.name}")
            val audioRecordField = audioInput.javaClass.getDeclaredField("audioRecord")
            audioRecordField.isAccessible = true
            val oldRecord = audioRecordField.get(audioInput) as? AudioRecord ?: run {
                LogCat.e("webrtc: audioRecord is null or not AudioRecord")
                return
            }

            // Read params from the existing AudioRecord to match WebRTC's expectations
            val sampleRate = oldRecord.sampleRate
            val channelCount = oldRecord.channelCount
            val channelConfig = if (channelCount == 1) AudioFormat.CHANNEL_IN_MONO else AudioFormat.CHANNEL_IN_STEREO
            val encoding = oldRecord.audioFormat
            LogCat.d("webrtc: audio swap step 3 - old record params: rate=$sampleRate ch=$channelCount encoding=$encoding state=${oldRecord.state}")

            // Stop & release the mic-based AudioRecord
            try { oldRecord.stop() } catch (e: Exception) {
                LogCat.d("webrtc: old record stop exception (expected): ${e.message}")
            }
            oldRecord.release()
            LogCat.d("webrtc: audio swap step 4 - old record released")

            // Create a new AudioRecord that captures system audio via MediaProjection
            val playbackConfig = AudioPlaybackCaptureConfiguration.Builder(projection)
                .addMatchingUsage(AudioAttributes.USAGE_MEDIA)
                .addMatchingUsage(AudioAttributes.USAGE_GAME)
                .addMatchingUsage(AudioAttributes.USAGE_UNKNOWN)
                .build()

            val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, encoding) * 2
            LogCat.d("webrtc: audio swap step 5 - creating playback capture AudioRecord (bufSize=$bufferSize)")
            val newRecord = AudioRecord.Builder()
                .setAudioPlaybackCaptureConfig(playbackConfig)
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setSampleRate(sampleRate)
                        .setChannelMask(channelConfig)
                        .setEncoding(encoding)
                        .build()
                )
                .setBufferSizeInBytes(bufferSize)
                .build()

            if (newRecord.state != AudioRecord.STATE_INITIALIZED) {
                LogCat.e("webrtc: Playback-capture AudioRecord failed to initialise (state=${newRecord.state})")
                newRecord.release()
                return
            }

            // Replace the field and start the new AudioRecord
            audioRecordField.set(audioInput, newRecord)
            newRecord.startRecording()
            audioSwapped = true

            LogCat.d("webrtc: audio swap DONE - system audio capture active (rate=$sampleRate ch=$channelCount recordingState=${newRecord.recordingState})")
        } catch (e: Exception) {
            LogCat.e("webrtc: Failed to swap to playback capture: ${e.javaClass.simpleName}: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun releaseAudioCapture() {
        audioTrack = null
        audioSource?.dispose()
        audioSource = null
    }

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

                val session = WebRtcPeerSession(clientId, factory, track, audioTrack, { computeTargetBitrateKbps() }, { computeStartBitrateKbps() }, { targetFps }, { getQuality().mode })
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

        releaseAudioCapture()
        audioDeviceModule?.release()
        audioDeviceModule = null
        audioSwapped = false

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

    private fun ensurePeerConnectionFactory(projection: MediaProjection) {
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

        // Create JavaAudioDeviceModule for system audio capture.
        // Disable HW AEC/NS (not needed for system audio) and register a
        // state callback to swap the internal mic AudioRecord with one
        // using AudioPlaybackCaptureConfiguration once recording starts.
        val adm = JavaAudioDeviceModule.builder(context)
            .setUseHardwareAcousticEchoCanceler(false)
            .setUseHardwareNoiseSuppressor(false)
            .setAudioRecordStateCallback(object : JavaAudioDeviceModule.AudioRecordStateCallback {
                override fun onWebRtcAudioRecordStart() {
                    swapToPlaybackCapture(projection)
                }
                override fun onWebRtcAudioRecordStop() {
                    audioSwapped = false
                }
            })
            .createAudioDeviceModule()
        audioDeviceModule = adm

        val encoderFactory = DefaultVideoEncoderFactory(eglBase!!.eglBaseContext, true, true)
        val decoderFactory = DefaultVideoDecoderFactory(eglBase!!.eglBaseContext)
        peerConnectionFactory = PeerConnectionFactory.builder()
            .setVideoEncoderFactory(encoderFactory)
            .setVideoDecoderFactory(decoderFactory)
            .setAudioDeviceModule(adm)
            .createPeerConnectionFactory()

        // Keep the ADM Java object alive so that our AudioRecordStateCallback
        // can use reflection to swap the internal AudioRecord later.
    }

    /**
     * Recreate the [VirtualDisplay] to match the current quality / orientation.
     *
     * We intentionally avoid [VirtualDisplay.resize] because on some Android 11
     * devices it does not correctly update the rendering region when going from
     * a smaller size back to a larger one (e.g. SMOOTH → HD), leaving black bars
     * on the right and bottom edges even though the dimensions are correct.
     * Releasing and recreating the VirtualDisplay is reliable across all devices.
     */
    private fun resizeVirtualDisplay() {
        val projection = mediaProjection ?: return
        val (width, height, dpi) = computeCaptureSize()

        // Release old VirtualDisplay
        virtualDisplay?.release()
        virtualDisplay = null

        // Update SurfaceTexture buffer size
        surfaceTextureHelper?.setTextureSize(width, height)

        // Recreate Surface to ensure clean state after buffer size change
        displaySurface?.release()
        displaySurface = Surface(surfaceTextureHelper!!.surfaceTexture)

        // Create fresh VirtualDisplay with the new dimensions
        virtualDisplay = projection.createVirtualDisplay(
            "WebRTC_ScreenCapture",
            width, height, dpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            displaySurface,
            null, null,
        )

        LogCat.d("webrtc: VirtualDisplay recreated ${width}x${height} dpi=$dpi")
    }

    private fun computeCaptureSize(): Triple<Int, Int, Int> {
        val realSize = getRealScreenSize()
        val width = realSize.x
        val height = realSize.y

        val shortSide = min(width, height)
        val targetShort = getEffectiveResolution()
        val scale = min(1f, targetShort.toFloat() / shortSide.toFloat())

        val targetWidth = makeEven(max(2, (width * scale).toInt()))
        val targetHeight = makeEven(max(2, (height * scale).toInt()))
        val dpi = context.resources.displayMetrics.densityDpi

        getIsPortrait()

        return Triple(targetWidth, targetHeight, dpi)
    }

    /**
     * Get the real physical screen dimensions including system bars.
     *
     * On Android 12+ we use WindowMetrics which is accurate.
     * On Android 11 and below, Display.getRealSize() may report dimensions that
     * include non-renderable areas (e.g. rounded-corner padding or system-bar
     * offsets) on some devices, causing the VirtualDisplay surface to be larger
     * than the actual mirrored content and producing black bars on the right /
     * bottom edges.  Display.Mode.physicalWidth/Height returns the exact pixel
     * resolution of the active display mode and avoids this mismatch.
     */
    private fun getRealScreenSize(): Point {
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        return if (isSPlus()) {
            val bounds = wm.currentWindowMetrics.bounds
            Point(bounds.width(), bounds.height())
        } else {
            @Suppress("DEPRECATION")
            val display = wm.defaultDisplay
            val mode = display.mode
            var w = mode.physicalWidth
            var h = mode.physicalHeight
            if (w > 0 && h > 0) {
                // physicalWidth/Height are in the display's natural orientation;
                // swap for landscape so the VirtualDisplay dimensions match.
                @Suppress("DEPRECATION")
                val rotation = display.rotation
                if (rotation == Surface.ROTATION_90 || rotation == Surface.ROTATION_270) {
                    val tmp = w
                    w = h
                    h = tmp
                }
                Point(w, h)
            } else {
                // Fallback for devices that report invalid mode dimensions.
                val size = Point()
                @Suppress("DEPRECATION")
                display.getRealSize(size)
                size
            }
        }
    }

    private fun getEffectiveResolution(): Int {
        val quality = getQuality()
        return when (quality.mode) {
            ScreenMirrorMode.AUTO -> adaptiveResolution
            ScreenMirrorMode.HD -> 1080
            ScreenMirrorMode.SMOOTH -> 720
        }
    }

    /**
     * Compute target bitrate for screen content.
     * Professional remote-desktop approach: screen content is mostly static UI/text,
     * so we use much lower bitrates than video streaming.
     * H.264/VP8 with high QP is fine for UI — sharp text, infrequent keyframes.
     */
    private fun computeTargetBitrateKbps(): Int {
        val resolution = getEffectiveResolution()
        return when {
            resolution >= 1080 -> 4000   // 1080p: 4 Mbps max (was 20 Mbps)
            resolution >= 720  -> 2000   // 720p:  2 Mbps max (was 10 Mbps)
            else               -> 1000
        }
    }

    /**
     * Compute initial (starting) bitrate for the congestion controller.
     * Start conservatively so the first few seconds don't cause bufferbloat.
     */
    private fun computeStartBitrateKbps(): Int {
        val resolution = getEffectiveResolution()
        return when {
            resolution >= 1080 -> 3000   // start at 3 Mbps, ramp to 4
            resolution >= 720  -> 1500   // start at 1.5 Mbps, ramp to 2
            else               -> 800
        }
    }

    fun getTargetFps(): Int = targetFps

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
            val oldFps = targetFps

            // ── Step 1: Aggressive ABR — react to packet loss first ──
            if (packetLossPercent > 2.0) {
                // High packet loss: drop fps first (cheaper than resolution change)
                if (targetFps > 10) {
                    targetFps = max(10, (targetFps * 0.7).toInt())
                    minFrameIntervalNs = 1_000_000_000L / targetFps
                }
                // If fps already low and still losing packets, drop resolution
                if (packetLossPercent > 5.0 && adaptiveResolution > 720) {
                    adaptiveResolution = 720
                }
            }

            // ── Step 2: RTT-based degradation ──
            if (rttMs > 150) {
                if (targetFps > 15) {
                    targetFps = 15
                    minFrameIntervalNs = 1_000_000_000L / targetFps
                }
                if (rttMs > 300 && adaptiveResolution > 720) {
                    adaptiveResolution = 720
                }
            }

            // ── Step 3: Bandwidth-based degradation ──
            if (availableBitrateKbps in 1 until 2000) {
                if (adaptiveResolution > 720) adaptiveResolution = 720
                if (targetFps > 15) {
                    targetFps = 15
                    minFrameIntervalNs = 1_000_000_000L / targetFps
                }
            }

            // ── Step 4: Upgrade when network is healthy ──
            val isHealthy = availableBitrateKbps > 4000 && packetLossPercent < 1.0 && rttMs < 50
            if (isHealthy) {
                if (adaptiveResolution < 1080) adaptiveResolution = 1080
                if (targetFps < 20) {
                    targetFps = 20
                    minFrameIntervalNs = 1_000_000_000L / targetFps
                }
            }

            val changed = oldResolution != adaptiveResolution || oldFps != targetFps
            if (changed) {
                LogCat.d("webrtc: adaptive ${oldResolution}p@${oldFps}fps → ${adaptiveResolution}p@${targetFps}fps " +
                    "(bw=${availableBitrateKbps}kbps loss=${String.format("%.1f", packetLossPercent)}% rtt=${String.format("%.0f", rttMs)}ms)")
                if (oldResolution != adaptiveResolution) resizeVirtualDisplay()
                peerSessions.values.forEach { it.updateVideoBitrate() }
            }
        }
    }

    private fun makeEven(value: Int): Int = if (value % 2 == 0) value else value - 1

    companion object {
        private var webrtcInitialized = false
    }
}
