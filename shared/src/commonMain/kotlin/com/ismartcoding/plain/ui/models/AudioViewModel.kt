package com.ismartcoding.plain.ui.models

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.mutableStateMapOf
import com.ismartcoding.plain.audio.DAudio
import com.ismartcoding.plain.enums.DataType

class AudioViewModel : BaseMediaViewModel<DAudio>() {
    override val dataType = DataType.AUDIO
    override val showFoldersAsList = true
    val scrollStateMap = mutableStateMapOf<Int, LazyListState>()

}
