package com.ismartcoding.plain.ui.audio

import android.os.Bundle
import android.view.View
import android.widget.SeekBar
import androidx.lifecycle.lifecycleScope
import com.ismartcoding.lib.channel.receiveEvent
import com.ismartcoding.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.plain.R
import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.data.DPlaylistAudio
import com.ismartcoding.plain.enums.AudioAction
import com.ismartcoding.plain.enums.MediaPlayMode
import com.ismartcoding.plain.preference.AudioPlayModePreference
import com.ismartcoding.plain.preference.AudioPlayingPreference
import com.ismartcoding.plain.databinding.DialogAudioPlayerBinding
import com.ismartcoding.plain.features.AudioActionEvent
import com.ismartcoding.plain.features.ClearAudioPlaylistEvent
import com.ismartcoding.plain.features.audio.AudioPlayer
import com.ismartcoding.plain.helpers.FormatHelper
import com.ismartcoding.plain.ui.BaseBottomSheetDialog
import com.ismartcoding.plain.ui.extensions.setSafeClick
import kotlinx.coroutines.launch

class AudioPlayerDialog() : BaseBottomSheetDialog<DialogAudioPlayerBinding>() {
    private lateinit var seekBarUpdateRunnable: Runnable
    private val seekBarUpdateDelayMillis: Long = 1000

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        initEvents()
        initSeekBarUpdateRunnable()
        setListen()
    }

    private fun initEvents() {
        receiveEvent<AudioActionEvent> { event ->
            when (event.action) {
                AudioAction.PLAYBACK_STATE_CHANGED, AudioAction.MEDIA_ITEM_TRANSITION -> {
                    updateUI()
                }

                else -> {}
            }
        }
        receiveEvent<ClearAudioPlaylistEvent> {
            dismissNow()
        }
    }

    override fun onResume() {
        super.onResume()
        updateUI()
    }

    override fun onPause() {
        super.onPause()
        binding.seekBar.removeCallbacks(seekBarUpdateRunnable)
    }

    private fun initSeekBarUpdateRunnable() {
        seekBarUpdateRunnable =
            Runnable {
                if (view == null) {
                    return@Runnable
                }
                binding.seekBar.progress = (AudioPlayer.playerProgress / 1000).toInt()
                binding.seekBar.postDelayed(seekBarUpdateRunnable, seekBarUpdateDelayMillis)
            }
    }

    private fun updateUI() {
        lifecycleScope.launch {
            binding.seekBar.removeCallbacks(seekBarUpdateRunnable)
            val process = AudioPlayer.playerProgress / 1000
            binding.process.text = FormatHelper.formatDuration(process)
            val path = withIO { AudioPlayingPreference.getValueAsync(requireContext()) }
            if (path.isNotEmpty()) {
                val audio = withIO { DPlaylistAudio.fromPath(requireContext(), path) }
                binding.seekBar.max = audio.duration.toInt()
                binding.seekBar.progress = process.toInt()
                binding.duration.text = FormatHelper.formatDuration(audio.duration)
                binding.title.text = audio.title
                binding.title.isSelected = true // need for marquee
                binding.artist.text = audio.artist
            }

            val isPlaying = AudioPlayer.isPlaying()
            if (isPlaying) {
                binding.seekBar.postDelayed(seekBarUpdateRunnable, seekBarUpdateDelayMillis)
            }
            binding.play.setImageResource(if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play)
            updatePlayMode()
        }
    }

    private fun updatePlayMode() {
        lifecycleScope.launch {
            val mode = TempData.audioPlayMode
            binding.repeat.setImageResource(
                when (mode) {
                    MediaPlayMode.REPEAT -> R.drawable.ic_repeat
                    MediaPlayMode.REPEAT_ONE -> R.drawable.ic_repeat_one
                    MediaPlayMode.SHUFFLE -> R.drawable.ic_shuffle
                },
            )
        }
    }

    private fun setListen() {
        binding.repeat.setSafeClick {
            lifecycleScope.launch {
                val newMode =
                    when (TempData.audioPlayMode) {
                        MediaPlayMode.REPEAT -> MediaPlayMode.REPEAT_ONE
                        MediaPlayMode.REPEAT_ONE -> MediaPlayMode.SHUFFLE
                        MediaPlayMode.SHUFFLE -> MediaPlayMode.REPEAT
                    }
                withIO { AudioPlayModePreference.putAsync(requireContext(), newMode) }
                updatePlayMode()
            }
        }

        binding.seekBar.setOnSeekBarChangeListener(
            object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(
                    s: SeekBar,
                    progress: Int,
                    fromUser: Boolean,
                ) {
                    if (fromUser) {
                        s.removeCallbacks(seekBarUpdateRunnable)
                    }

                    binding.process.text = FormatHelper.formatDuration(s.progress.toLong())
                }

                override fun onStartTrackingTouch(s: SeekBar) = Unit

                override fun onStopTrackingTouch(s: SeekBar) {
                    s.removeCallbacks(seekBarUpdateRunnable)
                    binding.process.text = FormatHelper.formatDuration(s.progress.toLong())
                    AudioPlayer.seekTo(s.progress.toLong())
                    s.postDelayed(seekBarUpdateRunnable, seekBarUpdateDelayMillis)
                }
            },
        )

        binding.skipPrev.setSafeClick {
            AudioPlayer.skipToPrevious()
        }

        binding.play.setSafeClick {
            if (AudioPlayer.isPlaying()) {
                AudioPlayer.pause()
            } else {
                AudioPlayer.play()
            }
        }

        binding.skipNext.setSafeClick {
            AudioPlayer.skipToNext()
        }

        binding.queue.setSafeClick {
            AudioPlaylistDialog().show()
        }

        binding.snooze.setSafeClick {
            SleepTimerDialog().show()
        }
    }
}
