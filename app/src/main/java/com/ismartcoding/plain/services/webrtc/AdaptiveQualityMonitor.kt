package com.ismartcoding.plain.services.webrtc

import android.os.Handler
import android.os.Looper
import com.ismartcoding.lib.logcat.LogCat
import com.ismartcoding.plain.data.DScreenMirrorQuality
import com.ismartcoding.plain.enums.ScreenMirrorMode
import kotlin.math.max

internal class AdaptiveQualityMonitor(
    private val getQuality: () -> DScreenMirrorQuality,
    private val getFirstPeerSession: () -> WebRtcPeerSession?,
    private val onAdapt: (resolutionChanged: Boolean) -> Unit,
) {
    var adaptiveResolution: Int = 1080
    var targetFps = 20
    var minFrameIntervalNs = 1_000_000_000L / targetFps
    var lastFrameTimeNs = 0L

    private var statsHandler: Handler? = null
    private val statsIntervalMs = 3000L

    fun start() {
        stop()
        if (getQuality().mode != ScreenMirrorMode.AUTO) return
        statsHandler = Handler(Looper.getMainLooper())
        statsHandler?.postDelayed(object : Runnable {
            override fun run() {
                if (getQuality().mode != ScreenMirrorMode.AUTO) return
                pollStatsAndAdapt()
                statsHandler?.postDelayed(this, statsIntervalMs)
            }
        }, statsIntervalMs)
    }

    fun stop() {
        statsHandler?.removeCallbacksAndMessages(null)
        statsHandler = null
    }

    private fun pollStatsAndAdapt() {
        val session = getFirstPeerSession() ?: return
        session.getStats { availableBitrateKbps, packetLossPercent, rttMs ->
            val oldResolution = adaptiveResolution
            val oldFps = targetFps

            // Aggressive ABR — react to packet loss first
            if (packetLossPercent > 2.0) {
                if (targetFps > 10) {
                    targetFps = max(10, (targetFps * 0.7).toInt())
                    minFrameIntervalNs = 1_000_000_000L / targetFps
                }
                if (packetLossPercent > 5.0 && adaptiveResolution > 720) {
                    adaptiveResolution = 720
                }
            }

            // RTT-based degradation
            if (rttMs > 150) {
                if (targetFps > 15) {
                    targetFps = 15
                    minFrameIntervalNs = 1_000_000_000L / targetFps
                }
                if (rttMs > 300 && adaptiveResolution > 720) adaptiveResolution = 720
            }

            // Bandwidth-based degradation
            if (availableBitrateKbps in 1 until 2000) {
                if (adaptiveResolution > 720) adaptiveResolution = 720
                if (targetFps > 15) {
                    targetFps = 15
                    minFrameIntervalNs = 1_000_000_000L / targetFps
                }
            }

            // Upgrade when network is healthy
            val isHealthy = availableBitrateKbps > 4000 && packetLossPercent < 1.0 && rttMs < 50
            if (isHealthy) {
                if (adaptiveResolution < 1080) adaptiveResolution = 1080
                if (targetFps < 20) {
                    targetFps = 20
                    minFrameIntervalNs = 1_000_000_000L / targetFps
                }
            }

            if (oldResolution != adaptiveResolution || oldFps != targetFps) {
                LogCat.d(
                    "webrtc: adaptive ${oldResolution}p@${oldFps}fps → ${adaptiveResolution}p@${targetFps}fps " +
                        "(bw=${availableBitrateKbps}kbps loss=${String.format("%.1f", packetLossPercent)}% " +
                        "rtt=${String.format("%.0f", rttMs)}ms)",
                )
                onAdapt(oldResolution != adaptiveResolution)
            }
        }
    }
}
