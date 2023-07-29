package com.ismartcoding.plain.ui.models

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel

class SharedViewModel : ViewModel() {
    val chatContent = mutableStateOf("")
    val textTitle = mutableStateOf("")
    val textContent = mutableStateOf("")
}