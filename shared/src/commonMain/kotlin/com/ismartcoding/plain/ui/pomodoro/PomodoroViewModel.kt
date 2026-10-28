package com.ismartcoding.plain.ui.pomodoro

import com.ismartcoding.plain.ui.state.launchSafe

import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ismartcoding.plain.Prefs
import com.ismartcoding.plain.features.pomodoro.PomodoroClient
import com.ismartcoding.plain.models.PomodoroState
import com.ismartcoding.plain.models.PomodoroToday

class PomodoroViewModel : ViewModel() {

    var currentState = mutableStateOf(PomodoroState.WORK)
    var isRunning = mutableStateOf(false)
    var isPaused = mutableStateOf(false)
    var timeLeft = mutableIntStateOf(Prefs.pomodoroSettings.value.workDurationMin * 60)
    var completedCount = mutableIntStateOf(0)
    var currentRound = mutableIntStateOf(1)
    var settings = mutableStateOf(Prefs.pomodoroSettings.value)

    val showSettings = mutableStateOf(false)

    private var totalSeconds = Prefs.pomodoroSettings.value.workDurationMin * 60

    init {
        viewModelScope.launch {
            snapshotFlow { PomodoroProjection.today.value }.filterNotNull().collect { applyToday(it) }
        }
    }

    suspend fun loadAsync() {
        settings.value = Prefs.pomodoroSettings.value
        refreshAsync()
    }
    suspend fun refreshAsync() { PomodoroProjection.refresh() }
    private fun applyToday(today: PomodoroToday) {
        currentState.value = today.state
        completedCount.intValue = today.completedCount
        currentRound.intValue = today.currentRound
        timeLeft.intValue = today.timeLeftSec
        totalSeconds = today.totalTimeSec
        isRunning.value = today.isRunning
        isPaused.value = today.isPaused
    }
    suspend fun startSessionAsync(seconds: Int) { PomodoroProjection.today.value = PomodoroClient.start(seconds) }
    fun startSession() { viewModelScope.launchSafe { startSessionAsync(timeLeft.intValue) } }
    fun pauseSession() { viewModelScope.launchSafe { PomodoroProjection.today.value = PomodoroClient.pause() } }
    fun resetTimer() { viewModelScope.launchSafe { PomodoroProjection.today.value = PomodoroClient.stop() } }
    fun updateTimeForCurrentState() { viewModelScope.launchSafe { loadAsync() } }
    fun formatTime(seconds: Int): String = "${(seconds / 60).toString().padStart(2, '0')}:${(seconds % 60).toString().padStart(2, '0')}"
    fun getTotalSeconds(): Int = totalSeconds
}
