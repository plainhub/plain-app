package com.ismartcoding.plain.data

import com.ismartcoding.plain.ui.page.pomodoro.PomodoroState
import kotlinx.serialization.Serializable

@Serializable
data class DPomodoroSettings(
    /** Timer lengths are in minutes. */
    val workDurationMin: Int,
    val shortBreakDurationMin: Int,
    val longBreakDurationMin: Int,
    val pomodorosBeforeLongBreak: Int,
    val showNotification: Boolean,
    val playSoundOnComplete: Boolean,
    val soundPath: String,
    val originalSoundName: String,
) {
    fun getTotalSeconds(state: PomodoroState): Int {
        return when (state) {
            PomodoroState.WORK -> workDurationMin * 60
            PomodoroState.SHORT_BREAK -> shortBreakDurationMin * 60
            PomodoroState.LONG_BREAK -> longBreakDurationMin * 60
        }
    }

    fun getTimeLeft(state: PomodoroState): Int {
        return when (state) {
            PomodoroState.WORK -> workDurationMin * 60
            PomodoroState.SHORT_BREAK -> shortBreakDurationMin * 60
            PomodoroState.LONG_BREAK -> longBreakDurationMin * 60
        }
    }
}
