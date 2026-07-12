package com.ismartcoding.plain.ui.models

import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.mutableStateMapOf
import com.ismartcoding.plain.appContext
import com.ismartcoding.plain.data.DVideo
import com.ismartcoding.plain.enums.DataType
import com.ismartcoding.plain.features.TagHelper
import com.ismartcoding.plain.features.media.VideoMediaStoreHelper
import com.ismartcoding.plain.ui.helpers.DialogHelper

class VideosViewModel : BaseMediaViewModel<DVideo>() {
    override val dataType = DataType.VIDEO
    val scrollStateMap = mutableStateMapOf<Int, LazyGridState>()

    fun delete(tagsVM: TagsViewModel, ids: Set<String>) {
        launchSafe {
            DialogHelper.showLoading()
            TagHelper.deleteTagRelationByKeys(ids, dataType)
            VideoMediaStoreHelper.deleteRecordsAndFilesByIdsAsync(appContext, ids, trash.value)
            loadAsync(tagsVM)
            DialogHelper.hideLoading()
        }
    }
}
