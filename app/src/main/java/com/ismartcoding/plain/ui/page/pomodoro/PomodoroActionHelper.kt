package com.ismartcoding.plain.ui.page.pomodoro

import com.ismartcoding.lib.channel.sendEvent
import com.ismartcoding.lib.helpers.JsonHelper
import com.ismartcoding.plain.events.EventType
import com.ismartcoding.plain.events.PomodoroActionData
import com.ismartcoding.plain.events.WebSocketEvent
import com.ismartcoding.plain.ui.models.PomodoroViewModel

internal fun sendPomodoroAction(action: String, vm: PomodoroViewModel) {
    sendEvent(
        WebSocketEvent(
            EventType.POMODORO_ACTION, JsonHelper.jsonEncode(
                PomodoroActionData(
                    action, vm.timeLeft.intValue,
                    vm.settings.value.getTotalSeconds(vm.currentState.value),
                    vm.completedCount.intValue, vm.currentRound.value, vm.currentState.value
                )
            )
        )
    )
}
