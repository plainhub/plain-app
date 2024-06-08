package com.ismartcoding.plain.data

import kotlinx.serialization.Serializable
import java.io.File

@Serializable
data class DownloadFileItem(val path: String, val name: String = "")
data class DownloadFileItemWrap(val file: File, val name: String)
