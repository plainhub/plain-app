package com.ismartcoding.plain.features.file

data class FileTaskOp(val src: String, val dst: String, val overwrite: Boolean = false)
