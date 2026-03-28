package com.ismartcoding.plain.ui.models

import com.ismartcoding.plain.db.DAppFile

data class VAppFile(
    val appFile: DAppFile,
    val fileName: String,
)