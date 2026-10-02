package com.ismartcoding.plain.httpserver.guestschemas

import com.ismartcoding.plain.features.share.ShareManager
import com.ismartcoding.plain.lib.extensions.isImageFast
import com.ismartcoding.plain.lib.kgraphql.Context
import com.ismartcoding.plain.lib.kgraphql.annotations.GraphQLQuery
import com.ismartcoding.plain.lib.kgraphql.annotations.GraphQLSchemaTarget
import com.ismartcoding.plain.lib.withIO
import com.ismartcoding.plain.platform.getContentTypeForPath
import com.ismartcoding.plain.platform.statFile
import com.ismartcoding.plain.httpserver.http.GraphqlRequestContext

/**
 * The only query exposed to a shared-file link (`/guest_graphql`). Returns the
 * share metadata (name, read-only, expiry, the dedicated `url_token` for
 * `/fs` / `/zip/dir`) plus the entries of the requested [virtualPath] directory. Clients
 * select whichever fields they need — GraphQL handles projection.
 *
 * Authentication is the request itself: the body is encrypted with the derived
 * `shared_token` and the `c-id` header carries the `shared_id`, so [consumers]
 * never present a stored token.
 */
@GraphQLQuery(name = "sharedInfo", target = GraphQLSchemaTarget.GUEST)
suspend fun sharedInfo(context: Context, virtualPath: String? = null): SharedInfo {
    val ctx = context.get<GraphqlRequestContext>()!!
    val sharedId = ctx.header("c-id") ?: ""
    return withIO {
        val (share, roots) = ShareManager.browse(sharedId, virtualPath.orEmpty())
        SharedInfo(
            name = share.name,
            readOnly = share.readOnly,
            requiresPassword = share.password.isNotEmpty(),
            expiresAt = share.expiresAt?.toEpochMilliseconds(),
            urlToken = share.urlToken,
            entries = roots.map { root ->
                val name = root.virtualPath.trimEnd('/').substringAfterLast('/')
                SharedFile(name = name, virtualPath = root.virtualPath, isDir = root.isDir,
                    size = if (root.isDir) 0L else statFile(root.realPath)?.size ?: 0L,
                    mimeType = if (root.isDir) "" else getContentTypeForPath(root.realPath) ?: "",
                    hasThumb = !root.isDir && name.isImageFast())
            },
        )
    }
}
