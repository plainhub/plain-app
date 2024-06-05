package com.ismartcoding.plain.ui.models

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.toMutableStateList
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.ismartcoding.plain.data.DMediaBucket
import com.ismartcoding.plain.db.DFeed
import com.ismartcoding.plain.enums.DataType
import com.ismartcoding.plain.features.media.ImageMediaStoreHelper
import com.ismartcoding.plain.features.media.VideoMediaStoreHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@OptIn(androidx.lifecycle.viewmodel.compose.SavedStateHandleSaveableApi::class)
class MediaFoldersViewModel(private val savedStateHandle: SavedStateHandle) : ViewModel() {
    private val _itemsFlow = MutableStateFlow(mutableStateListOf<DMediaBucket>())
    val itemsFlow: StateFlow<List<DMediaBucket>> get() = _itemsFlow
    var showLoading = mutableStateOf(true)
    var selectedItem = mutableStateOf<DFeed?>(null)
    var dataType = mutableStateOf(DataType.DEFAULT)

    fun loadAsync(context: Context) {
        _itemsFlow.value = (when (dataType.value) {
            DataType.IMAGE -> {
                ImageMediaStoreHelper.getBucketsAsync(context)
            }
            DataType.VIDEO -> {
                VideoMediaStoreHelper.getBucketsAsync(context)
            }
            else -> {
                emptyList()
            }
        }).toMutableStateList()
        showLoading.value = false
    }

}
