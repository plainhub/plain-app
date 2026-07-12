package com.ismartcoding.plain.platform

import com.ismartcoding.plain.data.DPomodoroSettings
import com.ismartcoding.plain.ui.page.pomodoro.PomodoroState

actual suspend fun showPomodoroNotification(state: PomodoroState) {
}

actual suspend fun playPomodoroCompletionSound(settings: DPomodoroSettings) {
}
