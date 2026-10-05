package com.ismartcoding.plain.features.share

import kotlinx.serialization.Serializable

@Serializable
data class SharedLink(
    val host: String,
    val port: Int,
    val sharedId: String,
    val token: String,
    val pageUrl: String = "",
)
