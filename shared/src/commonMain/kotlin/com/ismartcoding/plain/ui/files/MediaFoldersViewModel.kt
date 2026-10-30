package com.ismartcoding.plain.ui.files

import com.ismartcoding.plain.rust.graphql.ContentSource
import com.ismartcoding.plain.models.MediaBucket
import com.ismartcoding.plain.i18n.*
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ismartcoding.plain.lib.TimeHelper
import com.ismartcoding.plain.lib.withIO
import com.ismartcoding.plain.platform.system.DataType
import com.ismartcoding.plain.ui.i18n.LocaleHelper
import com.ismartcoding.plain.platform.files.fileExists
import com.ismartcoding.plain.ui.components.LoadingHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class MediaFoldersViewModel internal constructor(val source: ContentSource = ContentSource.Local) : ViewModel() {
    private val _itemsFlow = MutableStateFlow<List<MediaBucket>>(emptyList())
    val itemsFlow: StateFlow<List<MediaBucket>> = _itemsFlow.asStateFlow()
    val totalBucket = mutableStateOf<MediaBucket?>(null)
    val trashCount = mutableIntStateOf(0)

    val bucketsMapFlow: StateFlow<Map<String, MediaBucket>> =
        _itemsFlow
            .map { list -> list.associateBy { it.id } }
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    var showLoading = mutableStateOf(true)
    var dataType = mutableStateOf(DataType.DEFAULT)

    suspend fun loadAsync() = withIO {
        val startTime = TimeHelper.nowMillis()
        _itemsFlow.value = source.catalog.buckets(dataType.value)
        trashCount.intValue = source.catalog.count(dataType.value, "trash:true")

        var totalValue = 0
        var sizeValue = 0L
        val subItems = mutableSetOf<String>()

        // Take one top item from each folder until we have 4 items
        for (bucket in _itemsFlow.value) {
            totalValue += bucket.itemCount
            sizeValue += bucket.size

            if (subItems.size < 4) {
                // Add the first item from each folder's topItems if available
                val validTopItems = bucket.topItemPaths.filter { source is ContentSource.Remote || fileExists(it) }
                if (validTopItems.isNotEmpty()) {
                    subItems.add(validTopItems.first())
                }
            }
        }

        // If we have fewer than 4 items and there's at least one folder with more items
        // take additional items from the first folder that has multiple items
        if (subItems.size < 4 && _itemsFlow.value.isNotEmpty()) {
            for (bucket in _itemsFlow.value) {
                val validTopItems = bucket.topItemPaths.filter { source is ContentSource.Remote || fileExists(it) }
                if (validTopItems.size > 1) {
                    // Start from the second item (index 1) since we've already added the first one
                    for (i in 1 until validTopItems.size) {
                        if (subItems.size < 4) {
                            subItems.add(validTopItems[i])
                        } else {
                            break
                        }
                    }
                }

                if (subItems.size >= 4) {
                    break
                }
            }
        }

        totalBucket.value = MediaBucket(id = "all", name = LocaleHelper.getString(Res.string.all), itemCount = totalValue, size = sizeValue, topItemPaths = subItems.toMutableList())

        LoadingHelper.ensureMinimumLoadingTime(
            viewModel = this@MediaFoldersViewModel,
            startTime = startTime,
            updateLoadingState = { isLoading -> showLoading.value = isLoading }
        )
    }

    /**
     * Async pre-validation of file existence, can be called on a background thread
     * to filter out non-existent files ahead of time, reducing UI thread burden.
     */
    suspend fun preValidateFilesAsync() = withIO {
        _itemsFlow.value.forEach { bucket ->
            val toRemove = bucket.topItemPaths.filter { source is ContentSource.Local && !fileExists(it) }
            bucket.topItemPaths = bucket.topItemPaths.filterNot { it in toRemove }
        }
    }
}
