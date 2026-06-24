package com.ismartcoding.plain.ui.models

import android.content.Context
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.mutableStateMapOf
import com.ismartcoding.plain.audio.DAudio
import com.ismartcoding.plain.enums.DataType
import com.ismartcoding.plain.features.TagHelper
import com.ismartcoding.plain.audio.AudioMediaStoreHelper
import com.ismartcoding.plain.preferences.AudioPlaylistPreference
import com.ismartcoding.plain.ui.helpers.DialogHelper

class AudioViewModel : BaseMediaViewModel<DAudio>() {
    override val dataType = DataType.AUDIO
    val scrollStateMap = mutableStateMapOf<Int, LazyListState>()

    fun delete(context: Context, tagsVM: TagsViewModel, ids: Set<String>) {
        launchSafe {
            DialogHelper.showLoading()
            TagHelper.deleteTagRelationByKeys(ids, dataType)
            AudioMediaStoreHelper.deleteRecordsAndFilesByIdsAsync(context, ids, trash.value)
            val pathes = itemsFlow.value.filter { ids.contains(it.id) }.map { it.path }.toSet()
            AudioPlaylistPreference.deleteAsync(pathes)
            loadAsync(context, tagsVM)
            DialogHelper.hideLoading()
        }
    }
}
