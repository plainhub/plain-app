package com.ismartcoding.plain.ui.models

import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.mutableStateMapOf
import com.ismartcoding.plain.data.DVideo
import com.ismartcoding.plain.enums.DataType

class VideosViewModel : BaseMediaViewModel<DVideo>() {
    override val dataType = DataType.VIDEO
    val scrollStateMap = mutableStateMapOf<Int, LazyGridState>()
}
