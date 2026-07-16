package com.ismartcoding.plain.web.schemas

import com.ismartcoding.plain.lib.kgraphql.schema.dsl.SchemaBuilder
import com.ismartcoding.plain.lib.channel.sendEvent
import com.ismartcoding.plain.platform.AppDatabase
import com.ismartcoding.plain.events.HPomodoroPauseEvent
import com.ismartcoding.plain.events.HPomodoroStartEvent
import com.ismartcoding.plain.events.HPomodoroStopEvent
import com.ismartcoding.plain.helpers.TimeHelper
import com.ismartcoding.plain.preferences.PomodoroSettingsPreference
import com.ismartcoding.plain.ui.page.pomodoro.PomodoroState
import com.ismartcoding.plain.web.models.PomodoroRuntimeInfo
import com.ismartcoding.plain.web.models.PomodoroToday
import com.ismartcoding.plain.web.models.pomodoroRuntimeInfoProvider
import com.ismartcoding.plain.web.models.toModel
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

fun SchemaBuilder.addPomodoroSchema() {
    query("pomodoroSettings") {
        resolver { ->
            PomodoroSettingsPreference.getValueAsync().toModel()
        }
    }
    query("pomodoroToday") {
        resolver { ->
            val dao = AppDatabase.instance.pomodoroItemDao()
            val today = TimeHelper.now().toLocalDateTime(TimeZone.currentSystemDefault()).date.toString()
            val info = pomodoroRuntimeInfoProvider?.invoke()
            if (info != null) {
                PomodoroToday(
                    date = today,
                    completedCount = info.completedCount,
                    currentRound = info.currentRound,
                    timeLeft = info.timeLeft,
                    totalTime = info.totalTime,
                    isRunning = info.isRunning,
                    isPause = info.isPause,
                    state = info.state
                )
            } else {
                PomodoroToday(
                    date = today,
                    completedCount = 0,
                    currentRound = 1,
                    timeLeft = 0,
                    totalTime = 0,
                    isRunning = false,
                    isPause = false,
                    state = PomodoroState.WORK
                )
            }
        }
    }
    mutation("startPomodoro") {
        resolver("timeLeft") { timeLeft: Int ->
            sendEvent(HPomodoroStartEvent(timeLeft))
            true
        }
    }
    mutation("pausePomodoro") {
        resolver { ->
            sendEvent(HPomodoroPauseEvent())
            true
        }
    }
    mutation("stopPomodoro") {
        resolver { ->
            sendEvent(HPomodoroStopEvent())
            true
        }
    }
}
