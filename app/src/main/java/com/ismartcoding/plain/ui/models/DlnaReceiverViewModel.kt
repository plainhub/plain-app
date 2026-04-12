package com.ismartcoding.plain.ui.models

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.exoplayer.ExoPlayer
import com.ismartcoding.plain.features.dlna.DlnaCommand
import com.ismartcoding.plain.features.dlna.DlnaPlaybackState
import com.ismartcoding.plain.features.dlna.DlnaRendererState
import com.ismartcoding.plain.features.dlna.receiver.DlnaRenderer
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class DlnaReceiverViewModel : ViewModel() {

    private var commandJob: Job? = null
    private var positionJob: Job? = null

    init {
        startCommandProcessing()
    }

    fun startReceiver(context: Context) {
        DlnaRenderer.start(context)
        startCommandProcessing()
    }

    fun stopReceiver(context: Context) {
        commandJob?.cancel()
        positionJob?.cancel()
        DlnaRenderer.stop()
    }

    fun startCommandProcessing() {
        commandJob?.cancel()
        commandJob = viewModelScope.launch {
            for (command in DlnaRendererState.commandChannel) {
                when (command) {
                    is DlnaCommand.SetUri -> {
                        DlnaRendererState.mediaUri.value = command.uri
                        DlnaRendererState.mediaTitle.value = command.title
                        DlnaRendererState.playbackState.value = DlnaPlaybackState.TRANSITIONING
                    }
                    is DlnaCommand.Play -> DlnaRendererState.playbackState.value = DlnaPlaybackState.PLAYING
                    is DlnaCommand.Pause -> DlnaRendererState.playbackState.value = DlnaPlaybackState.PAUSED
                    is DlnaCommand.Stop -> {
                        DlnaRendererState.seekTargetMs.value = 0L
                        DlnaRendererState.playbackState.value = DlnaPlaybackState.STOPPED
                    }
                    is DlnaCommand.Seek -> DlnaRendererState.seekTargetMs.value = command.positionMs
                }
            }
        }
    }

    fun startPositionSync(player: ExoPlayer) {
        positionJob?.cancel()
        positionJob = viewModelScope.launch {
            while (true) {
                DlnaRendererState.currentPositionMs.value = player.currentPosition.coerceAtLeast(0L)
                DlnaRendererState.durationMs.value = player.duration.coerceAtLeast(0L)
                delay(1_000)
            }
        }
    }
}
