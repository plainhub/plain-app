package com.ismartcoding.plain.httpserver

import kotlinx.serialization.Serializable


@Serializable
data class FileIdParams(
    val path: String = "",
    val mediaId: String = "",
    val name: String = "",
)
