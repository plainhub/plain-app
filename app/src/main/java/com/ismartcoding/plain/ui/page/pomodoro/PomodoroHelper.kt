package com.ismartcoding.plain.ui.page.pomodoro

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.ismartcoding.lib.extensions.getFinalPath
import com.ismartcoding.lib.extensions.isAudioFast
import com.ismartcoding.lib.helpers.CoroutinesHelper
import com.ismartcoding.lib.helpers.CoroutinesHelper.coIO
import com.ismartcoding.lib.logcat.LogCat
import com.ismartcoding.plain.Constants
import com.ismartcoding.plain.R
import com.ismartcoding.plain.audio.DPlaylistAudio
import com.ismartcoding.plain.data.DPomodoroSettings
import com.ismartcoding.plain.db.AppDatabase
import com.ismartcoding.plain.audio.AudioPlayer
import com.ismartcoding.plain.features.Permission
import com.ismartcoding.plain.features.locale.LocaleHelper
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
        val settings = PomodoroSettingsPreference.getValueAsync(context)
        if (!settings.showNotification) {
            return
        }

        NotificationHelper.ensureDefaultChannel()
        val database = AppDatabase.Companion.instance
        val pomodoroDao = database.pomodoroItemDao()
        val today = TimeHelper.now()
            .toLocalDateTime(TimeZone.currentSystemDefault()).date.toString()
        val todayRecord = CoroutinesHelper.withIO { pomodoroDao.getByDate(today) }
        val completedPomodoros = todayRecord?.completedCount ?: 0

        // Determine notification content based on current state
        val (title, message) = when (state) {
            PomodoroState.WORK -> {
                val newCount = completedPomodoros + 1
                val shouldBeLongBreak = newCount % settings.pomodorosBeforeLongBreak == 0 && newCount > 0
                val messageRes = if (shouldBeLongBreak) R.string.great_job_long_break else R.string.great_job_short_break
                Pair(
                    LocaleHelper.getString(R.string.work_session_complete),
                    LocaleHelper.getString(messageRes)
                )
            }

            PomodoroState.SHORT_BREAK -> {
                Pair(LocaleHelper.getString(R.string.break_complete), LocaleHelper.getString(R.string.time_to_work))
            }

            PomodoroState.LONG_BREAK -> {
                Pair(LocaleHelper.getString(R.string.long_break_complete), LocaleHelper.getString(R.string.ready_for_work))
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
            .setSmallIcon(R.drawable.notification)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        try {
            if (Permission.POST_NOTIFICATIONS.can(context)) {
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
                val actualPath = settings.soundPath.getFinalPath(context)
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