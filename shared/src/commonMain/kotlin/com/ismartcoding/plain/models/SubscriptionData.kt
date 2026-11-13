package com.ismartcoding.plain.models

import com.ismartcoding.plain.platform.system.DataType
import com.ismartcoding.plain.platform.system.GraphqlDataTypeSerializer
import kotlinx.serialization.Serializable

@Serializable
data class ChatItemId(val id: String)

@Serializable
data class MediaChange(val types: List<@Serializable(with = GraphqlDataTypeSerializer::class) DataType>)

@Serializable
data class MediaScanProgress(
    val indexed: Int,
    val pending: Int? = null,
    val total: Int? = null,
    val state: MediaScanState,
    val error: String? = null,
)
