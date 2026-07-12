package com.ismartcoding.plain.ui.models

import com.ismartcoding.plain.i18n.*

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ismartcoding.plain.helpers.TimeHelper
import com.ismartcoding.plain.helpers.withIO
import com.ismartcoding.plain.data.DMediaBucket
import com.ismartcoding.plain.enums.DataType
import com.ismartcoding.plain.platform.LocaleHelper
import com.ismartcoding.plain.platform.getMediaBuckets
import com.ismartcoding.plain.platform.fileExists
import com.ismartcoding.plain.ui.helpers.LoadingHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class MediaFoldersViewModel : ViewModel() {
    private val _itemsFlow = MutableStateFlow<List<DMediaBucket>>(emptyList())
    val itemsFlow: StateFlow<List<DMediaBucket>> = _itemsFlow.asStateFlow()
    val totalBucket = mutableStateOf<DMediaBucket?>(null)

    val bucketsMapFlow: StateFlow<Map<String, DMediaBucket>> =
        _itemsFlow
            .map { list -> list.associateBy { it.id } }
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    var showLoading = mutableStateOf(true)
    var dataType = mutableStateOf(DataType.DEFAULT)

    suspend fun loadAsync() = withIO {
        val startTime = TimeHelper.nowMillis()
        _itemsFlow.value = getMediaBuckets(dataType.value)

        var totalValue = 0
        var sizeValue = 0L
        val subItems = mutableSetOf<String>()

        // Take one top item from each folder until we have 4 items
        for (bucket in _itemsFlow.value) {
            totalValue += bucket.itemCount
            sizeValue += bucket.size

            if (subItems.size < 4) {
                // Add the first item from each folder's topItems if available
                val validTopItems = bucket.topItems.filter { fileExists(it) }
                if (validTopItems.isNotEmpty()) {
                    subItems.add(validTopItems.first())
                }
            }
        }

        // If we have fewer than 4 items and there's at least one folder with more items
        // take additional items from the first folder that has multiple items
        if (subItems.size < 4 && _itemsFlow.value.isNotEmpty()) {
            for (bucket in _itemsFlow.value) {
                val validTopItems = bucket.topItems.filter { fileExists(it) }
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

        totalBucket.value = DMediaBucket("all", LocaleHelper.getString(Res.string.all), totalValue, sizeValue, subItems.toMutableList())

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
            val toRemove = bucket.topItems.filter { !fileExists(it) }
            bucket.topItems.removeAll(toRemove)
        }
    }
}
