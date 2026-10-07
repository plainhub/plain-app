package com.ismartcoding.plain.httpserver.models

import com.ismartcoding.plain.data.DPomodoroSettings
import com.ismartcoding.plain.ui.page.pomodoro.PomodoroState

data class PomodoroRuntimeInfo(
    val completedCount: Int,
    val currentRound: Int,
    val timeLeft: Int,
    val totalTime: Int,
    val isRunning: Boolean,
    val isPaused: Boolean,
    val state: PomodoroState,
)

var pomodoroRuntimeInfoProvider: (() -> PomodoroRuntimeInfo)? = null
