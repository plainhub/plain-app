package com.ismartcoding.plain.ui.models

import android.content.Context
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import com.ismartcoding.plain.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.plain.ai.ImageIndexManager
import com.ismartcoding.plain.data.DImage
import com.ismartcoding.plain.enums.DataType
import com.ismartcoding.plain.features.TagHelper
import com.ismartcoding.plain.features.media.ImageMediaStoreHelper
import com.ismartcoding.plain.ui.helpers.DialogHelper

class ImagesViewModel : BaseMediaViewModel<DImage>() {
    override val dataType = DataType.IMAGE
    val scrollStateMap = mutableStateMapOf<Int, LazyGridState>()
    val useAiSearch = mutableStateOf(false)

    suspend fun loadWithAiSearchAsync(context: Context, tagsVM: TagsViewModel) = withIO {
        val query = queryText.value.trim()
        val combined = com.ismartcoding.plain.features.media.ImageSearchHelper.searchCombinedAsync(
            context = context,
            queryText = query,
            extraQuery = getQuery(),
            limit = limit.intValue,
            offset = 0,
            sortBy = sortBy.value
        )
        useAiSearch.value = query.isNotEmpty() && com.ismartcoding.plain.ai.ImageSearchManager.isModelReady()
        offset.intValue = 0
        _itemsFlow.value = combined
        tagsVM.loadAsync(_itemsFlow.value.map { it.id }.toSet())
        total.intValue = combined.size
        totalTrash.intValue = 0
        noMore.value = true
        showLoading.value = false
        if (combined.isEmpty()) {
            useAiSearch.value = false
            loadAsync(context, tagsVM)
        }
    }

    fun delete(context: Context, tagsVM: TagsViewModel, ids: Set<String>) {
        launchSafe {
            DialogHelper.showLoading()
            TagHelper.deleteTagRelationByKeys(ids, dataType)
            ImageMediaStoreHelper.deleteRecordsAndFilesByIdsAsync(context, ids, trash.value)
            ImageIndexManager.enqueueRemove(ids)
            loadAsync(context, tagsVM)
            DialogHelper.hideLoading()
        }
    }
}