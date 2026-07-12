package com.ismartcoding.plain.platform

import com.ismartcoding.plain.appContext
import com.ismartcoding.plain.data.DPomodoroSettings
import com.ismartcoding.plain.ui.page.pomodoro.PomodoroHelper
import com.ismartcoding.plain.ui.page.pomodoro.PomodoroState

actual suspend fun showPomodoroNotification(state: PomodoroState) {
    PomodoroHelper.showNotificationAsync(appContext, state)
}

actual suspend fun playPomodoroCompletionSound(settings: DPomodoroSettings) {
    PomodoroHelper.playCompletionSound(appContext, settings)
}
