package com.ismartcoding.plain.ui.models

import com.ismartcoding.lib.logcat.LogCat
import com.ismartcoding.plain.MainApp
import com.ismartcoding.plain.db.AppDatabase
import com.ismartcoding.plain.db.DPomodoroItem
import com.ismartcoding.plain.helpers.TimeHelper
import com.ismartcoding.plain.ui.page.pomodoro.PomodoroHelper
import com.ismartcoding.plain.ui.page.pomodoro.PomodoroState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.lifecycle.viewModelScope

internal fun PomodoroViewModel.startCountdownTimer() {
    cancelTimer()
    timerJob = viewModelScope.launch(Dispatchers.IO) {
        while (isRunning.value && !isPaused.value && timeLeft.intValue > 0) {
            delay(1000L)
            if (isRunning.value && !isPaused.value && timeLeft.intValue > 0) {
                timeLeft.intValue--
            }
        }

        if (isRunning.value && !isPaused.value && timeLeft.intValue <= 0) {
            val context = MainApp.instance
            if (settings.value.showNotification) {
                PomodoroHelper.showNotificationAsync(context, currentState.value)
            }
            try {
                PomodoroHelper.playCompletionSound(context, settings.value)
            } catch (e: Exception) {
                LogCat.e("Failed to play Pomodoro sound: ${e.message}")
            }
            when (currentState.value) {
                PomodoroState.WORK -> handleWorkSessionCompleteAsync(isSkip = false)
                PomodoroState.SHORT_BREAK, PomodoroState.LONG_BREAK -> handleBreakSessionComplete()
            }
            resetSessionState()
        }
    }
}

internal suspend fun PomodoroViewModel.updateDailyRecord(completedPomodoros: Int, workSeconds: Int) {
    val pomodoroDao = AppDatabase.instance.pomodoroItemDao()
    val today = getCurrentDateString()
    val record = pomodoroDao.getByDate(today) ?: run {
        val r = DPomodoroItem().apply {
            this.date = today
            this.completedCount = 0
            this.totalWorkSeconds = 0
        }
        pomodoroDao.insert(r)
        r
    }

    record.apply {
        this.completedCount = completedPomodoros
        this.totalWorkSeconds += workSeconds
        this.updatedAt = TimeHelper.now()
    }
    pomodoroDao.update(record)
    todayRecord.value = record
}
