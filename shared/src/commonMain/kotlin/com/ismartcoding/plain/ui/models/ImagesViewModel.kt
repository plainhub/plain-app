package com.ismartcoding.plain.ui.models

import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.viewModelScope
import com.ismartcoding.plain.helpers.withIO
import com.ismartcoding.plain.data.DImage
import com.ismartcoding.plain.enums.DataType
import com.ismartcoding.plain.features.TagHelper
import com.ismartcoding.plain.platform.deleteMedia
import com.ismartcoding.plain.platform.enqueueRemoveImageIndex
import com.ismartcoding.plain.platform.isImageSearchModelReady
import com.ismartcoding.plain.platform.searchImagesCombined
import com.ismartcoding.plain.ui.helpers.DialogHelper

class ImagesViewModel : BaseMediaViewModel<DImage>() {
    override val dataType = DataType.IMAGE
    val scrollStateMap = mutableStateMapOf<Int, LazyGridState>()
    val useAiSearch = mutableStateOf(false)

    suspend fun loadWithAiSearchAsync(tagsVM: TagsViewModel) = withIO {
        val query = queryText.value.trim()
        val combined = searchImagesCombined(
            queryText = query,
            extraQuery = getQuery(),
            limit = limit.intValue,
            offset = 0,
            sortBy = sortBy.value
        )
        useAiSearch.value = query.isNotEmpty() && isImageSearchModelReady()
        offset.intValue = 0
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

    override fun delete(tagsVM: TagsViewModel, ids: Set<String>) {
        viewModelScope.launchSafe {
            DialogHelper.showLoading()
            TagHelper.deleteTagRelationByKeys(ids, dataType)
            deleteMedia(dataType, ids, trash.value)
            enqueueRemoveImageIndex(ids)
            loadAsync(tagsVM)
            DialogHelper.hideLoading()
        }
    }
}
