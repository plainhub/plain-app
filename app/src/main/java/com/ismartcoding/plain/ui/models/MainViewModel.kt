package com.ismartcoding.plain.ui.models

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ismartcoding.plain.db.DBox
import com.ismartcoding.plain.features.box.BoxHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {
    private val _boxes = MutableStateFlow(listOf<DBox>())
    val boxes: StateFlow<List<DBox>> get() = _boxes.asStateFlow()

    fun fetch() {
        viewModelScope.launch(Dispatchers.IO) {
            _boxes.value = BoxHelper.getItemsAsync()
        }
    }
}
