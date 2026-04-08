package com.ismartcoding.plain.services.webrtc

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioPlaybackCaptureConfiguration
import android.media.AudioRecord
import android.media.projection.MediaProjection
import com.ismartcoding.lib.logcat.LogCat
import com.ismartcoding.plain.enums.AppFeatureType
import org.webrtc.audio.JavaAudioDeviceModule

@SuppressLint("MissingPermission")
internal fun swapToPlaybackCapture(
    context: Context,
    audioDeviceModule: JavaAudioDeviceModule?,
    projection: MediaProjection,
): Boolean {
    if (!AppFeatureType.MIRROR_AUDIO.has()) {
        LogCat.d("webrtc: audio swap skipped, API < Q")
        return false
    }
    if (context.checkSelfPermission(android.Manifest.permission.RECORD_AUDIO)
        != android.content.pm.PackageManager.PERMISSION_GRANTED
    ) {
        LogCat.e("webrtc: RECORD_AUDIO permission not granted, cannot capture audio")
        return false
    }
    try {
        val adm = audioDeviceModule ?: run {
            LogCat.e("webrtc: audioDeviceModule is null")
            return false
        }
        val audioInputField = adm.javaClass.getDeclaredField("audioInput")
        audioInputField.isAccessible = true
        val audioInput = audioInputField.get(adm) ?: run {
            LogCat.e("webrtc: audioInput is null")
            return false
        }
        val audioRecordField = audioInput.javaClass.getDeclaredField("audioRecord")
        audioRecordField.isAccessible = true
        val oldRecord = audioRecordField.get(audioInput) as? AudioRecord ?: run {
            LogCat.e("webrtc: audioRecord is null or not AudioRecord")
            return false
        }

        val sampleRate = oldRecord.sampleRate
        val channelCount = oldRecord.channelCount
        val channelConfig = if (channelCount == 1) AudioFormat.CHANNEL_IN_MONO else AudioFormat.CHANNEL_IN_STEREO
        val encoding = oldRecord.audioFormat
        LogCat.d("webrtc: audio swap - old record: rate=$sampleRate ch=$channelCount encoding=$encoding")

        try { oldRecord.stop() } catch (_: Exception) {}
        oldRecord.release()

        val playbackConfig = AudioPlaybackCaptureConfiguration.Builder(projection)
            .addMatchingUsage(AudioAttributes.USAGE_MEDIA)
            .addMatchingUsage(AudioAttributes.USAGE_GAME)
            .addMatchingUsage(AudioAttributes.USAGE_UNKNOWN)
            .build()
        val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, encoding) * 2
        val newRecord = AudioRecord.Builder()
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

        if (newRecord.state != AudioRecord.STATE_INITIALIZED) {
            LogCat.e("webrtc: Playback-capture AudioRecord failed to initialise (state=${newRecord.state})")
            newRecord.release()
            return false
        }

        audioRecordField.set(audioInput, newRecord)
        newRecord.startRecording()
        LogCat.d("webrtc: audio swap DONE - system audio capture active (rate=$sampleRate ch=$channelCount)")
        return true
    } catch (e: Exception) {
        LogCat.e("webrtc: Failed to swap to playback capture: ${e.javaClass.simpleName}: ${e.message}")
        return false
    }
}
