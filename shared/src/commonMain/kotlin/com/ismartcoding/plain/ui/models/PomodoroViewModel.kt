package com.ismartcoding.plain.ui.models

import com.ismartcoding.plain.preferences.*
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ismartcoding.plain.data.DPomodoroSettings
import com.ismartcoding.plain.features.PomodoroHelper
import com.ismartcoding.plain.ui.page.pomodoro.PomodoroToday
import com.ismartcoding.plain.db.DPomodoroItem
import com.ismartcoding.plain.lib.TimeHelper
import com.ismartcoding.plain.ui.page.pomodoro.PomodoroState
import kotlinx.coroutines.Job
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

class PomodoroViewModel : ViewModel() {

    // State variables
    var currentState = mutableStateOf(PomodoroState.WORK)
    var isRunning = mutableStateOf(false)
    var adjustJob = mutableStateOf<Job?>(null)
    var isPaused = mutableStateOf(false)
    var timeLeft = mutableIntStateOf(UserPrefs.pomodoroSettingsValue().workDurationMin * 60)
    var completedCount = mutableIntStateOf(0)
    var currentRound = mutableIntStateOf(1)
    var settings = mutableStateOf(UserPrefs.pomodoroSettingsValue())

    val showSettings = mutableStateOf(false)
    var todayRecord = mutableStateOf<DPomodoroItem?>(null)

    private var totalSeconds = UserPrefs.pomodoroSettingsValue().workDurationMin * 60

    suspend fun loadAsync() {
        settings.value = UserPrefs.pomodoroSettingsValue()
        PomodoroHelper.configureDay(force = true)
        refreshAsync()
        com.ismartcoding.plain.features.PomodoroHost.refresh()
    }
    suspend fun refreshAsync() { applySnapshot(PomodoroHelper.today()) }
    fun applySnapshot(today: PomodoroToday) {
        currentState.value = today.state
        completedCount.intValue = today.completedCount
        currentRound.intValue = today.currentRound
        timeLeft.intValue = today.timeLeftSec
        totalSeconds = today.totalTimeSec
        isRunning.value = today.isRunning
        isPaused.value = today.isPaused
    }
    fun startSession() { viewModelScope.launchSafe { applySnapshot(PomodoroHelper.start(timeLeft.intValue)) } }
    fun pauseSession() { viewModelScope.launchSafe { applySnapshot(PomodoroHelper.pause()) } }
    fun resetTimer() { viewModelScope.launchSafe { applySnapshot(PomodoroHelper.stop()) } }
    fun adjustTime(seconds: Int) { viewModelScope.launchSafe { applySnapshot(PomodoroHelper.adjust(seconds)) } }
    fun updateTimeForCurrentState() { viewModelScope.launchSafe { loadAsync() } }
    internal fun getCurrentDateString(): String = TimeHelper.now().toLocalDateTime(TimeZone.currentSystemDefault()).date.toString()
    fun formatTime(seconds: Int): String = "${(seconds / 60).toString().padStart(2, '0')}:${(seconds % 60).toString().padStart(2, '0')}"
    fun getTotalSeconds(): Int = totalSeconds
}
