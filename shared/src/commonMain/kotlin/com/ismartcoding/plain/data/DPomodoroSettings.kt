package com.ismartcoding.plain.data

import com.ismartcoding.plain.ui.page.pomodoro.PomodoroState
import kotlinx.serialization.Serializable

@Serializable
data class DPomodoroSettings(
    val workDuration: Int = 25,
    val shortBreakDuration: Int = 5,
    val longBreakDuration: Int = 15,
    val pomodorosBeforeLongBreak: Int = 4,
    val showNotification: Boolean = true,
    val playSoundOnComplete: Boolean = true,
    val soundPath: String = "",
    val originalSoundName: String = "",
) {
    fun getTotalSeconds(state: PomodoroState): Int {
        return when (state) {
            PomodoroState.WORK -> workDuration * 60
            PomodoroState.SHORT_BREAK -> shortBreakDuration * 60
            PomodoroState.LONG_BREAK -> longBreakDuration * 60
        }
    }

    fun getTimeLeft(state: PomodoroState): Int {
        return when (state) {
            PomodoroState.WORK -> workDuration * 60
            PomodoroState.SHORT_BREAK -> shortBreakDuration * 60
            PomodoroState.LONG_BREAK -> longBreakDuration * 60
        }
    }
}