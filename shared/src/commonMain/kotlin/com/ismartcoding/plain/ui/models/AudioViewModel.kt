package com.ismartcoding.plain.ui.models

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.mutableStateMapOf
import com.ismartcoding.plain.audio.DAudio
import com.ismartcoding.plain.enums.DataType
import com.ismartcoding.plain.features.TagHelper
import com.ismartcoding.plain.platform.deleteMedia
import com.ismartcoding.plain.platform.getMediaPathsByIds
import com.ismartcoding.plain.preferences.AudioPlaylistPreference
import com.ismartcoding.plain.ui.helpers.DialogHelper

class AudioViewModel : BaseMediaViewModel<DAudio>() {
    override val dataType = DataType.AUDIO
    override val showFoldersAsList = true
    val scrollStateMap = mutableStateMapOf<Int, LazyListState>()

    override fun delete(tagsVM: TagsViewModel, ids: Set<String>) {
        launchSafe {
            DialogHelper.showLoading()
            TagHelper.deleteTagRelationByKeys(ids, dataType)
            val pathes = getMediaPathsByIds(dataType, ids)
            deleteMedia(dataType, ids, trash.value)
            AudioPlaylistPreference.deleteAsync(pathes)
            loadAsync(tagsVM)
            DialogHelper.hideLoading()
        }
    }
}
