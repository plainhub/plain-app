package com.ismartcoding.plain.web.models

import com.ismartcoding.plain.data.DPomodoroSettings

data class PomodoroSettings(
    val workDuration: Int,
    val shortBreakDuration: Int,
    val longBreakDuration: Int,
    val pomodorosBeforeLongBreak: Int,
    val showNotification: Boolean,
    val playSoundOnComplete: Boolean,
    val soundPath: String,
    val originalSoundName: String
)

fun DPomodoroSettings.toModel(): PomodoroSettings {
    return PomodoroSettings(
        workDuration = workDuration,
        shortBreakDuration = shortBreakDuration,
        longBreakDuration = longBreakDuration,
        pomodorosBeforeLongBreak = pomodorosBeforeLongBreak,
        showNotification = showNotification,
        playSoundOnComplete = playSoundOnComplete,
        soundPath = soundPath,
        originalSoundName = originalSoundName
    )
}