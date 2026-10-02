package com.ismartcoding.plain.platform

import com.ismartcoding.plain.data.DPomodoroSettings
import com.ismartcoding.plain.ui.page.pomodoro.PomodoroState

expect suspend fun showPomodoroNotification(state: PomodoroState, nextState: PomodoroState)

expect suspend fun playPomodoroCompletionSound(settings: DPomodoroSettings)
