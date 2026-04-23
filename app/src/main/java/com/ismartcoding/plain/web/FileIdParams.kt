package com.ismartcoding.plain.web

import kotlinx.serialization.Serializable


@Serializable
data class FileIdParams(
    val path: String = "",
    val mediaId: String = "",
    val name: String = "",
)