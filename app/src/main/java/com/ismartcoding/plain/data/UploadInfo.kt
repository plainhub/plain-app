package com.ismartcoding.plain.data

import kotlinx.serialization.Serializable

@Serializable
data class UploadInfo(val dir: String, val replace: Boolean)

@Serializable
data class UploadChunkInfo(val fileId: String, val index: Int)
