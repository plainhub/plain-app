package com.ismartcoding.plain.services.webrtc

import android.content.Context
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.projection.MediaProjection
import android.view.Surface
import com.ismartcoding.lib.isUPlus
import com.ismartcoding.lib.logcat.LogCat
import com.ismartcoding.plain.data.DScreenMirrorQuality
import com.ismartcoding.plain.enums.ScreenMirrorMode
import com.ismartcoding.plain.web.websocket.WebRtcSignalingMessage
import org.webrtc.AudioSource
import org.webrtc.AudioTrack
import org.webrtc.EglBase
import org.webrtc.MediaConstraints
import org.webrtc.PeerConnectionFactory
import org.webrtc.SurfaceTextureHelper
import org.webrtc.VideoSource
import org.webrtc.VideoTrack
import org.webrtc.audio.JavaAudioDeviceModule

class ScreenMirrorWebRtcManager(
    private val context: Context,
    private val getQuality: () -> DScreenMirrorQuality,
    private val getIsPortrait: () -> Boolean,
) {
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
    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var displaySurface: Surface? = null
    private val peerSessions = mutableMapOf<String, WebRtcPeerSession>()

    internal val adaptiveMonitor = AdaptiveQualityMonitor(
        getQuality = getQuality,
        getFirstPeerSession = { peerSessions.values.firstOrNull() },
        onAdapt = { resChanged ->
            if (resChanged) resizeVirtualDisplay()
            peerSessions.values.forEach { it.updateVideoBitrate() }
        },
    )

    fun initCapture(projection: MediaProjection): Boolean {
        if (virtualDisplay != null) {
            LogCat.d("webrtc: capture already initialised, skipping")
            return true
        }
        mediaProjection = projection
        eglBase = EglBase.create()
        val (factory, adm) = createWebRtcFactory(context, eglBase!!, projection,
            onAudioRecordStart = {
                if (!audioSwapped) audioSwapped = swapToPlaybackCapture(context, audioDeviceModule, projection)
            },
            onAudioRecordStop = { audioSwapped = false },
        )
        peerConnectionFactory = factory
        audioDeviceModule = adm

        projection.registerCallback(object : MediaProjection.Callback() {
            override fun onStop() {
                LogCat.d("webrtc: MediaProjection stopped by system")
                releaseAll()
            }
        }, null)

        surfaceTextureHelper = SurfaceTextureHelper.create("ScreenCaptureThread", eglBase!!.eglBaseContext)
        videoSource = factory.createVideoSource(true)
        videoTrack = factory.createVideoTrack("screen_video", videoSource)

        val (width, height, dpi) = computeCaptureSize(context, getQuality(), adaptiveMonitor.adaptiveResolution)
        getIsPortrait()
        surfaceTextureHelper!!.setTextureSize(width, height)
        displaySurface = Surface(surfaceTextureHelper!!.surfaceTexture)

        val vdFlags = if (isUPlus()) 0 else DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR
        virtualDisplay = projection.createVirtualDisplay(
            "WebRTC_ScreenCapture", width, height, dpi, vdFlags, displaySurface, null, null,
        )
        if (virtualDisplay == null) {
            LogCat.e("webrtc: createVirtualDisplay returned null")
            videoTrack = null
            videoSource?.dispose(); videoSource = null
            surfaceTextureHelper?.dispose(); surfaceTextureHelper = null
            displaySurface?.release(); displaySurface = null
            return false
        }

        surfaceTextureHelper!!.startListening { frame ->
            val now = System.nanoTime()
            if (now - adaptiveMonitor.lastFrameTimeNs >= adaptiveMonitor.minFrameIntervalNs) {
                adaptiveMonitor.lastFrameTimeNs = now
                videoSource!!.capturerObserver.onFrameCaptured(frame)
            }
        }
        videoSource!!.capturerObserver.onCapturerStarted(true)
        LogCat.d("webrtc: VirtualDisplay created ${width}x${height} dpi=$dpi")

        audioSource = factory.createAudioSource(MediaConstraints())
        audioTrack = factory.createAudioTrack("screen_audio", audioSource)
        audioTrack?.setEnabled(true)
        return true
    }

    fun handleSignaling(clientId: String, message: WebRtcSignalingMessage) {
        when (message.type) {
            "ready" -> {
                LogCat.d("webrtc: ready from $clientId, phoneIp=${message.phoneIp}")
                val factory = peerConnectionFactory ?: return
                val track = videoTrack ?: return
                peerSessions.remove(clientId)?.release()
                val session = WebRtcPeerSession(
                    clientId, factory, track, audioTrack,
                    { computeTargetBitrateKbps(getEffectiveResolution(getQuality(), adaptiveMonitor.adaptiveResolution)) },
                    { computeStartBitrateKbps(getEffectiveResolution(getQuality(), adaptiveMonitor.adaptiveResolution)) },
                    { adaptiveMonitor.targetFps }, { getQuality().mode },
                )
                peerSessions[clientId] = session
                session.createPeerConnectionAndOffer()
                if (getQuality().mode == ScreenMirrorMode.AUTO) adaptiveMonitor.start()
            }
            "answer" -> if (!message.sdp.isNullOrBlank()) peerSessions[clientId]?.handleAnswer(message.sdp)
            "ice_candidate" -> if (!message.candidate.isNullOrBlank()) peerSessions[clientId]?.handleIceCandidate(message)
            else -> LogCat.d("webrtc: ignore signaling type=${message.type}")
        }
    }

    fun onQualityChanged() {
        val quality = getQuality()
        if (quality.mode == ScreenMirrorMode.AUTO) {
            adaptiveMonitor.adaptiveResolution = 1080
            adaptiveMonitor.start()
        } else {
            adaptiveMonitor.stop()
        }
        resizeVirtualDisplay()
        peerSessions.values.forEach { it.updateVideoBitrate() }
    }

    fun onOrientationChanged() = resizeVirtualDisplay()

    fun removeClient(clientId: String) {
        peerSessions.remove(clientId)?.release()
    }

    fun releaseAll() {
        adaptiveMonitor.stop()
        peerSessions.values.forEach { it.release() }; peerSessions.clear()
        audioTrack = null; audioSource?.dispose(); audioSource = null
        audioDeviceModule?.release(); audioDeviceModule = null; audioSwapped = false
        virtualDisplay?.release(); virtualDisplay = null
        displaySurface?.release(); displaySurface = null
        surfaceTextureHelper?.stopListening()
        videoSource?.capturerObserver?.onCapturerStopped()
        mediaProjection?.stop(); mediaProjection = null
        surfaceTextureHelper?.dispose(); surfaceTextureHelper = null
        videoTrack = null; videoSource?.dispose(); videoSource = null
        peerConnectionFactory?.dispose(); peerConnectionFactory = null
        eglBase?.release(); eglBase = null
    }

    private fun resizeVirtualDisplay() {
        val projection = mediaProjection ?: return
        val (w, h, dpi) = computeCaptureSize(context, getQuality(), adaptiveMonitor.adaptiveResolution)
        getIsPortrait()
        if (isUPlus()) {
            surfaceTextureHelper?.setTextureSize(w, h)
            virtualDisplay?.resize(w, h, dpi)
        } else {
            virtualDisplay?.release(); virtualDisplay = null
            surfaceTextureHelper?.setTextureSize(w, h)
            displaySurface?.release()
            displaySurface = Surface(surfaceTextureHelper!!.surfaceTexture)
            virtualDisplay = projection.createVirtualDisplay(
                "WebRTC_ScreenCapture", w, h, dpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, displaySurface, null, null,
            )
        }
        LogCat.d("webrtc: VirtualDisplay ${if (isUPlus()) "resized" else "recreated"} ${w}x${h} dpi=$dpi")
    }
}
