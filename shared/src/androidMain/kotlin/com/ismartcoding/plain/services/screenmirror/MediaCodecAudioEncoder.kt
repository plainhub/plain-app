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
import android.os.Build
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.nio.ByteBuffer

/**
 * System audio (playback-capture). PCM frames are fed into MediaCodec Opus
 * encoder; onEncoded delivers opus packets.
 *
 * Replaces AudioPlaybackCapture (the WebRTC-internal AudioRecord swap hack).
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
        const val MIME = "audio/opus"
    }

    private var codec: MediaCodec? = null
    private var record: AudioRecord? = null
    private var inputThread: Job? = null
    private var outputThread: Job? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    var onEncoded: ((opusBytes: ByteArray, pts: Long) -> Unit)? = null

    @SuppressLint("MissingPermission")
    fun start() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
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
        record = ar

        val format = MediaFormat.createAudioFormat(MIME, sampleRate, channelCount).apply {
            setInteger(MediaFormat.KEY_BIT_RATE, bitrateBps)
            setInteger(MediaFormat.KEY_PCM_ENCODING, encoding)
        }
        val c = try {
            MediaCodec.createEncoderByType(MIME)
        } catch (e: Exception) {
            Log.e(TAG, "Opus encoder not available: ${e.message}")
            ar.release()
            record = null
            return
        }
        c.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        c.start()
        codec = c
        ar.startRecording()
        Log.d(TAG, "started ${sampleRate}Hz ${channelCount}ch ${bitrateBps / 1000}kbps opus")
        inputThread = scope.launch { feedLoop(ar, bufferSize) }
        outputThread = scope.launch { drainLoop() }
    }

    fun stop() {
        inputThread?.cancel(); inputThread = null
        outputThread?.cancel(); outputThread = null
        try { codec?.release() } catch (_: Exception) {}
        codec = null
        try { record?.release() } catch (_: Exception) {}
        record = null
        scope.cancel()
        Log.d(TAG, "stopped")
    }

    private suspend fun feedLoop(ar: AudioRecord, bufferSize: Int) {
        val pcm = ByteArray(bufferSize)
        var pts = 0L
        val frameUs = (bufferSize * 1_000_000L) / (sampleRate * channelCount * 2)
        while (scope.isActive) {
            val c = codec ?: return
            val read = try {
                ar.read(pcm, 0, pcm.size)
            } catch (e: IllegalStateException) {
                return
            }
            if (read <= 0) return
            var offset = 0
            while (offset < read) {
                val idx = try {
                    c.dequeueInputBuffer(10_000)
                } catch (e: IllegalStateException) {
                    return
                }
                if (idx < 0) break
                val inBuf: ByteBuffer = c.getInputBuffer(idx) ?: break
                val chunk = minOf(read - offset, inBuf.remaining())
                inBuf.put(pcm, offset, chunk)
                try {
                    c.queueInputBuffer(idx, 0, chunk, pts, 0)
                } catch (e: IllegalStateException) {
                    return
                }
                offset += chunk
                pts += frameUs
            }
        }
    }

    private suspend fun drainLoop() {
        val info = MediaCodec.BufferInfo()
        while (scope.isActive) {
            val c = codec ?: return
            val idx = try {
                c.dequeueOutputBuffer(info, 10_000)
            } catch (e: IllegalStateException) {
                return
            } catch (e: Exception) {
                Log.e(TAG, "drain failed: ${e.message}")
                return
            }
            when {
                idx == MediaCodec.INFO_TRY_AGAIN_LATER -> Unit
                idx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                    Log.d(TAG, "output format changed: ${c.outputFormat}")
                }
                idx >= 0 -> {
                    val buf = try {
                        c.getOutputBuffer(idx)
                    } catch (e: IllegalStateException) {
                        return
                    } ?: continue
                    if (info.size > 0 && info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG == 0) {
                        val data = ByteArray(info.size)
                        buf.position(info.offset)
                        buf.get(data, 0, info.size)
                        onEncoded?.invoke(data, info.presentationTimeUs)
                    }
                    try {
                        c.releaseOutputBuffer(idx, false)
                    } catch (e: IllegalStateException) {
                        return
                    }
                    if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) return
                }
            }
        }
    }
}
