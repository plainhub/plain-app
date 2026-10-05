package com.ismartcoding.plain.features.share

import kotlinx.serialization.Serializable
import kotlin.time.Instant

@Serializable
data class SharedInfoDto(
    val name: String = "",
    val readOnly: Boolean = true,
    val requiresPassword: Boolean = false,
    /** Server wire value is epoch millis (existing guest GraphQL contract). */
    val expiresAt: Long? = null,
    val urlToken: String = "",
    val entries: List<SharedFileDto> = emptyList(),
) {
    /** [expiresAt] converted to [Instant] at the parse boundary. */
    val expiresAtInstant: Instant?
        get() = expiresAt?.let { Instant.fromEpochMilliseconds(it) }
}
