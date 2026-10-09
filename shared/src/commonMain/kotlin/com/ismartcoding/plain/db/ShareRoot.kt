package com.ismartcoding.plain.db

import kotlinx.serialization.Serializable

/**
 * A whitelisted root of a share. Stored as JSON inside `shares.data` (no
 * separate table). Browsing enumerates the real path live so new files appear
 * automatically; traversal is blocked by ensuring every requested `virtualPath`
 * stays under one of these roots.
 */
@Serializable
data class ShareRoot(
    var virtualPath: String = "",
    var realPath: String = "",
    var isDir: Boolean = true,
)
