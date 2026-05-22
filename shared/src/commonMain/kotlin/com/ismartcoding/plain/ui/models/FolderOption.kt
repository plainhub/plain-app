package com.ismartcoding.plain.ui.models

import com.ismartcoding.plain.enums.FilesType

class FolderOption(
    val rootPath: String,
    val fullPath: String,
    val type: FilesType,
    var title: String,
    var isChecked: Boolean = false,
    var isFavoriteFolder: Boolean = false
)
