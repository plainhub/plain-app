package com.ismartcoding.plain.ui.models

import android.content.Context
import androidx.compose.runtime.toMutableStateList
import androidx.lifecycle.viewModelScope
import com.ismartcoding.plain.data.IData
import com.ismartcoding.plain.enums.DataType
import com.ismartcoding.plain.features.TagHelper
import com.ismartcoding.plain.audio.AudioMediaStoreHelper
import com.ismartcoding.plain.docs.DocMediaStoreHelper
import com.ismartcoding.plain.features.media.ImageMediaStoreHelper
import com.ismartcoding.plain.features.media.VideoMediaStoreHelper
import com.ismartcoding.plain.ui.helpers.DialogHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal fun <T : IData> BaseMediaViewModel<T>.trashItems(
    context: Context, tagsVM: TagsViewModel, ids: Set<String>,
) {
    viewModelScope.launch(Dispatchers.IO) {
        DialogHelper.showLoading()
        TagHelper.deleteTagRelationByKeys(ids, dataType)
        when (dataType) {
            DataType.AUDIO -> AudioMediaStoreHelper.trashByIdsAsync(context, ids)
            DataType.DOC -> DocMediaStoreHelper.trashByIdsAsync(context, ids)
            DataType.IMAGE -> ImageMediaStoreHelper.trashByIdsAsync(context, ids)
            DataType.VIDEO -> VideoMediaStoreHelper.trashByIdsAsync(context, ids)
            else -> {}
        }
        loadAsync(context, tagsVM)
        DialogHelper.hideLoading()
        _itemsFlow.update {
            it.toMutableStateList().apply { removeIf { i -> ids.contains(i.id) } }
        }
    }
}

internal fun <T : IData> BaseMediaViewModel<T>.restoreItems(
    context: Context, tagsVM: TagsViewModel, ids: Set<String>,
) {
    viewModelScope.launch(Dispatchers.IO) {
        DialogHelper.showLoading()
        when (dataType) {
            DataType.AUDIO -> AudioMediaStoreHelper.restoreByIdsAsync(context, ids)
            DataType.DOC -> DocMediaStoreHelper.restoreByIdsAsync(context, ids)
            DataType.IMAGE -> ImageMediaStoreHelper.restoreByIdsAsync(context, ids)
            DataType.VIDEO -> VideoMediaStoreHelper.restoreByIdsAsync(context, ids)
            else -> {}
        }
        loadAsync(context, tagsVM)
        DialogHelper.hideLoading()
        _itemsFlow.update {
            it.toMutableStateList().apply { removeIf { i -> ids.contains(i.id) } }
        }
    }
}

internal suspend fun <T : IData> BaseMediaViewModel<T>.countMediaAsync(
    context: Context, query: String,
): Int {
    return when (dataType) {
        DataType.AUDIO -> AudioMediaStoreHelper.countAsync(context, query)
        DataType.DOC -> DocMediaStoreHelper.countAsync(context, query)
        DataType.IMAGE -> ImageMediaStoreHelper.countAsync(context, query)
        DataType.VIDEO -> VideoMediaStoreHelper.countAsync(context, query)
        else -> 0
    }
}

@Suppress("UNCHECKED_CAST")
internal suspend fun <T : IData> BaseMediaViewModel<T>.searchMediaAsync(
    context: Context, query: String,
): List<T> {
    return when (dataType) {
        DataType.AUDIO -> AudioMediaStoreHelper.searchAsync(context, query, limit.intValue, offset.intValue, sortBy.value)
        DataType.DOC -> DocMediaStoreHelper.searchAsync(context, query, limit.intValue, offset.intValue, sortBy.value)
        DataType.IMAGE -> ImageMediaStoreHelper.searchAsync(context, query, limit.intValue, offset.intValue, sortBy.value)
        DataType.VIDEO -> VideoMediaStoreHelper.searchAsync(context, query, limit.intValue, offset.intValue, sortBy.value)
        else -> emptyList()
    } as List<T>
}
