package com.ismartcoding.plain.ui.audio

import com.ismartcoding.plain.ui.models.BaseItemModel

open class BaseAudioModel : BaseItemModel() {
    var title: String = ""
    var subtitle: String = ""
    var isPlaying = false
}
