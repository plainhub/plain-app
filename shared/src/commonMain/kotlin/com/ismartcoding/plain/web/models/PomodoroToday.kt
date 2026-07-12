package com.ismartcoding.plain.web.models

import com.ismartcoding.plain.ui.page.pomodoro.PomodoroState

data class PomodoroToday(
    val date: String,
    val completedCount: Int,
    val currentRound: Int,
    val timeLeft: Int,
    val totalTime: Int,
    val isRunning: Boolean,
    val isPause: Boolean,
    val state: PomodoroState,
)

