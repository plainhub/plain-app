package com.ismartcoding.plain.web.schemas

import com.ismartcoding.plain.lib.kgraphql.schema.dsl.SchemaBuilder
import com.ismartcoding.plain.lib.channel.sendEvent
import com.ismartcoding.plain.db.AppDatabase
import com.ismartcoding.plain.events.HPomodoroPauseEvent
import com.ismartcoding.plain.events.HPomodoroStartEvent
import com.ismartcoding.plain.events.HPomodoroStopEvent
import com.ismartcoding.plain.helpers.TimeHelper
import com.ismartcoding.plain.preferences.PomodoroSettingsPreference
import com.ismartcoding.plain.ui.MainActivity
import com.ismartcoding.plain.web.models.PomodoroToday
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
            val vm = MainActivity.instance.get()!!.pomodoroVM
            PomodoroToday(
                date = today,
                completedCount = vm.completedCount.intValue,
                currentRound = vm.currentRound.intValue,
                timeLeft = vm.timeLeft.intValue,
                totalTime = vm.settings.value.getTotalSeconds(vm.currentState.value),
                isRunning = vm.isRunning.value,
                isPause = vm.isPaused.value,
                state = vm.currentState.value
            )
        }
    }
    mutation("startPomodoro") {
        resolver { timeLeft: Int ->
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
