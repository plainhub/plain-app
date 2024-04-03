package com.ismartcoding.plain.ui.models

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel

class UpdateViewModel : ViewModel() {
    var updateDialogVisible = mutableStateOf(false)

    fun showDialog() {
        updateDialogVisible.value = true
    }

    fun hideDialog() {
        updateDialogVisible.value = false
    }
}