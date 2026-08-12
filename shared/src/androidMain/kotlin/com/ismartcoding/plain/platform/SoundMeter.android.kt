package com.ismartcoding.plain.platform

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import com.ismartcoding.plain.helpers.SoundMeterHelper
import kotlin.math.abs

@SuppressLint("MissingPermission")
actual class SoundMeterDataSource {
    private var audioRecord: AudioRecord? = null
    private var buffer: ShortArray = ShortArray(0)

    actual fun start(): Boolean {
        val sampleRate = 44100
        val channelConfig = AudioFormat.CHANNEL_IN_MONO
        val format = AudioFormat.ENCODING_PCM_16BIT
        val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, format)
        if (bufferSize <= 0) return false

        buffer = ShortArray(bufferSize)
        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            channelConfig,
            format,
            bufferSize
        )

        val record = audioRecord ?: return false
        if (record.state != AudioRecord.STATE_INITIALIZED) {
            release()
            return false
        }
        record.startRecording()
        return true
    }

    actual fun stop() {
        release()
    }

    actual fun getDecibel(): Float {
        val record = audioRecord ?: return Float.NaN
        val readSize = record.read(buffer, 0, buffer.size)
        if (readSize <= 0) return Float.NaN
        val amplitude = SoundMeterHelper.getMaxAmplitude(buffer, readSize)
        return abs(SoundMeterHelper.amplitudeToDecibel(amplitude))
    }

    private fun release() {
        audioRecord?.let {
            if (it.state == AudioRecord.STATE_INITIALIZED) {
                it.stop()
                it.release()
            }
        }
        audioRecord = null
    }
}
