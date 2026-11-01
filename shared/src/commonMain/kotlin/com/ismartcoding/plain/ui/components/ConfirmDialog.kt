package com.ismartcoding.plain.ui.components

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class ConfirmDialogCheckbox(val label: String) {
    var checked by mutableStateOf(false)
}

class ConfirmDialog(
    val title: String,
    val message: String,
    val confirmButton: Pair<String, () -> Unit>,
    val dismissButton: Pair<String, () -> Unit>?,
    val danger: Boolean = false,
    val checkbox: ConfirmDialogCheckbox? = null,
)
