package com.ismartcoding.plain.ui.models

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ismartcoding.plain.preference.ScanHistoryPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ScanHistoryViewModel : ViewModel() {
    private val _itemsFlow = MutableStateFlow(emptyList<String>())
    val itemsFlow: StateFlow<List<String>> get() = _itemsFlow

    fun fetch(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            _itemsFlow.update {
                ScanHistoryPreference.getValueAsync(context)
            }
        }
    }

    fun delete(
        context: Context,
        value: String,
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            _itemsFlow.update {
                val mutableList = it.toMutableList()
                mutableList.remove(value)
                ScanHistoryPreference.putAsync(context, mutableList)
                mutableList
            }
        }
    }
}
