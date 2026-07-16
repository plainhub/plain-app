package com.ismartcoding.plain.ui.components

import kotlinx.serialization.Serializable

@Serializable
data class EditorData(val language: String, val wrapContent: Boolean, val isDarkTheme: Boolean, val readOnly: Boolean, val gotoEnd: Boolean, val content: String)
