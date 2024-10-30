package com.ismartcoding.plain.ui.models

import android.content.Context
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.viewModelScope
import com.ismartcoding.plain.data.DImage
import com.ismartcoding.plain.enums.DataType
import com.ismartcoding.plain.features.TagHelper
import com.ismartcoding.plain.features.media.ImageMediaStoreHelper
import com.ismartcoding.plain.ui.helpers.DialogHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ImagesViewModel : BaseMediaViewModel<DImage>() {
    override val dataType = DataType.IMAGE
    val scrollStateMap = mutableStateMapOf<Int, LazyGridState>()
    var showCellsPerRowDialog = mutableStateOf(false)

    fun delete(context: Context, tagsVM: TagsViewModel, ids: Set<String>) {
        viewModelScope.launch(Dispatchers.IO) {
            DialogHelper.showLoading()
            TagHelper.deleteTagRelationByKeys(ids, dataType)
            ImageMediaStoreHelper.deleteRecordsAndFilesByIdsAsync(context, ids)
            loadAsync(context, tagsVM)
            DialogHelper.hideLoading()
        }
    }
}