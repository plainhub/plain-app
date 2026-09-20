package com.ismartcoding.plain.httpserver.models

import com.ismartcoding.plain.lib.kgraphql.annotations.GraphQLField
import com.ismartcoding.plain.lib.kgraphql.annotations.GraphQLType
import com.ismartcoding.plain.ui.page.pomodoro.PomodoroState
import kotlin.time.Instant

@GraphQLType
data class PomodoroToday(
    @GraphQLField(description = "Start of the pomodoro day on the device (UTC instant). Clients derive the calendar date in their own timezone.")
    val date: Instant,
    val completedCount: Int,
    val currentRound: Int,
    val timeLeftSec: Int,
    val totalTimeSec: Int,
    val isRunning: Boolean,
    val isPaused: Boolean,
    val state: PomodoroState,
)
