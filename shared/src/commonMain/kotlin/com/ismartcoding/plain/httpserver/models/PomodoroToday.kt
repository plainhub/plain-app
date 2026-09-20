package com.ismartcoding.plain.httpserver.models

import com.ismartcoding.plain.lib.kgraphql.annotations.GraphQLType
import com.ismartcoding.plain.ui.page.pomodoro.PomodoroState

@GraphQLType
data class PomodoroToday(
    /** Local calendar date on the device, YYYY-MM-DD. */
    val date: String,
    val completedCount: Int,
    val currentRound: Int,
    val timeLeftSec: Int,
    val totalTimeSec: Int,
    val isRunning: Boolean,
    val isPaused: Boolean,
    val state: PomodoroState,
)

