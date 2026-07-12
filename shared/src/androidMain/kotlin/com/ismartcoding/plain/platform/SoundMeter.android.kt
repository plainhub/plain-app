package com.ismartcoding.plain.platform

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.rememberCoroutineScope
import com.ismartcoding.plain.lib.channel.Channel
import com.ismartcoding.plain.events.PermissionsResultEvent
import com.ismartcoding.plain.helpers.SoundMeterHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

@SuppressLint("MissingPermission")
@Composable
actual fun SoundMeterRecorder(
    isRunning: MutableState<Boolean>,
    decibel: MutableFloatState,
    total: MutableFloatState,
    count: MutableIntState,
    min: MutableFloatState,
    avg: MutableFloatState,
    max: MutableFloatState,
) {
    val scope = rememberCoroutineScope()
    val sharedFlow = Channel.sharedFlow
    var audioRecord: AudioRecord? = null

    LaunchedEffect(sharedFlow) {
        sharedFlow.collect { event ->
            if (event is PermissionsResultEvent) {
                isRunning.value = com.ismartcoding.plain.platform.Permission.RECORD_AUDIO.isGranted()
            }
        }
    }

    LaunchedEffect(isRunning.value) {
        if (!isRunning.value) {
            if (audioRecord?.state == AudioRecord.STATE_INITIALIZED) {
                audioRecord?.stop(); audioRecord?.release(); audioRecord = null
            }
            return@LaunchedEffect
        }
        val bufferSize = AudioRecord.getMinBufferSize(44100, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
        val buffer = ShortArray(bufferSize)
        audioRecord = AudioRecord(MediaRecorder.AudioSource.MIC, 44100, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize)
        if (audioRecord?.state == AudioRecord.STATE_INITIALIZED) audioRecord?.startRecording()
        scope.launch(Dispatchers.Default) {
            while (isRunning.value) {
                val ar = audioRecord
                if (ar != null) {
                    val readSize = ar.read(buffer, 0, bufferSize)
                    if (readSize > 0) {
                        val amplitudeValue = SoundMeterHelper.getMaxAmplitude(buffer, readSize)
                        val value = abs(SoundMeterHelper.amplitudeToDecibel(amplitudeValue))
                        if (value.isFinite()) {
                            decibel.floatValue = value; total.floatValue += value; count.intValue++
                            avg.floatValue = total.floatValue / count.intValue
                            if (value > max.floatValue) max.floatValue = value
                            if (value < min.floatValue || min.floatValue == 0f) min.floatValue = value
                        }
                    }
                }
                delay(180)
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            if (audioRecord?.state == AudioRecord.STATE_INITIALIZED) {
                audioRecord?.stop(); audioRecord?.release(); audioRecord = null
            }
        }
    }
}
