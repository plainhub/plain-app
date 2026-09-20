package com.ismartcoding.plain.ui.page.pomodoro

import com.ismartcoding.plain.lib.JsonHelper
import com.ismartcoding.plain.events.EventType
import com.ismartcoding.plain.events.PomodoroActionData
import com.ismartcoding.plain.events.WebSocketEvent
import com.ismartcoding.plain.lib.sendEvent
import com.ismartcoding.plain.ui.models.PomodoroViewModel

internal fun sendPomodoroAction(action: String, vm: PomodoroViewModel) {
    sendEvent(
        WebSocketEvent(
            EventType.POMODORO_ACTION, JsonHelper.jsonEncode(
                PomodoroActionData(
                    action, timeLeftSec = vm.timeLeft.intValue,
                    totalTimeSec = vm.settings.value.getTotalSeconds(vm.currentState.value),
                    completedCount = vm.completedCount.intValue, round = vm.currentRound.value, state = vm.currentState.value
                )
            )
        )
    )
}
