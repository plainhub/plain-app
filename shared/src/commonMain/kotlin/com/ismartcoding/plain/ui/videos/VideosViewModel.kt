package com.ismartcoding.plain.ui.videos

import com.ismartcoding.plain.rust.graphql.ContentSource
import com.ismartcoding.plain.ui.media.BaseMediaViewModel

import com.ismartcoding.plain.models.Video as MediaVideo
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.mutableStateMapOf
import com.ismartcoding.plain.platform.system.DataType

class VideosViewModel internal constructor(source: ContentSource = ContentSource.Local) : BaseMediaViewModel<MediaVideo>(
    source,
    DataType.VIDEO,
    pageSize = if (source is ContentSource.Remote) 256 else 1000,
    initialPageSize = if (source is ContentSource.Remote) 128 else 1000,
) {
    val scrollStateMap = mutableStateMapOf<Int, LazyGridState>()
}
