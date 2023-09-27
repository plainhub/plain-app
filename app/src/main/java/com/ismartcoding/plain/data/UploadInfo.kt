package com.ismartcoding.plain.data

import kotlinx.serialization.Serializable

@Serializable
data class UploadInfo(val dir: String, val replace: Boolean, val index: Int, val total: Int, val size: Long)
