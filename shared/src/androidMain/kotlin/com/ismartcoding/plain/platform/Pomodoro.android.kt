package com.ismartcoding.plain.platform

import com.ismartcoding.plain.appContext
import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.platform.AppResources
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
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
import com.ismartcoding.plain.audio.AudioPlayer
import com.ismartcoding.plain.audio.fromPath
import com.ismartcoding.plain.platform.Permission
import com.ismartcoding.plain.platform.isGranted
import com.ismartcoding.plain.helpers.NotificationHelper
import com.ismartcoding.plain.helpers.TimeHelper
import com.ismartcoding.plain.preferences.PomodoroSettingsPreference
import com.ismartcoding.plain.MainActivity
import com.ismartcoding.plain.ui.page.pomodoro.PomodoroState
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.io.File
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.sin

@SuppressLint("MissingPermission")
actual suspend fun showPomodoroNotification(state: PomodoroState) {
    val context = appContext
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

actual suspend fun playPomodoroCompletionSound(settings: DPomodoroSettings) {
    val context = appContext
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
        }
    }

    coIO {
        playNotificationSound()
    }
}

private suspend fun playCustomSong(context: android.content.Context, songPath: String) {
    try {
        val audio = DPlaylistAudio.fromPath(context, songPath)
        coIO {
            AudioPlayer.justPlay(context, audio)
        }
    } catch (e: Exception) {
        LogCat.e("Failed to play custom song: ${e.message}")
    }
}

private fun playNotificationSound() {
    val sampleRate = 44100
    val durationMs = 300
    val samples = (sampleRate * durationMs / 1000.0).toInt()

    val audioBuffer = ShortArray(samples)

    for (i in 0 until samples) {
        val time = i.toDouble() / sampleRate

        val startFreq = 800.0
        val endFreq = 600.0
        val modulationTime = 0.1

        val frequency = if (time <= modulationTime) {
            val ratio = endFreq / startFreq
            startFreq * exp(ln(ratio) * (time / modulationTime))
        } else {
            endFreq
        }

        val startGain = 0.3
        val endGain = 0.01
        val gainRatio = endGain / startGain
        val gain = startGain * exp(ln(gainRatio) * (time / (durationMs / 1000.0)))

        val sample = (sin(2 * PI * frequency * time) * gain * Short.MAX_VALUE).toInt()
        audioBuffer[i] = sample.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
    }

    val audioAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()

    val audioFormat = AudioFormat.Builder()
        .setSampleRate(sampleRate)
        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
        .build()

    val bufferSize = AudioTrack.getMinBufferSize(
        sampleRate,
        AudioFormat.CHANNEL_OUT_MONO,
        AudioFormat.ENCODING_PCM_16BIT,
    )

    val audioTrack = AudioTrack.Builder()
        .setAudioAttributes(audioAttributes)
        .setAudioFormat(audioFormat)
        .setBufferSizeInBytes(maxOf(bufferSize, audioBuffer.size * 2))
        .setTransferMode(AudioTrack.MODE_STATIC)
        .build()

    audioTrack.write(audioBuffer, 0, audioBuffer.size)
    audioTrack.setNotificationMarkerPosition(audioBuffer.size)
    audioTrack.setPlaybackPositionUpdateListener(object : AudioTrack.OnPlaybackPositionUpdateListener {
        override fun onMarkerReached(track: AudioTrack?) {
            track?.stop()
            track?.release()
        }

        override fun onPeriodicNotification(track: AudioTrack?) {}
    })

    audioTrack.play()
}
