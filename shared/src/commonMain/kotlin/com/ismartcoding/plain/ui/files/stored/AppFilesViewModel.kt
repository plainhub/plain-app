package com.ismartcoding.plain.ui.files.stored

import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.ismartcoding.plain.lib.withIO
import com.ismartcoding.plain.features.files.stored.AppFileStore
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

class AppFilesViewModel internal constructor(val source: com.ismartcoding.plain.rust.graphql.ContentSource = com.ismartcoding.plain.rust.graphql.ContentSource.Local) : ViewModel() {
    private val _itemsFlow = MutableStateFlow<List<VAppFile>>(emptyList())
    val itemsFlow: StateFlow<List<VAppFile>> = _itemsFlow

    val showLoading = mutableStateOf(true)
    val offset = mutableIntStateOf(0)
    val limit = mutableIntStateOf(50)
    val noMore = mutableStateOf(false)
    val total = mutableIntStateOf(0)

    private val lock = Mutex()
    private suspend fun fetchPage(pageOffset: Int): List<VAppFile> = source.storedFiles.page(pageOffset,limit.intValue)

    suspend fun moreAsync() = withIO {
        lock.withLock {
            val nextOffset = offset.intValue + limit.intValue
            val items = fetchPage(nextOffset)
            offset.intValue = nextOffset
            _itemsFlow.update { it + items }
            noMore.value = items.size < limit.intValue
            showLoading.value = false
        }
    }

    suspend fun loadAsync() = withIO {
        lock.withLock {
            val count = source.storedFiles.count()
            val items = fetchPage(0)
            offset.intValue = 0
            total.intValue = count
            _itemsFlow.value = items
            noMore.value = items.size < limit.intValue
            showLoading.value = false
        }
    }
}
