package com.ismartcoding.plain.ui.models

import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.ismartcoding.plain.db.DClipboard
import com.ismartcoding.plain.features.ClipboardHelper
import com.ismartcoding.plain.lib.withIO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class ClipboardHistoryViewModel : ViewModel() {
    private val updates = Mutex()
    private val _itemsFlow = MutableStateFlow<List<DClipboard>>(emptyList())
    val itemsFlow: StateFlow<List<DClipboard>> = _itemsFlow

    private val _sourceNames = MutableStateFlow<Map<String, String>>(emptyMap())

    var isLoading = mutableStateOf(true)
    var offset = mutableIntStateOf(0)
    var limit = mutableIntStateOf(50)
    var noMore = mutableStateOf(false)

    suspend fun loadAsync() = updates.withLock {
        withIO {
            offset.intValue = 0
            val items = ClipboardHelper.getPage(limit.intValue, 0)
            _itemsFlow.value = items
            resolveSources(items)
            noMore.value = items.size < limit.intValue
            isLoading.value = false
        }
    }

    suspend fun moreAsync() = updates.withLock {
        withIO {
            if (noMore.value) return@withIO
            val nextOffset = offset.intValue + limit.intValue
            val items = ClipboardHelper.getPage(limit.intValue, nextOffset)
            offset.intValue = nextOffset
            _itemsFlow.update { it + items }
            resolveSources(items)
            noMore.value = items.size < limit.intValue
        }
    }

    suspend fun deleteAsync(id: String) = updates.withLock {
        withIO {
            ClipboardHelper.deleteByIds(listOf(id))
            _itemsFlow.update { items -> items.filter { it.id != id } }
        }
    }

    suspend fun clearAllAsync() = updates.withLock {
        withIO {
            ClipboardHelper.clear()
            _itemsFlow.value = emptyList()
            noMore.value = true
        }
    }

    /** Peer name for a source client id; empty string falls back to "this device". */
    fun sourceNameFor(entry: DClipboard): String {
        if (entry.source.isBlank()) return ""
        return _sourceNames.value[entry.source] ?: entry.source
    }

    private suspend fun resolveSources(entries: List<DClipboard>) = withIO {
        val unknown = entries.map { it.source }.filter { it.isNotBlank() && it !in _sourceNames.value }.distinct()
        if (unknown.isEmpty()) return@withIO
        _sourceNames.update { it + unknown.associateWith { id -> ClipboardHelper.getSourceName(id) } }
    }
}
