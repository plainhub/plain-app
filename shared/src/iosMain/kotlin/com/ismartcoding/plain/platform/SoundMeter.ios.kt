package com.ismartcoding.plain.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.MutableState

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
    // iOS sound meter recording not yet implemented
}
