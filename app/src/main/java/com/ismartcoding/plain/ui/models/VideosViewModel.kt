package com.ismartcoding.plain.ui.models

import android.content.Context
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.SavedStateHandleSaveableApi
import com.ismartcoding.plain.data.DVideo
import com.ismartcoding.plain.enums.DataType
import com.ismartcoding.plain.features.TagHelper
import com.ismartcoding.plain.features.media.VideoMediaStoreHelper
import com.ismartcoding.plain.ui.helpers.DialogHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(SavedStateHandleSaveableApi::class)
class VideosViewModel : BaseMediaViewModel<DVideo>() {
    override val dataType = DataType.VIDEO
    val scrollStateMap = mutableStateMapOf<Int, LazyGridState>()
    val showCellsPerRowDialog = mutableStateOf(false)

    fun delete(context: Context, tagsVM: TagsViewModel, ids: Set<String>) {
        viewModelScope.launch(Dispatchers.IO) {
            DialogHelper.showLoading()
            TagHelper.deleteTagRelationByKeys(ids, dataType)
            VideoMediaStoreHelper.deleteRecordsAndFilesByIdsAsync(context, ids)
            loadAsync(context, tagsVM)
            DialogHelper.hideLoading()
        }
    }
}
