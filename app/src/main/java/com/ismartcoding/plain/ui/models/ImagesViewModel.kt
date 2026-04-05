package com.ismartcoding.plain.ui.models

import android.content.Context
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.toMutableStateList
import androidx.lifecycle.viewModelScope
import com.ismartcoding.plain.ai.ImageIndexManager
import com.ismartcoding.plain.ai.ImageSearchManager
import com.ismartcoding.plain.data.DImage
import com.ismartcoding.plain.enums.DataType
import com.ismartcoding.plain.features.TagHelper
import com.ismartcoding.plain.features.media.ImageMediaStoreHelper
import com.ismartcoding.plain.ui.helpers.DialogHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ImagesViewModel : BaseMediaViewModel<DImage>() {
    override val dataType = DataType.IMAGE
    val scrollStateMap = mutableStateMapOf<Int, LazyGridState>()
    val useAiSearch = mutableStateOf(false)

    suspend fun loadWithAiSearchAsync(context: Context, tagsVM: TagsViewModel) {
        val query = queryText.value.trim()
        if (query.isNotEmpty() && ImageSearchManager.isModelReady()) {
            useAiSearch.value = true
            val results = ImageSearchManager.search(query)
            if (results.isNotEmpty()) {
                val ids = results.map { it.imageId }
                val idsQuery = "ids:${ids.joinToString(",")} trash:false"
                offset.intValue = 0
                val items = ImageMediaStoreHelper.searchAsync(context, idsQuery, ids.size, 0, sortBy.value)
                val idOrder = ids.withIndex().associate { it.value to it.index }
                _itemsFlow.value = items.sortedBy { idOrder[it.id] ?: Int.MAX_VALUE }.toMutableStateList()
                tagsVM.loadAsync(_itemsFlow.value.map { it.id }.toSet())
                total.intValue = items.size
                totalTrash.intValue = 0
                noMore.value = true
                showLoading.value = false
                return
            }
        }
        useAiSearch.value = false
        loadAsync(context, tagsVM)
    }

    fun delete(context: Context, tagsVM: TagsViewModel, ids: Set<String>) {
        viewModelScope.launch(Dispatchers.IO) {
            DialogHelper.showLoading()
            TagHelper.deleteTagRelationByKeys(ids, dataType)
            ImageMediaStoreHelper.deleteRecordsAndFilesByIdsAsync(context, ids, trash.value)
            ImageIndexManager.enqueueRemove(ids)
            loadAsync(context, tagsVM)
            DialogHelper.hideLoading()
        }
    }
}