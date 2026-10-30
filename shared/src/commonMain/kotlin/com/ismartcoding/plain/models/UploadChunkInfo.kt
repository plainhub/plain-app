package com.ismartcoding.plain.models

import kotlinx.serialization.Serializable

@Serializable
internal data class UploadChunkInfo(val fileId: String, val index: Long, val size: Long)
