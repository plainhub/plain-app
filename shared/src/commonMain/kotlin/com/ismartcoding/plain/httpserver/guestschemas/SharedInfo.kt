package com.ismartcoding.plain.httpserver.guestschemas

import com.ismartcoding.plain.lib.kgraphql.annotations.GraphQLType

/**
 * One entry (file or directory) exposed by a shared link.
 */
@GraphQLType
data class SharedFile(
    val name: String,
    val virtualPath: String,
    val isDir: Boolean,
    val size: Long,
    val mimeType: String,
    val hasThumb: Boolean,
)

/**
 * Result of the `sharedInfo` query: share metadata + the entries of the
 * requested [virtualPath] directory, plus the dedicated `urlToken` used to
 * fetch files via `/sfs`.
 */
@GraphQLType
data class SharedInfo(
    val name: String,
    val readOnly: Boolean,
    val requiresPassword: Boolean,
    val expiresAt: Long?,
    val urlToken: String,
    val entries: List<SharedFile>,
)