package com.ismartcoding.plain.ui.models

import android.content.Context
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ismartcoding.lib.channel.sendEvent
import com.ismartcoding.plain.data.DPomodoroSettings
import com.ismartcoding.plain.db.AppDatabase
import com.ismartcoding.plain.db.DPomodoroItem
import com.ismartcoding.plain.events.CancelPomodoroTimerEvent
import com.ismartcoding.plain.events.PomodoroTimerEvent
import com.ismartcoding.plain.preference.PomodoroSettingsPreference
import com.ismartcoding.plain.ui.page.pomodoro.PomodoroState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

class PomodoroViewModel : ViewModel() {

    // Constants
    companion object {
        private const val DEFAULT_WORK_DURATION = 25 * 60 // 25 minutes in seconds
    }

    // State variables
    var currentState = mutableStateOf(PomodoroState.WORK)
    var isRunning = mutableStateOf(false)
    var adjustJob = mutableStateOf<Job?>(null)
    var isPaused = mutableStateOf(false)
    var timeLeft = mutableIntStateOf(DEFAULT_WORK_DURATION)
    var completedCount = mutableIntStateOf(0)
    var currentRound = mutableIntStateOf(1)
    var settings = mutableStateOf(DPomodoroSettings())

    val showSettings = mutableStateOf(false)
    var todayRecord = mutableStateOf<DPomodoroItem?>(null)

    private val pomodoroDao = AppDatabase.instance.pomodoroItemDao()
    private var timerJob: Job? = null
    private var eventHandler: Job? = null

    suspend fun loadAsync(context: Context) {
        val today = getCurrentDateString()
        todayRecord.value = pomodoroDao.getByDate(today)
        completedCount.intValue = todayRecord.value?.completedCount ?: 0
        settings.value = PomodoroSettingsPreference.getValueAsync(context)
        updateTimeForCurrentState()
    }

    fun startSession() {
        isRunning.value = true
        isPaused.value = false
        sendEvent(PomodoroTimerEvent(timeLeft.intValue, currentState.value))
        startCountdownTimer()
    }

    private fun startCountdownTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch(Dispatchers.IO) {
            while (isRunning.value && !isPaused.value && timeLeft.intValue > 0) {
                delay(1000L) // 1 second interval
                if (isRunning.value && !isPaused.value && timeLeft.intValue > 0) {
                    timeLeft.intValue--
                }
            }
        }
    }

    fun pauseSession() {
        stopTimer()
        isRunning.value = false
        isPaused.value = true
    }

    fun resetTimer() {
        stopTimer()
        resetToInitialState()
    }


    private fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
        sendEvent(CancelPomodoroTimerEvent())
    }

    private fun resetToInitialState() {
        isPaused.value = false
        isRunning.value = false
        currentState.value = PomodoroState.WORK
        updateTimeForCurrentState()
    }

    fun updateTimeForCurrentState() {
        timeLeft.intValue = settings.value.getTimeLeft(currentState.value)
    }

    suspend fun handleWorkSessionCompleteAsync(isSkip: Boolean) {
        val newCount = if (isSkip) completedCount.intValue else completedCount.intValue + 1
        completedCount.intValue = newCount

        if (!isSkip) {
            updateDailyRecord(newCount, settings.value.workDuration * 60)
        }

        currentState.value = if (shouldTakeLongBreak(newCount)) {
            PomodoroState.LONG_BREAK
        } else {
            PomodoroState.SHORT_BREAK
        }
        updateTimeForCurrentState()
    }

    private fun shouldTakeLongBreak(completedCount: Int): Boolean {
        return completedCount % settings.value.pomodorosBeforeLongBreak == 0 && completedCount > 0
    }

    fun handleBreakSessionComplete() {
        currentState.value = PomodoroState.WORK
        updateTimeForCurrentState()
        currentRound.intValue += 1
    }

    fun resetSessionState() {
        stopTimer()
        isRunning.value = false
        isPaused.value = false
    }

    private suspend fun updateDailyRecord(completedPomodoros: Int, workSeconds: Int) {
        val today = getCurrentDateString()
        val record = pomodoroDao.getByDate(today) ?: createNewRecord(today)

        record.apply {
            this.completedCount = completedPomodoros
            this.totalWorkSeconds += workSeconds
            this.updatedAt = Clock.System.now()
        }
        pomodoroDao.update(record)

        // Update local state
        todayRecord.value = record
    }

    private fun createNewRecord(date: String): DPomodoroItem {
        val record = DPomodoroItem().apply {
            this.date = date
            this.completedCount = 0
            this.totalWorkSeconds = 0
        }
        pomodoroDao.insert(record)
        return record
    }

    private fun getCurrentDateString(): String {
        return Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date.toString()
    }

    fun formatTime(seconds: Int): String {
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60
        return "%02d:%02d".format(minutes, remainingSeconds)
    }

    fun getTotalSeconds(): Int {
        return settings.value.getTotalSeconds(currentState.value)
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
        eventHandler?.cancel()
        sendEvent(CancelPomodoroTimerEvent())
    }
}