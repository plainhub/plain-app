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
) {
    companion object {
        private const val TAG = "MirrorCodec"
        const val MIME = "video/avc"

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

    private var codec: MediaCodec? = null
    private var inputSurface: Surface? = null
    private var outputThread: Job? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    var onEncoded: ((nalu: ByteArray, isKeyFrame: Boolean, pts: Long) -> Unit)? = null
    var onCodecConfig: ((configBytes: ByteArray) -> Unit)? = null

    fun start() {
        val format = MediaFormat.createVideoFormat(MIME, width, height).apply {
            setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
            setInteger(MediaFormat.KEY_BIT_RATE, bitrateBps)
            setInteger(MediaFormat.KEY_FRAME_RATE, frameRate)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, iFrameIntervalSec)
            setLong(MediaFormat.KEY_REPEAT_PREVIOUS_FRAME_AFTER, 100_000L)
            setInteger(MediaFormat.KEY_COLOR_RANGE, MediaFormat.COLOR_RANGE_LIMITED)
            setInteger(MediaFormat.KEY_PRIORITY, 0)
            setInteger(MediaFormat.KEY_LATENCY, 1)
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
                        if (info.size > 0) {
                            val isConfig = info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0
                            val isKey = info.flags and MediaCodec.BUFFER_FLAG_SYNC_FRAME != 0
                            // Some encoders (Qualcomm/Xiaomi) bundle SPS+PPS+IDR in one
                            // buffer with CODEC_CONFIG|SYNC_FRAME flags. Skipping these
                            // drops the IDR — the decoder then only gets P-frames and
                            // produces mosaic/garbage output. Only skip pure config
                            // buffers (already handled via FORMAT_CHANGED); keep any
                            // buffer that carries a sync frame.
                            if (!isConfig || isKey) {
                                val data = ByteArray(info.size)
                                buf.position(info.offset)
                                buf.get(data, 0, info.size)
                                onEncoded?.invoke(H264AnnexB.avccToAnnexB(data), isKey, info.presentationTimeUs)
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

    private fun hex(b: ByteArray): String = b.joinToString("") { "%02x".format(it) }
}

data class EncoderVideoCaps(
    val maxWidth: Int,
    val maxHeight: Int,
    val widthAlignment: Int,
    val heightAlignment: Int,
)
