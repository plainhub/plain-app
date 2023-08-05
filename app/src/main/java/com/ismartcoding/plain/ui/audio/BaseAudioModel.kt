package com.ismartcoding.plain.ui.audio

import android.content.Context
import com.ismartcoding.plain.data.preference.AudioPlayingPreference
import com.ismartcoding.plain.features.audio.AudioPlayer
import com.ismartcoding.plain.ui.models.BaseItemModel

open class BaseAudioModel : BaseItemModel() {
    var title: String = ""
    var subtitle: String = ""
    var isPlaying = false
}
