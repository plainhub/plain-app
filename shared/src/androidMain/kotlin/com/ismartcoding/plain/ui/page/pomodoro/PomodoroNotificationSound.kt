package com.ismartcoding.plain.ui.page.pomodoro

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.sin

internal fun playNotificationSound() {
    val sampleRate = 44100
    val durationMs = 300
    val samples = (sampleRate * durationMs / 1000.0).toInt()

    val audioBuffer = ShortArray(samples)

    for (i in 0 until samples) {
        val time = i.toDouble() / sampleRate

        val startFreq = 800.0
        val endFreq = 600.0
        val modulationTime = 0.1

        val frequency = if (time <= modulationTime) {
            val ratio = endFreq / startFreq
            startFreq * exp(ln(ratio) * (time / modulationTime))
        } else {
            endFreq
        }

        val startGain = 0.3
        val endGain = 0.01
        val gainRatio = endGain / startGain
        val gain = startGain * exp(ln(gainRatio) * (time / (durationMs / 1000.0)))

        val sample = (sin(2 * PI * frequency * time) * gain * Short.MAX_VALUE).toInt()
        audioBuffer[i] = sample.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
    }

    val audioAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()

    val audioFormat = AudioFormat.Builder()
        .setSampleRate(sampleRate)
        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
        .build()

    val bufferSize = AudioTrack.getMinBufferSize(
        sampleRate,
        AudioFormat.CHANNEL_OUT_MONO,
        AudioFormat.ENCODING_PCM_16BIT,
    )

    val audioTrack = AudioTrack.Builder()
        .setAudioAttributes(audioAttributes)
        .setAudioFormat(audioFormat)
        .setBufferSizeInBytes(maxOf(bufferSize, audioBuffer.size * 2))
        .setTransferMode(AudioTrack.MODE_STATIC)
        .build()

    audioTrack.write(audioBuffer, 0, audioBuffer.size)
    audioTrack.setNotificationMarkerPosition(audioBuffer.size)
    audioTrack.setPlaybackPositionUpdateListener(object : AudioTrack.OnPlaybackPositionUpdateListener {
        override fun onMarkerReached(track: AudioTrack?) {
            track?.stop()
            track?.release()
        }

        override fun onPeriodicNotification(track: AudioTrack?) {}
    })

    audioTrack.play()
}
