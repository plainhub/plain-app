package com.ismartcoding.plain.ui.audio

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import com.ismartcoding.lib.channel.receiveEventHandler
import com.ismartcoding.lib.helpers.CoroutinesHelper.coMain
import com.ismartcoding.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.plain.R
import com.ismartcoding.plain.data.preference.AudioPlayingPreference
import com.ismartcoding.plain.databinding.ViewBottomAudioPlayerBinding
import com.ismartcoding.plain.features.AudioActionEvent
import com.ismartcoding.plain.features.audio.AudioAction
import com.ismartcoding.plain.features.audio.AudioPlayer
import com.ismartcoding.plain.services.AudioPlayerService
import com.ismartcoding.plain.ui.extensions.setSafeClick
import com.ismartcoding.plain.ui.views.CustomViewBase

class BottomAudioPlayerView(context: Context, attrs: AttributeSet?) : CustomViewBase(context, attrs) {
    private val binding = ViewBottomAudioPlayerBinding.inflate(LayoutInflater.from(context), this, true)
    private var seekBarUpdateRunnable: Runnable? = null
    private val seekBarUpdateDelayMillis: Long = 1000

    private fun initSeekBarUpdateRunnable() {
        seekBarUpdateRunnable =
            Runnable {
                binding.audioProgress.run {
                    progress++
                    postDelayed(seekBarUpdateRunnable, seekBarUpdateDelayMillis)
                }
            }
    }

    fun initView() {
        events.add(
            receiveEventHandler<AudioActionEvent> { event ->
                when (event.action) {
                    AudioAction.PLAY, AudioAction.PAUSE, AudioAction.SEEK -> {
                        updateUI()
                    }

                    else -> {}
                }
            },
        )
        initSeekBarUpdateRunnable()
        setListen()
    }

    fun updateUI() {
        coMain {
            binding.audioProgress.removeCallbacks(seekBarUpdateRunnable)
            val audio = withIO { AudioPlayingPreference.getValueAsync(context) }
            if (audio == null) {
                this@BottomAudioPlayerView.visibility = View.GONE
                return@coMain
            } else {
                this@BottomAudioPlayerView.visibility = View.VISIBLE
            }

            binding.audioProgress.apply {
                progress = AudioPlayer.instance.getPlayerProgress()
                max = audio.duration.toInt()
            }
            binding.audioTitle.text = audio.title
            binding.audioArtist.text = audio.artist
            val isPlaying = AudioPlayer.instance.isPlaying()
            if (isPlaying) {
                binding.audioProgress.postDelayed(seekBarUpdateRunnable, seekBarUpdateDelayMillis)
            }
            binding.playPauseButton.setImageResource(if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play)
        }
    }

    private fun setListen() {
        binding.playPauseButton.setSafeClick {
            if (AudioPlayer.instance.isPlaying()) {
                AudioPlayerService.pause(context)
            } else {
                AudioPlayerService.play(context)
            }
        }

        binding.audioInfo.setSafeClick {
            AudioPlayerDialog().show()
        }

        binding.audioQueue.setSafeClick {
            AudioPlaylistDialog().show()
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        seekBarUpdateRunnable?.let {
            binding.audioProgress.removeCallbacks(it)
        }
    }
}
