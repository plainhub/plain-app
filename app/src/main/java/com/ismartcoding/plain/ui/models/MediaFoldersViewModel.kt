package com.ismartcoding.plain.ui.models

import android.content.Context
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.toMutableStateList
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ismartcoding.plain.R
import com.ismartcoding.plain.data.DMediaBucket
import com.ismartcoding.plain.enums.DataType
import com.ismartcoding.plain.features.locale.LocaleHelper
import com.ismartcoding.plain.features.media.AudioMediaStoreHelper
import com.ismartcoding.plain.features.media.ImageMediaStoreHelper
import com.ismartcoding.plain.features.media.VideoMediaStoreHelper
import com.ismartcoding.plain.ui.helpers.LoadingHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(androidx.lifecycle.viewmodel.compose.SavedStateHandleSaveableApi::class)
class MediaFoldersViewModel : ViewModel() {
    private val _itemsFlow = MutableStateFlow<List<DMediaBucket>>(emptyList())
    val itemsFlow: StateFlow<List<DMediaBucket>> get() = _itemsFlow
    val totalBucket = mutableStateOf<DMediaBucket?>(null)

    val bucketsMapFlow: StateFlow<Map<String, DMediaBucket>> =
        _itemsFlow
            .map { list -> list.associateBy { it.id } }
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    var showLoading = mutableStateOf(true)
    var dataType = mutableStateOf(DataType.DEFAULT)

    fun loadAsync(context: Context) {
        val startTime = System.currentTimeMillis()
        _itemsFlow.value = (when (dataType.value) {
            DataType.IMAGE -> {
                ImageMediaStoreHelper.getBucketsAsync(context)
            }

            DataType.VIDEO -> {
                VideoMediaStoreHelper.getBucketsAsync(context)
            }

            DataType.AUDIO -> {
                AudioMediaStoreHelper.getBucketsAsync(context)
            }

            else -> {
                emptyList()
            }
        }).toMutableStateList()

        var totalValue = 0
        var sizeValue = 0L
        val subItems = mutableSetOf<String>()

        // Take one top item from each folder until we have 4 items
        for (bucket in _itemsFlow.value) {
            totalValue += bucket.itemCount
            sizeValue += bucket.size

            // Add the first item from each folder's topItems if available
            val validTopItems = bucket.topItems.filter { File(it).exists() }
            if (validTopItems.isNotEmpty()) {
                subItems.add(validTopItems.first())
            }

            // Stop if we've collected 4 items
            if (subItems.size >= 4) {
                break
            }
        }

        // If we have fewer than 4 items and there's at least one folder with more items
        // take additional items from the first folder that has multiple items
        if (subItems.size < 4 && _itemsFlow.value.isNotEmpty()) {
            for (bucket in _itemsFlow.value) {
                val validTopItems = bucket.topItems.filter { File(it).exists() }
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

        totalBucket.value = DMediaBucket("all", LocaleHelper.getString(R.string.all), totalValue, sizeValue, subItems.toMutableList())

        LoadingHelper.ensureMinimumLoadingTime(
            viewModel = this,
            startTime = startTime,
            updateLoadingState = { isLoading -> showLoading.value = isLoading }
        )
    }

    /**
     * 异步预验证文件存在性，可以在后台线程中调用
     * 这样可以提前过滤掉不存在的文件，减少UI线程的负担
     */
    suspend fun preValidateFilesAsync() = withContext(Dispatchers.IO) {
        _itemsFlow.value.forEach { bucket ->
            // 过滤掉不存在的文件
            bucket.topItems.removeAll { !File(it).exists() }
        }
    }
}
