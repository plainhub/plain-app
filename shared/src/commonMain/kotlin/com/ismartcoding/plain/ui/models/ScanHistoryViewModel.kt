package com.ismartcoding.plain.ui.models

import com.ismartcoding.plain.preferences.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ismartcoding.plain.helpers.launchSafe
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class ScanHistoryViewModel : ViewModel() {
    private val _itemsFlow = MutableStateFlow(emptyList<String>())
    val itemsFlow = _itemsFlow.asStateFlow()

    fun fetch() {
        viewModelScope.launchSafe {
            _itemsFlow.update {
                UserPrefs.scanHistoryValue()
            }
        }
    }

    fun delete(value: String) {
        viewModelScope.launchSafe {
            _itemsFlow.update {
                val mutableList = it.toMutableList()
                mutableList.remove(value)
                UserPrefs.setScanHistory(mutableList)
                mutableList
            }
        }
    }
}
