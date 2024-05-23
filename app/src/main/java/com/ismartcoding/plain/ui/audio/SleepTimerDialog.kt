package com.ismartcoding.plain.ui.audio

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.os.SystemClock
import android.view.View
import android.widget.SeekBar
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.ismartcoding.lib.channel.receiveEvent
import com.ismartcoding.lib.extensions.formatDuration
import com.ismartcoding.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.plain.helpers.FormatHelper
import com.ismartcoding.lib.isSPlus
import com.ismartcoding.plain.R
import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.alarmManager
import com.ismartcoding.plain.preference.AudioSleepTimerFinishLastPreference
import com.ismartcoding.plain.preference.AudioSleepTimerMinutesPreference
import com.ismartcoding.plain.databinding.DialogSleepTimerBinding
import com.ismartcoding.plain.features.Permission
import com.ismartcoding.plain.features.PermissionsResultEvent
import com.ismartcoding.plain.features.audio.AudioPlayer
import com.ismartcoding.plain.enums.AudioServiceAction
import com.ismartcoding.plain.features.locale.LocaleHelper
import com.ismartcoding.plain.services.AudioPlayerService
import com.ismartcoding.plain.ui.BaseBottomSheetDialog
import com.ismartcoding.plain.ui.extensions.setSafeClick
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference

class SleepTimerDialog() : BaseBottomSheetDialog<DialogSleepTimerBinding>() {
    private var seekProgress: Int = 0
    private var timerUpdater: TimerUpdater? = null

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)

        receiveEvent<PermissionsResultEvent> {
            if (isSPlus()) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExact(
                        AlarmManager.ELAPSED_REALTIME_WAKEUP,
                        TempData.audioSleepTimerFutureTime,
                        makeTimerPendingIntent(PendingIntent.FLAG_CANCEL_CURRENT),
                    )
                } else {
                    alarmManager.setWindow(
                        AlarmManager.ELAPSED_REALTIME_WAKEUP,
                        TempData.audioSleepTimerFutureTime,
                        1000,
                        makeTimerPendingIntent(PendingIntent.FLAG_CANCEL_CURRENT),
                    )
                }
            }
        }
    }

    fun updateUI() {
        lifecycleScope.launch {
            val context = requireContext()
            val leftTime = TempData.audioSleepTimerFutureTime - SystemClock.elapsedRealtime()
            if (leftTime > 0) {
                binding.seekBar.isVisible = false
                binding.shouldFinishLastAudio.isVisible = false
                binding.minutes.text = (leftTime / 1000).formatDuration()
                binding.start.text = getString(R.string.stop)
                binding.start.setSafeClick {
                    lifecycleScope.launch {
                        timerUpdater?.cancel()
                        val previous = withIO {
                            PendingIntent.getService(
                                requireContext(),
                                0,
                                makeTimerIntent(),
                                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
                            )
                        }
                        if (previous != null) {
                            alarmManager.cancel(previous)
                            previous.cancel()
                        }
                        AudioPlayer.pendingQuit = false
                        TempData.audioSleepTimerFutureTime = 0
                        updateTimeDisplayTime()
                        updateUI()
                    }
                }
                timerUpdater = TimerUpdater(
                    WeakReference(this@SleepTimerDialog),
                    leftTime,
                )
                timerUpdater?.start()
            } else {
                binding.shouldFinishLastAudio.apply {
                    isChecked = withIO { AudioSleepTimerFinishLastPreference.getAsync(context) }
                    setOnCheckedChangeListener { _, checked ->
                        lifecycleScope.launch {
                            withIO { AudioSleepTimerFinishLastPreference.putAsync(context, checked) }
                        }
                    }
                }
                binding.seekBar.apply {
                    seekProgress = withIO { AudioSleepTimerMinutesPreference.getAsync(context) }
                    updateTimeDisplayTime()
                    progress = seekProgress
                }
                binding.seekBar.setOnSeekBarChangeListener(
                    object : SeekBar.OnSeekBarChangeListener {
                        override fun onProgressChanged(
                            seekBar: SeekBar,
                            i: Int,
                            b: Boolean,
                        ) {
                            if (i < 1) {
                                seekBar.progress = 1
                                return
                            }
                            seekProgress = i
                            updateTimeDisplayTime()
                        }

                        override fun onStartTrackingTouch(seekBar: SeekBar) {
                        }

                        override fun onStopTrackingTouch(seekBar: SeekBar) {
                            lifecycleScope.launch {
                                withIO {
                                    AudioSleepTimerMinutesPreference.putAsync(context, seekProgress)
                                }
                            }
                        }
                    },
                )
                binding.seekBar.isVisible = true
                binding.shouldFinishLastAudio.isVisible = true
                binding.start.text = getString(R.string.start)
                binding.start.setSafeClick {
                    lifecycleScope.launch {
                        withIO {
                            TempData.audioSleepTimerFutureTime = SystemClock.elapsedRealtime() + AudioSleepTimerMinutesPreference.getAsync(context) * 60 * 1000

                            if (isSPlus()) {
                                if (alarmManager.canScheduleExactAlarms()) {
                                    alarmManager.setExact(
                                        AlarmManager.ELAPSED_REALTIME_WAKEUP,
                                        TempData.audioSleepTimerFutureTime,
                                        makeTimerPendingIntent(PendingIntent.FLAG_CANCEL_CURRENT),
                                    )
                                } else {
                                    Permission.SCHEDULE_EXACT_ALARM.grant(requireContext())
                                }
                            } else {
                                alarmManager.setExact(
                                    AlarmManager.ELAPSED_REALTIME_WAKEUP,
                                    TempData.audioSleepTimerFutureTime,
                                    makeTimerPendingIntent(PendingIntent.FLAG_CANCEL_CURRENT),
                                )
                            }
                        }
                        updateUI()
                    }
                }
            }
        }
    }

    fun updateTimeDisplayTime() {
        binding.minutes.text = LocaleHelper.getStringF(R.string.x_min, "num", seekProgress)
    }

    override fun onPause() {
        super.onPause()
        timerUpdater?.cancel()
    }

    override fun onResume() {
        super.onResume()
        updateUI()
    }

    private suspend fun makeTimerPendingIntent(flag: Int): PendingIntent {
        return PendingIntent.getService(
            requireContext(),
            0,
            makeTimerIntent(),
            flag or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private suspend fun makeTimerIntent(): Intent {
        val context = requireContext()
        val intent = Intent(requireActivity(), AudioPlayerService::class.java)
        return if (AudioSleepTimerFinishLastPreference.getAsync(context)) {
            intent.setAction(AudioServiceAction.PENDING_QUIT.name)
        } else {
            intent.setAction(AudioServiceAction.QUIT.name)
        }
    }

    private inner class TimerUpdater(val dialog: WeakReference<SleepTimerDialog>, val millisInFuture: Long) : CountDownTimer(
        millisInFuture,
        1000,
    ) {
        override fun onTick(millisUntilFinished: Long) {
            dialog.get()?.let {
                if (it.isActive) {
                    it.binding.minutes.text = (millisUntilFinished / 1000).formatDuration()
                }
            }
        }

        override fun onFinish() {
            lifecycleScope.launch {
                TempData.audioSleepTimerFutureTime = 0
                dialog.get()?.let {
                    if (it.isActive) {
                        it.updateTimeDisplayTime()
                        it.updateUI()
                    }
                }
            }
        }
    }
}
