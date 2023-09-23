package com.ismartcoding.plain.ui.models

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.ismartcoding.plain.ui.preview.PreviewItem

class SharedViewModel : ViewModel() {
    val chatContent = mutableStateOf("")
    val textTitle = mutableStateOf("")
    val textContent = mutableStateOf("")
    var previewItems = mutableStateOf(listOf<PreviewItem>())
    val previewKey = mutableStateOf("")
}
