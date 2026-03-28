package com.ismartcoding.plain.ui.page.tools

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
import androidx.compose.ui.platform.LocalContext
import com.ismartcoding.lib.channel.Channel
import com.ismartcoding.plain.events.PermissionsResultEvent
import com.ismartcoding.plain.features.Permission
import com.ismartcoding.plain.helpers.SoundMeterHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

@SuppressLint("MissingPermission")
@Composable
internal fun SoundMeterRecorder(
    audioRecord: MutableState<AudioRecord?>,
    isRunning: MutableState<Boolean>,
    decibel: MutableFloatState,
    total: MutableFloatState,
    count: MutableIntState,
    min: MutableFloatState,
    avg: MutableFloatState,
    max: MutableFloatState,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sharedFlow = Channel.sharedFlow

    LaunchedEffect(sharedFlow) {
        sharedFlow.collect { event ->
            if (event is PermissionsResultEvent) {
                isRunning.value = Permission.RECORD_AUDIO.can(context)
            }
        }
    }

    LaunchedEffect(isRunning.value) {
        if (!isRunning.value) {
            if (audioRecord.value?.state == AudioRecord.STATE_INITIALIZED) {
                audioRecord.value?.stop(); audioRecord.value?.release(); audioRecord.value = null
            }
            return@LaunchedEffect
        }
        val bufferSize = AudioRecord.getMinBufferSize(44100, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
        val buffer = ShortArray(bufferSize)
        audioRecord.value = AudioRecord(MediaRecorder.AudioSource.MIC, 44100, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize)
        if (audioRecord.value?.state == AudioRecord.STATE_INITIALIZED) audioRecord.value?.startRecording()
        scope.launch(Dispatchers.IO) {
            while (isRunning.value) {
                if (audioRecord.value != null) {
                    val readSize = audioRecord.value!!.read(buffer, 0, bufferSize)
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
            if (audioRecord.value?.state == AudioRecord.STATE_INITIALIZED) {
                audioRecord.value?.stop(); audioRecord.value?.release(); audioRecord.value = null
            }
        }
    }
}
