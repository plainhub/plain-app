package com.ismartcoding.plain.ui.models

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel

/**
 * iOS stub [AudioPlaylistViewModelBase]. Audio playback on iOS is not yet
 * implemented; this provides the minimal surface needed by chat UI.
 */
class IosAudioPlaylistViewModel : ViewModel(), AudioPlaylistViewModelBase {
    override val selectedPath = mutableStateOf("")
}
