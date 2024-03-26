package com.ismartcoding.plain.ui.models

import android.content.Context
import android.provider.MediaStore
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.toMutableStateList
import androidx.lifecycle.ViewModel
import com.ismartcoding.plain.data.enums.FileType
import com.ismartcoding.plain.features.file.DFile
import com.ismartcoding.plain.features.file.FileMediaStoreHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class DocsViewModel : ViewModel() {
    private val _itemsFlow = MutableStateFlow(mutableStateListOf<DFile>())
    val itemsFlow: StateFlow<List<DFile>> get() = _itemsFlow
    var showLoading = mutableStateOf(true)
    var offset = mutableIntStateOf(0)
    var limit = mutableIntStateOf(50)
    var noMore = mutableStateOf(false)

    fun moreAsync(context: Context, query: String = "") {
        offset.value += limit.value
        val items = FileMediaStoreHelper.getAllByFileType(context, MediaStore.VOLUME_EXTERNAL_PRIMARY, FileType.DOCUMENT)
            .filter { query.isEmpty() || it.name.contains(query) }
        _itemsFlow.value.addAll(items)
        showLoading.value = false
        noMore.value = items.size < limit.value
    }

    suspend fun loadAsync(context: Context, query: String = "") {
        offset.value = 0
        _itemsFlow.value = FileMediaStoreHelper.getAllByFileType(context, MediaStore.VOLUME_EXTERNAL_PRIMARY, FileType.DOCUMENT)
            .filter { query.isEmpty() || it.name.contains(query) }.toMutableStateList()
        noMore.value = _itemsFlow.value.size < limit.value
        showLoading.value = false
    }
}
