package com.ismartcoding.plain.httpserver.models

import com.ismartcoding.plain.data.DPomodoroSettings
import com.ismartcoding.plain.lib.kgraphql.annotations.GraphQLType

@GraphQLType
data class PomodoroSettings(
    val workDurationMin: Int,
    val shortBreakDurationMin: Int,
    val longBreakDurationMin: Int,
    val pomodorosBeforeLongBreak: Int,
    val showNotification: Boolean,
    val playSoundOnComplete: Boolean,
    val soundPath: String,
    val originalSoundName: String
)

fun DPomodoroSettings.toModel(): PomodoroSettings {
    return PomodoroSettings(
        workDurationMin = workDuration,
        shortBreakDurationMin = shortBreakDuration,
        longBreakDurationMin = longBreakDuration,
        pomodorosBeforeLongBreak = pomodorosBeforeLongBreak,
        showNotification = showNotification,
        playSoundOnComplete = playSoundOnComplete,
        soundPath = soundPath,
        originalSoundName = originalSoundName
    )
}