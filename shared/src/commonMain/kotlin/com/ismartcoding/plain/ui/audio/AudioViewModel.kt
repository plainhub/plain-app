package com.ismartcoding.plain.ui.audio

import com.ismartcoding.plain.rust.graphql.ContentSource
import com.ismartcoding.plain.ui.media.BaseMediaViewModel

import com.ismartcoding.plain.models.Audio as AudioTrack
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.mutableStateMapOf
import com.ismartcoding.plain.platform.system.DataType

class AudioViewModel internal constructor(source: ContentSource = ContentSource.Local) : BaseMediaViewModel<AudioTrack>(source) {
    override val dataType = DataType.AUDIO
    override val showFoldersAsList = true
    val scrollStateMap = mutableStateMapOf<Int, LazyListState>()

}
