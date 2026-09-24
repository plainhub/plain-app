package com.ismartcoding.plain.data

import com.ismartcoding.plain.ui.page.pomodoro.PomodoroState
import kotlinx.serialization.json.JsonNames
import kotlinx.serialization.Serializable

@Serializable
data class DPomodoroSettings(
    /** Lengths are stored in minutes; the unsuffixed names are the legacy persisted keys. */
    @JsonNames("workDuration")
    val workDurationMin: Int = 25,
    @JsonNames("shortBreakDuration")
    val shortBreakDurationMin: Int = 5,
    @JsonNames("longBreakDuration")
    val longBreakDurationMin: Int = 15,
    val pomodorosBeforeLongBreak: Int = 4,
    val showNotification: Boolean = true,
    val playSoundOnComplete: Boolean = true,
    val soundPath: String = "",
    val originalSoundName: String = "",
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