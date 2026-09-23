package com.ismartcoding.plain.httpserver.mainschemas

import com.ismartcoding.plain.lib.kgraphql.annotations.GraphQLMutation
import com.ismartcoding.plain.lib.kgraphql.annotations.GraphQLQuery
import com.ismartcoding.plain.lib.kgraphql.schema.dsl.SchemaBuilder
import com.ismartcoding.plain.platform.AppDatabase
import com.ismartcoding.plain.events.HPomodoroPauseEvent
import com.ismartcoding.plain.events.HPomodoroStartEvent
import com.ismartcoding.plain.events.HPomodoroStopEvent
import com.ismartcoding.plain.lib.TimeHelper
import com.ismartcoding.plain.lib.sendEvent
import com.ismartcoding.plain.preferences.PomodoroSettingsPreference
import com.ismartcoding.plain.ui.page.pomodoro.PomodoroState
import com.ismartcoding.plain.httpserver.models.PomodoroSettings
import com.ismartcoding.plain.httpserver.models.PomodoroToday
import com.ismartcoding.plain.httpserver.models.pomodoroRuntimeInfoProvider
import com.ismartcoding.plain.httpserver.models.toModel
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime

@GraphQLQuery
suspend fun pomodoroSettings(): PomodoroSettings {
    return PomodoroSettingsPreference.getValueAsync().toModel()
}

@GraphQLQuery
suspend fun pomodoroToday(): PomodoroToday {
    val zone = TimeZone.currentSystemDefault()
    val today = TimeHelper.now().toLocalDateTime(zone).date.atStartOfDayIn(zone)
    val info = pomodoroRuntimeInfoProvider?.invoke()
    return if (info != null) {
        PomodoroToday(
            date = today,
            completedCount = info.completedCount,
            currentRound = info.currentRound,
            timeLeftSec = info.timeLeft,
            totalTimeSec = info.totalTime,
            isRunning = info.isRunning,
            isPaused = info.isPaused,
            state = info.state
        )
    } else {
        PomodoroToday(
            date = today,
            completedCount = 0,
            currentRound = 1,
            timeLeftSec = 0,
            totalTimeSec = 0,
            isRunning = false,
            isPaused = false,
            state = PomodoroState.WORK
        )
    }
}

@GraphQLMutation
suspend fun startPomodoro(durationSec: Int): Boolean {
    sendEvent(HPomodoroStartEvent(durationSec))
    return true
}

@GraphQLMutation
suspend fun pausePomodoro(): Boolean {
    sendEvent(HPomodoroPauseEvent())
    return true
}

@GraphQLMutation
suspend fun stopPomodoro(): Boolean {
    sendEvent(HPomodoroStopEvent())
    return true
}

fun SchemaBuilder.addPomodoroSchema() {
}
