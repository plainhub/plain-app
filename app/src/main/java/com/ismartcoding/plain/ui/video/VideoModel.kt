package com.ismartcoding.plain.ui.video

import com.ismartcoding.plain.data.DVideo
import com.ismartcoding.plain.ui.models.BaseItemModel
import com.ismartcoding.plain.ui.models.IDataModel

open class VideoModel(override val data: DVideo) : IDataModel, BaseItemModel() {
    var title: String = ""
    var subtitle: String = ""
    var duration: String = ""
    var isPlaying = false
}
