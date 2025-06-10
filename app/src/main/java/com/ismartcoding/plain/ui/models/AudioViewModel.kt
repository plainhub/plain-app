package com.ismartcoding.plain.ui.models

import android.content.Context
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.toMutableStateList
import androidx.lifecycle.viewModelScope
import com.ismartcoding.plain.data.DAudio
import com.ismartcoding.plain.enums.DataType
import com.ismartcoding.plain.features.TagHelper
import com.ismartcoding.plain.features.media.AudioMediaStoreHelper
import com.ismartcoding.plain.preference.AudioPlaylistPreference
import com.ismartcoding.plain.ui.helpers.DialogHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AudioViewModel : BaseMediaViewModel<DAudio>() {
    override val dataType = DataType.AUDIO
    val scrollStateMap = mutableStateMapOf<Int, LazyListState>()

    fun delete(context: Context, tagsVM: TagsViewModel, ids: Set<String>) {
        viewModelScope.launch(Dispatchers.IO) {
            DialogHelper.showLoading()
            TagHelper.deleteTagRelationByKeys(ids, dataType)
            AudioMediaStoreHelper.deleteRecordsAndFilesByIdsAsync(context, ids, trash.value)
            val pathes = itemsFlow.value.filter { ids.contains(it.id) }.map { it.path }.toSet()
            AudioPlaylistPreference.deleteAsync(context, pathes)
            loadAsync(context, tagsVM)
            DialogHelper.hideLoading()
            _itemsFlow.update {
                it.toMutableStateList().apply {
                    removeIf { i -> ids.contains(i.id) }
                }
            }
        }
    }
}
