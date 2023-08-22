package com.ismartcoding.plain.ui.models

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.toMutableStateList
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ismartcoding.plain.data.preference.ScanHistoryPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ScanHistoryViewModel : ViewModel() {
    private val _itemsFlow = MutableStateFlow(mutableStateListOf<String>())
    val itemsFlow: StateFlow<List<String>> get() = _itemsFlow

    fun fetch(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            _itemsFlow.value = ScanHistoryPreference.getValueAsync(context).toMutableStateList()
        }
    }

    fun delete(context: Context, value: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val results = ScanHistoryPreference.getValueAsync(context).toMutableList()
            results.remove(value)
            ScanHistoryPreference.putAsync(context, results)
            _itemsFlow.value.remove(value)
        }
    }
}