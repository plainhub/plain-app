package com.ismartcoding.plain.ui.page.pomodoro
import com.ismartcoding.plain.ui.helpers.AppResources

import com.ismartcoding.plain.i18n.*
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.ismartcoding.plain.lib.extensions.getFinalPath
import com.ismartcoding.plain.lib.extensions.isAudioFast
import com.ismartcoding.plain.helpers.withIO
import com.ismartcoding.plain.helpers.coIO
import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.Constants
import com.ismartcoding.plain.audio.DPlaylistAudio
import com.ismartcoding.plain.data.DPomodoroSettings
import com.ismartcoding.plain.platform.AppDatabase
import com.ismartcoding.plain.audio.AudioPlayer
import com.ismartcoding.plain.platform.Permission
import com.ismartcoding.plain.platform.isGranted
import com.ismartcoding.plain.platform.LocaleHelper
import com.ismartcoding.plain.helpers.NotificationHelper
import com.ismartcoding.plain.helpers.TimeHelper
import com.ismartcoding.plain.preferences.PomodoroSettingsPreference
import com.ismartcoding.plain.ui.MainActivity
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.io.File

object PomodoroHelper {
    @SuppressLint("MissingPermission")
    suspend fun showNotificationAsync(context: Context, state: PomodoroState) {
        val settings = PomodoroSettingsPreference.getValueAsync()
        if (!settings.showNotification) {
            return
        }

        NotificationHelper.ensureDefaultChannel()
        val database = AppDatabase.Companion.instance
        val pomodoroDao = database.pomodoroItemDao()
        val today = TimeHelper.now()
            .toLocalDateTime(TimeZone.currentSystemDefault()).date.toString()
        val todayRecord = withIO { pomodoroDao.getByDate(today) }
        val completedPomodoros = todayRecord?.completedCount ?: 0

        // Determine notification content based on current state
        val (title, message) = when (state) {
            PomodoroState.WORK -> {
                val newCount = completedPomodoros + 1
                val shouldBeLongBreak = newCount % settings.pomodorosBeforeLongBreak == 0 && newCount > 0
                val messageRes = if (shouldBeLongBreak) Res.string.great_job_long_break else Res.string.great_job_short_break
                Pair(
                    LocaleHelper.getStringAsync(Res.string.work_session_complete),
                    LocaleHelper.getStringAsync(messageRes)
                )
            }

            PomodoroState.SHORT_BREAK -> {
                Pair(LocaleHelper.getStringAsync(Res.string.break_complete), LocaleHelper.getStringAsync(Res.string.time_to_work))
            }

            PomodoroState.LONG_BREAK -> {
                Pair(LocaleHelper.getStringAsync(Res.string.long_break_complete), LocaleHelper.getStringAsync(Res.string.ready_for_work))
            }
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            `package` = context.packageName
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val notificationId = NotificationHelper.generateId()
        val notification = NotificationCompat.Builder(context, Constants.NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(AppResources.drawable("notification"))
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        try {
            if (Permission.POST_NOTIFICATIONS.isGranted()) {
                NotificationManagerCompat.from(context).notify(notificationId, notification)
            }
        } catch (e: Exception) {
            LogCat.e("Failed to show Pomodoro notification: ${e.message}")
        }
    }

    suspend fun playCompletionSound(context: Context, settings: DPomodoroSettings) {
        // First check if sound should be played at all
        if (!settings.playSoundOnComplete) {
            return
        }
        
        if (settings.soundPath.isNotEmpty()) {
            try {
                val actualPath = settings.soundPath.getFinalPath()
                val file = File(actualPath)
                
                if (file.exists() && actualPath.isAudioFast()) {
                    playCustomSong(context, actualPath)
                    return
                } else if (settings.soundPath.startsWith("content://")) {
                    playCustomSong(context, settings.soundPath)
                    return
                }
            } catch (e: Exception) {
                LogCat.e("Failed to play custom song, falling back to default sound: ${e.message}")
                // Fall through to play default sound
            }
        }
        
        // Play default notification sound in IO thread
        coIO {
            playNotificationSound()
        }
    }

    private suspend fun playCustomSong(context: Context, songPath: String) {
        try {
            val audio = DPlaylistAudio.fromPath(context, songPath)
            coIO {
                AudioPlayer.justPlay(context, audio)
            }
        } catch (e: Exception) {
            LogCat.e("Failed to play custom song: ${e.message}")
            // Don't throw exception, let caller handle fallback
        }
    }

    fun playNotificationSound() {
        com.ismartcoding.plain.ui.page.pomodoro.playNotificationSound()
    }
}