package com.ismartcoding.plain.features.chat.models

import com.ismartcoding.plain.lib.TimeHelper
import kotlin.time.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class LinkPreview(
    val url: String,
    val title: String? = null,
    val description: String? = null,
    val image: String? = null,
    val imageWidth: Int = 0,
    val imageHeight: Int = 0,
    val siteName: String? = null,
    val domain: String? = null,
    @Transient val hasError: Boolean = false,
    val createdAt: Instant = TimeHelper.now()
) {
    /** A preview never loads a remote image or an ordinary filesystem path. */
    val imageReference: String?
        get() = image?.takeIf { it.startsWith("fid:") && it.length > 4 }
}
