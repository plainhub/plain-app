package com.ismartcoding.plain.ui.tools

import androidx.lifecycle.ViewModel
import com.ismartcoding.plain.IData
import com.ismartcoding.plain.models.ManagedStorageRoot
import com.ismartcoding.plain.platform.system.AppFeatureType
import com.ismartcoding.plain.features.files.FilesClient
import com.ismartcoding.plain.helpers.SearchHelper
import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

internal data class ManagedContentState(
    val items: List<IData> = emptyList(),
    val roots: List<ManagedStorageRoot> = emptyList(),
    val loading: Boolean = true,
    val error: String? = null,
    val hasMore: Boolean = false,
)

internal class ManagedContentViewModel(
    private val device: ManagedDeviceViewModel,
    val feature: AppFeatureType,
    val path: String,
    val feedId: String,
) : ViewModel() {
    private val _state = MutableStateFlow(ManagedContentState())
    val state = _state.asStateFlow()
    var query = ""
    val trash = mutableStateOf(false)
    private var offset = 0

    suspend fun load(more: Boolean = false) {
        _state.value = _state.value.copy(loading = true, error = null)
        try {
            val client = device.client.await()
            if (feature == AppFeatureType.FILES && path.isEmpty()) {
                val info = client.info()
                val roots = listOf(
                    ManagedStorageRoot(info.deviceName, info.appDir),
                    ManagedStorageRoot(info.downloadsDir.trimEnd('/').substringAfterLast('/'), info.downloadsDir),
                ) + client.roots()
                _state.value = ManagedContentState(roots = roots.filter { it.path.isNotEmpty() }.distinctBy { it.path }, loading = false)
                return
            }
            if (!more) offset = 0
            val filter = when (feature) {
                AppFeatureType.FILES -> if (query.isEmpty()) "" else FilesClient.literalNameQuery(query)
                AppFeatureType.NOTES -> "trash:${trash.value} " + SearchHelper.buildTextFilter(query)
                AppFeatureType.FEEDS -> query
                else -> SearchHelper.buildTextFilter(query)
            }
            val items = client.rows(feature, path, filter, offset, 100, feedId)
            _state.value = ManagedContentState(
                items = if (more) _state.value.items + items else items,
                loading = false,
                hasMore = (feature != AppFeatureType.FEEDS || feedId.isNotEmpty()) && items.size == 100,
            )
            offset += items.size
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            _state.value = _state.value.copy(loading = false, error = error.message ?: error.toString())
        }
    }
}
