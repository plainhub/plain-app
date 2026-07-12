package com.ismartcoding.plain.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.MutableState

/**
 * Records microphone audio and computes decibel values in real time.
 *
 * @param isRunning mutable flag controlling recording start/stop
 * @param decibel output: current decibel reading
 * @param total output: running sum of decibel values
 * @param count output: number of samples taken
 * @param min output: minimum decibel value
 * @param avg output: average decibel value
 * @param max output: maximum decibel value
 */
@Composable
expect fun SoundMeterRecorder(
    isRunning: MutableState<Boolean>,
    decibel: MutableFloatState,
    total: MutableFloatState,
    count: MutableIntState,
    min: MutableFloatState,
    avg: MutableFloatState,
    max: MutableFloatState,
)
