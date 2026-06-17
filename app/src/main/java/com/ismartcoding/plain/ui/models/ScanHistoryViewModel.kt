package com.ismartcoding.plain.ui.models
import com.ismartcoding.plain.preferences.*

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ismartcoding.plain.preferences.ScanHistoryPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ScanHistoryViewModel : ViewModel() {
    private val _itemsFlow = MutableStateFlow(emptyList<String>())
    val itemsFlow = _itemsFlow.asStateFlow()

    fun fetch(context: Context) {
        launchIO {
            _itemsFlow.update {
                ScanHistoryPreference.getValueAsync()
            }
        }
    }

    fun delete(
        context: Context,
        value: String,
    ) {
        launchIO {
            _itemsFlow.update {
                val mutableList = it.toMutableList()
                mutableList.remove(value)
                ScanHistoryPreference.putAsync(mutableList)
                mutableList
            }
        }
    }
}
