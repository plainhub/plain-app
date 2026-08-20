package com.ismartcoding.plain.httpserver

import kotlinx.serialization.Serializable


@Serializable
data class FileIdParams(
    val path: String = "",
    val mediaId: String = "",
    val name: String = "",
)

/**
 * Payload encrypted inside a `/sfs?id` parameter, encrypted with the share's
 * dedicated `url_token`. [sharedId] binds the request to a single share and
 * [virtualPath] is a public (root-relative) path later whitelisted against the
 * share's roots.
 */
@Serializable
data class ShareFileParams(
    val sharedId: String = "",
    val virtualPath: String = "",
)
