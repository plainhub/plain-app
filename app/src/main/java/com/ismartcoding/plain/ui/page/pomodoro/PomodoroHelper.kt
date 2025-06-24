package com.ismartcoding.plain.ui.page.pomodoro

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.ismartcoding.lib.extensions.getFinalPath
import com.ismartcoding.lib.extensions.isAudioFast
import com.ismartcoding.lib.helpers.CoroutinesHelper
import com.ismartcoding.lib.helpers.CoroutinesHelper.coIO
import com.ismartcoding.lib.logcat.LogCat
import com.ismartcoding.plain.Constants
import com.ismartcoding.plain.R
import com.ismartcoding.plain.data.DPlaylistAudio
import com.ismartcoding.plain.data.DPomodoroSettings
import com.ismartcoding.plain.db.AppDatabase
import com.ismartcoding.plain.features.AudioPlayer
import com.ismartcoding.plain.features.Permission
import com.ismartcoding.plain.features.locale.LocaleHelper
import com.ismartcoding.plain.helpers.NotificationHelper
import com.ismartcoding.plain.preferences.PomodoroSettingsPreference
import com.ismartcoding.plain.ui.MainActivity
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.sin
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
        val today = Clock.System.now()
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
        val sampleRate = 44100
        val durationMs = 300 // 0.3 seconds
        val samples = (sampleRate * durationMs / 1000.0).toInt()

        // Create audio buffer
        val audioBuffer = ShortArray(samples)

        // Generate sine wave with frequency and amplitude modulation
        for (i in 0 until samples) {
            val time = i.toDouble() / sampleRate

            // Frequency modulation: exponential decay from 800Hz to 600Hz
            // JavaScript: frequency.exponentialRampToValueAtTime(600, audioContext.currentTime + 0.1)
            val startFreq = 800.0
            val endFreq = 600.0
            val modulationTime = 0.1 // 100ms for frequency modulation

            val frequency = if (time <= modulationTime) {
                // Exponential decay for first 100ms
                val ratio = endFreq / startFreq
                startFreq * exp(ln(ratio) * (time / modulationTime))
            } else {
                // Constant frequency after 100ms
                endFreq
            }

            // Amplitude modulation: exponential decay from 0.3 to 0.01 over 300ms
            // JavaScript: gain.exponentialRampToValueAtTime(0.01, audioContext.currentTime + 0.3)
            val startGain = 0.3
            val endGain = 0.01
            val gainRatio = endGain / startGain
            val gain = startGain * exp(ln(gainRatio) * (time / (durationMs / 1000.0)))

            // Generate sine wave sample
            val sample = (sin(2 * PI * frequency * time) * gain * Short.MAX_VALUE).toInt()
            audioBuffer[i] = sample.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }

        // Create and configure AudioTrack
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
            AudioFormat.ENCODING_PCM_16BIT
        )

        val audioTrack = AudioTrack.Builder()
            .setAudioAttributes(audioAttributes)
            .setAudioFormat(audioFormat)
            .setBufferSizeInBytes(maxOf(bufferSize, audioBuffer.size * 2))
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()

        // Write audio data and play
        audioTrack.write(audioBuffer, 0, audioBuffer.size)
        audioTrack.setNotificationMarkerPosition(audioBuffer.size)
        audioTrack.setPlaybackPositionUpdateListener(object : AudioTrack.OnPlaybackPositionUpdateListener {
            override fun onMarkerReached(track: AudioTrack?) {
                track?.stop()
                track?.release()
            }

            override fun onPeriodicNotification(track: AudioTrack?) {
                // Not used
            }
        })

        audioTrack.play()
    }
}