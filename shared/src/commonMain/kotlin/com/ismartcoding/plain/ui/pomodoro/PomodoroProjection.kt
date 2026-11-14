package com.ismartcoding.plain.ui.pomodoro

import androidx.compose.runtime.mutableStateOf
import com.ismartcoding.plain.models.PomodoroAction
import com.ismartcoding.plain.models.PomodoroToday

import com.ismartcoding.plain.models.PomodoroState

import com.ismartcoding.plain.features.pomodoro.PomodoroClient
import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.Prefs
import com.ismartcoding.plain.ui.platform.media.playPomodoroCompletionSound
import com.ismartcoding.plain.ui.platform.media.showPomodoroNotification
import kotlinx.coroutines.CancellationException

internal object PomodoroProjection {
    val today = mutableStateOf<PomodoroToday?>(null)
    suspend fun refresh() {
        today.value = PomodoroClient.today()
    }

    suspend fun apply(event: PomodoroAction) {
        today.value = event.today
        val completed = event.completedState ?: return
        val state = PomodoroState.valueOf(completed)
        try {
            showPomodoroNotification(state, event.today.state)
        } catch (canceled: CancellationException) {
            throw canceled
        } catch (error: Exception) {
            LogCat.e("Pomodoro notification", error)
        }
        try {
            playPomodoroCompletionSound(Prefs.pomodoroSettings.value)
        } catch (canceled: CancellationException) {
            throw canceled
        } catch (error: Exception) {
            LogCat.e("Pomodoro completion sound", error)
        }
    }
}
