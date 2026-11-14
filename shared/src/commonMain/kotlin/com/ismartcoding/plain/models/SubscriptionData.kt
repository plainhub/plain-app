package com.ismartcoding.plain.models

import kotlinx.serialization.Serializable

@Serializable
data class ChatItemId(val id: String)

@Serializable
data class MediaChange(val types: List<MediaDataType>)

@Serializable
data class FileIndexScanProgress(
    val indexed: Int,
    val pending: Int? = null,
    val total: Int? = null,
    val state: FileIndexScanState,
    val error: String? = null,
)
