package com.ismartcoding.plain.data

import kotlinx.serialization.Serializable

@Serializable
data class DownloadFileItem(val path: String, val name: String = "")