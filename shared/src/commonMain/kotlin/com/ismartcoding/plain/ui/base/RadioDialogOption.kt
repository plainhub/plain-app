package com.ismartcoding.plain.ui.base

data class RadioDialogOption(
    val text: String = "",
    val selected: Boolean = false,
    val onClick: () -> Unit = {},
)
