package com.ismartcoding.plain.models

import kotlinx.serialization.Serializable

@Serializable
data class SharePath(val virtualPath: String, val realPath: String, val isDir: Boolean)
