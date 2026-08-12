package com.ismartcoding.plain.services.screenmirror

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.media.MediaFormat
import android.os.Bundle
import android.util.Log
import android.view.Surface
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.nio.ByteBuffer

/**
 * H.264 hardware encoder wrapping MediaCodec. Produces annex-b NAL units
 * via [onEncoded] callback. Input is a Surface — caller feeds it via
 * VirtualDisplay.createVirtualDisplay(display, surface), so frames are
 * GPU-direct (no SurfaceTexture readback, no I420 conversion).
 *
 * Replaces WebRtcPeerSession / libwebrtc's encoder.
 */
class MediaCodecVideoEncoder(
    private val width: Int,
    private val height: Int,
    private val frameRate: Int = 60,
    private val bitrateBps: Int = 8_000_000,
    private val iFrameIntervalSec: Int = 10,
    private val repeatPreviousFrameAfterUs: Long = 100_000L,
    private val latency: Int? = 1,
) {
    companion object {
        private const val TAG = "MirrorCodec"
        const val MIME = "video/avc"

        // OverloadDetector thresholds — inlined constants so the detector
        // class stays a simple inner class (Kotlin prohibits companion
        // objects in inner classes).
        private const val OVERLOAD_SAMPLE_FRAMES = 120
        private const val OVERLOAD_MAX_PERIOD_NS = 22_000_000L // 22 ms → ~45 fps
        private const val OVERLOAD_MAX_TIMEOUT_RATIO = 0.30f // ≥ 30 % timeouts

        fun queryEncoderCaps(): EncoderVideoCaps? {
            return try {
                val info = MediaCodecList(MediaCodecList.REGULAR_CODECS).codecInfos
                    .firstOrNull { it.isEncoder && MIME in it.supportedTypes }
                val caps = info?.getCapabilitiesForType(MIME) ?: return null
                val vc = caps.videoCapabilities ?: return null
                EncoderVideoCaps(
                    maxWidth = vc.supportedWidths.upper,
                    maxHeight = vc.supportedHeights.upper,
                    widthAlignment = vc.widthAlignment,
                    heightAlignment = vc.heightAlignment,
                )
            } catch (e: Exception) {
                Log.e(TAG, "queryEncoderCaps failed: ${e.message}")
                null
            }
        }
    }

    /**
     * Encoder overload detector. Accumulates the first sample of frame-period
     * and dequeue-timeout samples, then reports a single one-time verdict
     * via [MediaCodecVideoEncoder.onOverloaded].
     *
     * Designed so drainLoop() never needs to know how detection works —
     * it just calls [recordTimeout] / [recordFramePeriod] every iteration.
     */
    private inner class OverloadDetector {

        private var fired = false
        private var frames = 0
        private var totalPeriodNs = 0L
        private var timeouts = 0
        private var dequeues = 0

        fun recordTimeout() {
            timeouts++
            dequeues++
        }

        fun recordFramePeriod(periodNs: Long) {
            dequeues++
            if (fired) return
            frames++
            totalPeriodNs += periodNs
            if (frames >= OVERLOAD_SAMPLE_FRAMES) evaluate()
        }

        private fun evaluate() {
            fired = true
            val avgPeriodNs = totalPeriodNs / frames
            val timeoutRatio = if (dequeues == 0) 0f else timeouts.toFloat() / dequeues.toFloat()
            val overloaded = avgPeriodNs > OVERLOAD_MAX_PERIOD_NS || timeoutRatio > OVERLOAD_MAX_TIMEOUT_RATIO
            Log.d(
                TAG,
                "overload-check: frames=$frames avgPeriod=${avgPeriodNs / 1_000_000}ms " +
                    "timeoutRatio=${"%.2f".format(timeoutRatio)} overloaded=$overloaded"
            )
            if (overloaded) scope.launch { onOverloaded?.invoke() }
        }
    }

    private var codec: MediaCodec? = null
    private var inputSurface: Surface? = null
    private var outputThread: Job? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val overloadDetector = OverloadDetector()

    var onEncoded: ((nalu: ByteArray, isKeyFrame: Boolean, pts: Long) -> Unit)? = null
    var onCodecConfig: ((configBytes: ByteArray) -> Unit)? = null

    /**
     * Fired once when the encoder cannot sustain real-time output for the
     * current configuration. Caller should reduce resolution / encoder
     * parameters and recreate the encoder.
     *
     * Detection is based on the first 120 frames' actual output cadence —
     * not on build version or any other static property.
     */
    var onOverloaded: (() -> Unit)? = null

    fun start() {
        val format = MediaFormat.createVideoFormat(MIME, width, height).apply {
            setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
            setInteger(MediaFormat.KEY_BIT_RATE, bitrateBps)
            setInteger(MediaFormat.KEY_FRAME_RATE, frameRate)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, iFrameIntervalSec)
            setLong(MediaFormat.KEY_REPEAT_PREVIOUS_FRAME_AFTER, repeatPreviousFrameAfterUs)
            setInteger(MediaFormat.KEY_COLOR_RANGE, MediaFormat.COLOR_RANGE_LIMITED)
            setInteger(MediaFormat.KEY_PRIORITY, 0)
            if (latency != null) {
                setInteger(MediaFormat.KEY_LATENCY, latency)
            }
        }
        val c = MediaCodec.createEncoderByType(MIME)
        c.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        inputSurface = c.createInputSurface()
        c.start()
        codec = c
        Log.d(TAG, "started ${width}x${height}@${frameRate}fps ${bitrateBps / 1_000_000}Mbps iFrame=${iFrameIntervalSec}s")
        outputThread = scope.launch { drainLoop() }
    }

    fun getInputSurface(): Surface = inputSurface
        ?: error("encoder not started")

    fun requestKeyFrame() {
        val b = Bundle().apply { putInt(MediaCodec.PARAMETER_KEY_REQUEST_SYNC_FRAME, 1) }
        codec?.setParameters(b)
    }

    fun stop() {
        outputThread?.cancel()
        outputThread = null
        try {
            codec?.stop()
        } catch (_: Exception) {
        }
        codec?.release()
        codec = null
        try {
            inputSurface?.release()
        } catch (_: Exception) {
        }
        inputSurface = null
        Log.d(TAG, "stopped")
    }

    private suspend fun drainLoop() {
        val info = MediaCodec.BufferInfo()
        var frameCount = 0L
        var keyFrameCount = 0L
        var lastFrameTime = System.nanoTime()
        var dequeueTimeouts = 0
        var totalFrameSize = 0L

        while (scope.isActive) {
            val c = codec ?: return
            val idx = try {
                c.dequeueOutputBuffer(info, 10_000)
            } catch (e: Exception) {
                Log.e(TAG, "dequeue failed: ${e.message}")
                return
            }
            try {
                when {
                    idx == MediaCodec.INFO_TRY_AGAIN_LATER -> {
                        dequeueTimeouts++
                        overloadDetector.recordTimeout()
                    }
                    idx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        emitCodecConfig(c.outputFormat)
                    }
                    idx >= 0 -> {
                        val now = System.nanoTime()
                        val sinceLast = now - lastFrameTime
                        lastFrameTime = now
                        dequeueTimeouts = 0
                        overloadDetector.recordFramePeriod(sinceLast)
                        processOutputBuffer(c, idx, info, sinceLast).also {
                            if (it.encoded) {
                                frameCount++
                                totalFrameSize += it.size
                                if (it.isKey) keyFrameCount++
                                if (frameCount % 60 == 0L) logPeriodicStats(frameCount, keyFrameCount, totalFrameSize, sinceLast)
                            }
                        }
                        c.releaseOutputBuffer(idx, false)
                        if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) return
                    }
                }
            } catch (e: IllegalStateException) {
                Log.e(TAG, "codec stopped mid-iteration: ${e.message}")
                return
            }
        }
    }

    private data class BufferResult(val encoded: Boolean, val size: Long, val isKey: Boolean)

    private fun processOutputBuffer(c: MediaCodec, idx: Int, info: MediaCodec.BufferInfo, sinceLast: Long): BufferResult {
        val buf = c.getOutputBuffer(idx) ?: return BufferResult(false, 0, false)
        if (info.size <= 0) return BufferResult(false, 0, false)
        val isConfig = info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0
        val isKey = info.flags and MediaCodec.BUFFER_FLAG_SYNC_FRAME != 0
        if (!isConfig || isKey) {
            val data = ByteArray(info.size)
            buf.position(info.offset)
            buf.get(data, 0, info.size)
            onEncoded?.invoke(H264AnnexB.avccToAnnexB(data), isKey, info.presentationTimeUs)
        }
        return BufferResult(encoded = true, size = info.size.toLong(), isKey = isKey)
    }

    private fun emitCodecConfig(format: MediaFormat) {
        val rawSps = format.getByteBuffer("csd-0")
        val rawPps = format.getByteBuffer("csd-1")
        val sps: ByteArray? = rawSps?.let { ByteArray(it.remaining()).also(it::get) }
        val pps: ByteArray? = rawPps?.let { ByteArray(it.remaining()).also(it::get) }
        Log.d(TAG, "codec-spec raw: sps=${sps?.let { hex(it) }} (${sps?.size}B) pps=${pps?.let { hex(it) }} (${pps?.size}B)")
        if (sps != null && pps != null && sps.isNotEmpty() && pps.isNotEmpty()) {
            val config = H264AnnexB.joinSpsPps(sps, pps)
            onCodecConfig?.invoke(config)
            Log.d(TAG, "annex-B config: ${config.size}B = ${hex(config)}")
        } else {
            Log.d(TAG, "codec-spec unavailable (rawSps=${rawSps?.remaining()} rawPps=${rawPps?.remaining()})")
        }
    }

    private fun logPeriodicStats(frameCount: Long, keyFrameCount: Long, totalFrameSize: Long, sinceLast: Long) {
        val avgSize = totalFrameSize / frameCount
        Log.d(
            TAG,
            "drain stats: frames=${frameCount} keyFrames=${keyFrameCount} " +
                "avgSize=${avgSize}B lastGap=${sinceLast / 1_000_000}ms"
        )
    }

    private fun hex(b: ByteArray): String = b.joinToString("") { "%02x".format(it) }
}

data class EncoderVideoCaps(
    val maxWidth: Int,
    val maxHeight: Int,
    val widthAlignment: Int,
    val heightAlignment: Int,
)
