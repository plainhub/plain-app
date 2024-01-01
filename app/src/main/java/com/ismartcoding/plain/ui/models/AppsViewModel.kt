package com.ismartcoding.plain.ui.models

import android.graphics.drawable.Drawable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ismartcoding.plain.db.DSession
import com.ismartcoding.plain.features.pkg.DCertificate
import com.ismartcoding.plain.features.pkg.DPackage
import com.ismartcoding.plain.features.pkg.PackageHelper
import com.ismartcoding.plain.packageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant

data class VPackage(
    val id: String,
    val name: String,
    val type: String,
    val version: String,
    val path: String,
    val size: Long,
    val certs: List<DCertificate>,
    val installedAt: Instant,
    val updatedAt: Instant,
    val icon: Drawable,
) {
    companion object {
        fun from(data: DPackage): VPackage {
            return VPackage(
                data.id,
                data.name,
                data.type,
                data.version,
                data.path,
                data.size,
                data.certs,
                data.installedAt,
                data.updatedAt,
                packageManager.getApplicationIcon(data.app)
            )
        }
    }
}

class AppsViewModel : ViewModel() {
    private val _itemsFlow = MutableStateFlow(mutableStateListOf<VPackage>())
    val itemsFlow: StateFlow<List<VPackage>> get() = _itemsFlow
    var showLoading = mutableStateOf(true)
    var offset = mutableStateOf(0)
    var limit = mutableStateOf(100)
    var noMore = mutableStateOf(false)

    fun loadMore() {
        viewModelScope.launch(Dispatchers.IO) {
            offset.value += limit.value
            val items = PackageHelper.search("", limit.value, offset.value).map { VPackage.from(it) }
            _itemsFlow.value.addAll(items)
            showLoading.value = false
            noMore.value = items.size < limit.value
        }
    }

    fun load() {
        viewModelScope.launch(Dispatchers.IO) {
            offset.value = 0
            _itemsFlow.value = PackageHelper.search("", limit.value, 0).map { VPackage.from(it) }.toMutableStateList()
            showLoading.value = false
            noMore.value = _itemsFlow.value.size < limit.value
        }
    }

    fun uninstall(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
        }
    }
}
