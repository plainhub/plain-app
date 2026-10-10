package com.ismartcoding.plain.features

import com.ismartcoding.plain.events.HPomodoroChangedEvent
import com.ismartcoding.plain.lib.sendEvent
import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.platform.showPomodoroNotification
import com.ismartcoding.plain.platform.playPomodoroCompletionSound
import com.ismartcoding.plain.preferences.UserPrefs
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

object PomodoroHost {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val lock = Mutex()
    private var timer: Job? = null

    suspend fun refresh(): Unit = lock.withLock {
        val today = PomodoroHelper.today()
        sendEvent(HPomodoroChangedEvent(today))
        if (today.isRunning && timer?.isActive != true) {
            timer = scope.launch { runTimer() }
        }
    }

    private suspend fun runTimer() {
        try {
            while (currentCoroutineContext().isActive) {
                delay(1000)
                try {
                    val (state, completed) = PomodoroHelper.tick()
                    sendEvent(HPomodoroChangedEvent(state))
                    if (completed != null) {
                        val settings = UserPrefs.pomodoroSettingsValue()
                        showPomodoroNotification(completed, state.state)
                        playPomodoroCompletionSound(settings)
                    }
                    if (!state.isRunning) break
                } catch (cancelled: CancellationException) { throw cancelled }
                catch (e: Exception) { LogCat.e("Pomodoro host: ${e.message}"); delay(4000) }
            }
        } finally {
            lock.withLock { timer = null }
            try { refresh() }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (e: Exception) { LogCat.e("Pomodoro refresh: ${e.message}") }
        }
    }
}
