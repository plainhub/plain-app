package com.ismartcoding.plain.models

import kotlinx.serialization.Serializable

@Serializable
data class Root(
    var relativePath: String,
    var realPath: String,
    var isDir: Boolean,
)
