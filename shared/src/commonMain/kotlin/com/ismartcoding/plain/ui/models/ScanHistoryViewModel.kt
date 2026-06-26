package com.ismartcoding.plain.ui.models

import androidx.lifecycle.ViewModel
import com.ismartcoding.plain.helpers.launchSafe
import com.ismartcoding.plain.preferences.ScanHistoryPreference
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class ScanHistoryViewModel : ViewModel() {
    private val _itemsFlow = MutableStateFlow(emptyList<String>())
    val itemsFlow = _itemsFlow.asStateFlow()

    fun fetch() {
        launchSafe {
            _itemsFlow.update {
                ScanHistoryPreference.getValueAsync()
            }
        }
    }

    fun delete(value: String) {
        launchSafe {
            _itemsFlow.update {
                val mutableList = it.toMutableList()
                mutableList.remove(value)
                ScanHistoryPreference.putAsync(mutableList)
                mutableList
            }
        }
    }
}
