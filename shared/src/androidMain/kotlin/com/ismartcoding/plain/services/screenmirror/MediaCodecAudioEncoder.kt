package com.ismartcoding.plain.services.screenmirror

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioPlaybackCaptureConfiguration
import android.media.AudioRecord
import android.media.MediaCodec
import android.media.MediaFormat
import android.media.projection.MediaProjection
import android.util.Log
import com.ismartcoding.plain.platform.isQPlus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * System audio (playback-capture). PCM frames are fed into MediaCodec Opus
 * encoder; onEncoded delivers opus packets.
 *
 * Threading contract: [codec] and [record] are owned exclusively by the single
 * [loop] coroutine — the only place they are ever touched. stop() only cancels
 * the scope; actual release happens in the scope's completion handler, which
 * kotlinx.coroutines runs strictly after the loop has exited. No concurrent
 * codec access is possible, so teardown needs no join, timeout or retry.
 */
class MediaCodecAudioEncoder(
    private val context: Context,
    private val projection: MediaProjection,
    private val sampleRate: Int = 48_000,
    private val channelCount: Int = 2,
    private val bitrateBps: Int = 64_000,
) {
    companion object {
        private const val TAG = "MirrorAudio"
        private const val MIME = "audio/opus"
        private const val IDLE_POLL_MS = 10L
    }

    private val scopeJob = SupervisorJob()
    private val scope = CoroutineScope(scopeJob + Dispatchers.IO)
    private var codec: MediaCodec? = null
    private var record: AudioRecord? = null

    var onEncoded: ((opusBytes: ByteArray, pts: Long) -> Unit)? = null

    init {
        scopeJob.invokeOnCompletion { releaseResources() }
    }

    @SuppressLint("MissingPermission")
    fun start() {
        if (!isQPlus()) {
            Log.w(TAG, "playback-capture requires Android 10+, skipping")
            return
        }
        if (context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "RECORD_AUDIO not granted, skipping audio")
            return
        }
        val channelConfig = if (channelCount == 1) AudioFormat.CHANNEL_IN_MONO else AudioFormat.CHANNEL_IN_STEREO
        val encoding = AudioFormat.ENCODING_PCM_16BIT
        val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, encoding).coerceAtLeast(4096) * 2

        val playbackConfig = AudioPlaybackCaptureConfiguration.Builder(projection)
            .addMatchingUsage(AudioAttributes.USAGE_MEDIA)
            .addMatchingUsage(AudioAttributes.USAGE_GAME)
            .addMatchingUsage(AudioAttributes.USAGE_UNKNOWN)
            .build()
        val ar = AudioRecord.Builder()
            .setAudioPlaybackCaptureConfig(playbackConfig)
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(sampleRate)
                    .setChannelMask(channelConfig)
                    .setEncoding(encoding)
                    .build(),
            )
            .setBufferSizeInBytes(bufferSize)
            .build()
        if (ar.state != AudioRecord.STATE_INITIALIZED) {
            Log.e(TAG, "AudioRecord init failed state=${ar.state}")
            ar.release()
            return
        }

        val format = MediaFormat.createAudioFormat(MIME, sampleRate, channelCount).apply {
            setInteger(MediaFormat.KEY_BIT_RATE, bitrateBps)
            setInteger(MediaFormat.KEY_PCM_ENCODING, encoding)
        }
        val c = try {
            MediaCodec.createEncoderByType(MIME)
        } catch (e: Exception) {
            Log.e(TAG, "Opus encoder not available: ${e.message}")
            ar.release()
            return
        }
        c.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        c.start()
        if (scopeJob.isCancelled) {
            // stop() raced start(); publish then let the idempotent release clean up
            codec = c
            record = ar
            releaseResources()
            return
        }
        record = ar
        codec = c
        ar.startRecording()
        Log.d(TAG, "started ${sampleRate}Hz ${channelCount}ch ${bitrateBps / 1000}kbps opus")
        scope.launch { loop(ar, c, bufferSize) }
    }

    fun stop() {
        scopeJob.cancel()
        Log.d(TAG, "stopped")
    }

    /**
     * Sole owner of [ar] and [c]. Greedy non-blocking feed + drain in one
     * thread (same shape as the Mp4Helper transcode loop). All blocking waits
     * are bounded: [AudioRecord.READ_NON_BLOCKING] returns immediately, codec
     * dequeues use 0us timeouts, and idle backoff is [IDLE_POLL_MS] — so the
     * coroutine observes cancellation within one poll cycle and exits.
     */
    private suspend fun loop(ar: AudioRecord, c: MediaCodec, bufferSize: Int) {
        val pcm = ByteArray(bufferSize)
        val info = MediaCodec.BufferInfo()
        val bytesPerSecond = sampleRate * channelCount * 2L
        var offset = 0
        var pending = 0
        var fedBytes = 0L
        while (scope.isActive) {
            try {
                var progressed = false
                if (pending == 0) {
                    val read = ar.read(pcm, 0, pcm.size, AudioRecord.READ_NON_BLOCKING)
                    if (read < 0) return
                    offset = 0
                    pending = read
                }
                while (pending > 0) {
                    val idx = c.dequeueInputBuffer(0)
                    if (idx < 0) break
                    val inBuf = c.getInputBuffer(idx) ?: break
                    val chunk = minOf(pending, inBuf.remaining())
                    inBuf.put(pcm, offset, chunk)
                    c.queueInputBuffer(idx, 0, chunk, fedBytes * 1_000_000L / bytesPerSecond, 0)
                    fedBytes += chunk
                    offset += chunk
                    pending -= chunk
                    progressed = true
                }
                while (true) {
                    val idx = c.dequeueOutputBuffer(info, 0)
                    when {
                        idx == MediaCodec.INFO_TRY_AGAIN_LATER -> break
                        idx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> progressed = true
                        idx >= 0 -> {
                            val buf = c.getOutputBuffer(idx)
                            if (buf != null && info.size > 0 && info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG == 0) {
                                val data = ByteArray(info.size)
                                buf.position(info.offset)
                                buf.get(data, 0, info.size)
                                onEncoded?.invoke(data, info.presentationTimeUs)
                            }
                            c.releaseOutputBuffer(idx, false)
                            if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) return
                            progressed = true
                        }
                    }
                }
                if (!progressed) delay(IDLE_POLL_MS)
            } catch (e: IllegalStateException) {
                // codec or record failed internally; exit and let release happen via scope completion
                return
            }
        }
    }

    private fun releaseResources() {
        try { codec?.release() } catch (_: Exception) {}
        codec = null
        try { record?.release() } catch (_: Exception) {}
        record = null
    }
}
