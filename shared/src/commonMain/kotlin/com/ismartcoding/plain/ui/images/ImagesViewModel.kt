package com.ismartcoding.plain.ui.images

import com.ismartcoding.plain.rust.graphql.ContentSource
import com.ismartcoding.plain.ui.tags.TagsViewModel
import com.ismartcoding.plain.ui.media.BaseMediaViewModel

import com.ismartcoding.plain.models.Image as MediaImage
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import com.ismartcoding.plain.lib.withIO
import com.ismartcoding.plain.platform.system.DataType
import com.ismartcoding.plain.features.images.search.RustImageModels
import com.ismartcoding.plain.features.images.search.ImageSearchStatusType

class ImagesViewModel internal constructor(source: ContentSource = ContentSource.Local) : BaseMediaViewModel<MediaImage>(source) {
    override val dataType = DataType.IMAGE
    val scrollStateMap = mutableStateMapOf<Int, LazyGridState>()
    val useAiSearch = mutableStateOf(false)

    suspend fun loadWithAiSearchAsync(tagsVM: TagsViewModel) = withIO {
        val query = queryText.value.trim()
        offset.intValue = 0
        val combined = searchMediaAsync(getQuery())
        useAiSearch.value = query.isNotEmpty() && RustImageModels.snapshot.value.status == ImageSearchStatusType.READY
        _itemsFlow.value = combined
        tagsVM.loadAsync(_itemsFlow.value.map { it.id }.toSet())
        total.intValue = combined.size
        totalTrash.intValue = 0
        noMore.value = true
        showLoading.value = false
        if (combined.isEmpty()) {
            useAiSearch.value = false
            loadAsync(tagsVM)
        }
    }
}
