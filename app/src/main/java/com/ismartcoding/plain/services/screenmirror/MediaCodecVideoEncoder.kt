package com.ismartcoding.plain.services.screenmirror

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Surface
import com.ismartcoding.plain.lib.helpers.CoroutinesHelper.coIO
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
    private val frameRate: Int = 30,
    private val bitrateBps: Int = 4_000_000,
    private val iFrameIntervalSec: Int = 1,
    private val profile: Int = MediaCodecInfo.CodecProfileLevel.AVCProfileBaseline,
) {
    companion object {
        private const val TAG = "MirrorCodec"
        const val MIME = "video/avc"
    }

    private var codec: MediaCodec? = null
    private var inputSurface: Surface? = null
    private var outputThread: Job? = null
    private val lock = Any()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile
    private var currentBitrateBps: Int = bitrateBps
    @Volatile
    private var currentFps: Int = frameRate

    var onEncoded: ((nalu: ByteArray, isKeyFrame: Boolean, pts: Long) -> Unit)? = null
    var onCodecConfig: ((configBytes: ByteArray) -> Unit)? = null

    fun start() {
        val format = MediaFormat.createVideoFormat(MIME, width, height).apply {
            setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
            setInteger(MediaFormat.KEY_BIT_RATE, bitrateBps)
            setInteger(MediaFormat.KEY_FRAME_RATE, frameRate)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, iFrameIntervalSec)
            setInteger(MediaFormat.KEY_PROFILE, profile)
        }
        val c = MediaCodec.createEncoderByType(MIME)
        c.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        inputSurface = c.createInputSurface()
        c.start()
        codec = c
        Log.d(TAG, "started ${width}x${height}@${frameRate}fps ${bitrateBps / 1_000_000}Mbps")
        outputThread = scope.launch { drainLoop() }
    }

    fun getInputSurface(): Surface = inputSurface
        ?: error("encoder not started")

    fun setBitrate(bps: Int) {
        synchronized(lock) {
            if (bps == currentBitrateBps) return
            currentBitrateBps = bps
            val b = Bundle().apply { putInt(MediaCodec.PARAMETER_KEY_VIDEO_BITRATE, bps) }
            codec?.setParameters(b)
        }
    }

    fun setFps(fps: Int) {
        synchronized(lock) {
            if (fps == currentFps) return
            currentFps = fps
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val b = Bundle().apply { putInt(MediaFormat.KEY_FRAME_RATE, fps) }
                codec?.setParameters(b)
            }
        }
    }

    fun requestKeyFrame() {
        val b = Bundle().apply { putInt(MediaCodec.PARAMETER_KEY_REQUEST_SYNC_FRAME, 1) }
        codec?.setParameters(b)
    }

    fun stop() {
        outputThread?.cancel()
        outputThread = null
        try { codec?.stop() } catch (_: Exception) {}
        codec?.release()
        codec = null
        try { inputSurface?.release() } catch (_: Exception) {}
        inputSurface = null
        Log.d(TAG, "stopped")
    }

    private suspend fun drainLoop() {
        val info = MediaCodec.BufferInfo()
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
                    idx == MediaCodec.INFO_TRY_AGAIN_LATER -> Unit
                    idx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        val rawSps = c.outputFormat.getByteBuffer("csd-0")
                        val rawPps = c.outputFormat.getByteBuffer("csd-1")
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
                    idx >= 0 -> {
                        val buf = c.getOutputBuffer(idx) ?: continue
                        if (info.size > 0 && info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG == 0) {
                            val data = ByteArray(info.size)
                            buf.position(info.offset)
                            buf.get(data, 0, info.size)
                            val isKey = info.flags and MediaCodec.BUFFER_FLAG_SYNC_FRAME != 0
                            onEncoded?.invoke(H264AnnexB.avccToAnnexB(data), isKey, info.presentationTimeUs)
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

    private fun hex(b: ByteArray): String = b.joinToString("") { "%02x".format(it) }
}
