package com.ismartcoding.plain.ui.images

import com.ismartcoding.plain.rust.graphql.ContentSource
import com.ismartcoding.plain.ui.media.BaseMediaViewModel

import com.ismartcoding.plain.models.Image as MediaImage
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.mutableStateMapOf
import com.ismartcoding.plain.platform.system.DataType

class ImagesViewModel internal constructor(source: ContentSource = ContentSource.Local) : BaseMediaViewModel<MediaImage>(source) {
    override val dataType = DataType.IMAGE
    val scrollStateMap = mutableStateMapOf<Int, LazyGridState>()
}
