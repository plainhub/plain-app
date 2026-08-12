package com.ismartcoding.plain.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.rememberCoroutineScope
import com.ismartcoding.plain.events.PermissionsResultEvent
import com.ismartcoding.plain.lib.Channel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Platform-specific sound meter data source. Each platform provides raw
 * audio recording and decibel measurement; commonMain handles state, polling,
 * and statistics.
 */
expect class SoundMeterDataSource() {
    fun start(): Boolean
    fun stop()
    fun getDecibel(): Float
}

/**
 * Records microphone audio and computes decibel values in real time.
 *
 * All shared logic (polling loop, statistics, permission handling) lives here
 * in commonMain. Platform-specific recording is delegated to [SoundMeterDataSource].
 */
@Composable
fun SoundMeterRecorder(
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
    val dataSource = SoundMeterDataSource()

    LaunchedEffect(sharedFlow) {
        sharedFlow.collect { event ->
            if (event is PermissionsResultEvent) {
                isRunning.value = Permission.RECORD_AUDIO.isGranted()
            }
        }
    }

    LaunchedEffect(isRunning.value) {
        if (!isRunning.value) {
            dataSource.stop()
            return@LaunchedEffect
        }

        if (!dataSource.start()) {
            isRunning.value = false
            return@LaunchedEffect
        }

        scope.launch(Dispatchers.Default) {
            while (isRunning.value) {
                val value = dataSource.getDecibel()
                if (value.isFinite() && value > 0f) {
                    decibel.floatValue = value
                    total.floatValue += value
                    count.intValue++
                    avg.floatValue = total.floatValue / count.intValue
                    if (value > max.floatValue) max.floatValue = value
                    if (value < min.floatValue || min.floatValue == 0f) min.floatValue = value
                }
                delay(180)
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            dataSource.stop()
        }
    }
}
