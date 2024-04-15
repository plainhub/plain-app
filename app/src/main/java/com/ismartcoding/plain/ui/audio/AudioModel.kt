package com.ismartcoding.plain.ui.audio

import com.ismartcoding.plain.data.DAudio
import com.ismartcoding.plain.ui.models.IDataModel

data class AudioModel(override val data: DAudio) : IDataModel, BaseAudioModel()
