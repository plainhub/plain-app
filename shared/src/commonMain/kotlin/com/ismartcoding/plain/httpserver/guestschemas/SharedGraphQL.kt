package com.ismartcoding.plain.httpserver.guestschemas

import com.ismartcoding.plain.features.file.FileSortBy
import com.ismartcoding.plain.features.share.ShareManager
import com.ismartcoding.plain.lib.extensions.isImageFast
import com.ismartcoding.plain.lib.kgraphql.Context
import com.ismartcoding.plain.lib.kgraphql.GraphQLError
import com.ismartcoding.plain.lib.kgraphql.annotations.GraphQLQuery
import com.ismartcoding.plain.lib.kgraphql.annotations.GraphQLSchemaTarget
import com.ismartcoding.plain.lib.withIO
import com.ismartcoding.plain.platform.AppDatabase
import com.ismartcoding.plain.platform.getContentTypeForPath
import com.ismartcoding.plain.platform.listFilesInDir
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
        val share = AppDatabase.instance.shareDao().getById(sharedId)
            ?: throw GraphQLError("Share not found")
        if (!share.isActive) throw GraphQLError("Share is inactive or expired")
        val roots = share.data
        val entries = if (virtualPath.isNullOrBlank()) {
            // Top level: expose each whitelisted root as a directory entry.
            roots.map { SharedFile(it.virtualPath.trimEnd('/'), it.virtualPath, true, 0L, "", false) }
        } else {
            val path = virtualPath
            val realPath = ShareManager.resolveVirtualPath(share, path)
                ?: throw GraphQLError("Path not allowed")
            if (statFile(realPath)?.isDir != true) {
                emptyList()
            } else {
                val parent = path.trim('/')
                listFilesInDir(realPath, showHidden = false, sortBy = FileSortBy.NAME_ASC).map { d ->
                    SharedFile(
                        name = d.name,
                        virtualPath = if (parent.isEmpty()) d.name else "$parent/${d.name}",
                        isDir = d.isDir,
                        size = if (d.isDir) 0 else d.size,
                        mimeType = getContentTypeForPath(d.path) ?: "",
                        hasThumb = !d.isDir && d.name.isImageFast(),
                    )
                }
            }
        }
        SharedInfo(
            name = share.name,
            readOnly = share.readOnly,
            requiresPassword = share.password.isNotEmpty(),
            expiresAt = share.expiresAt?.toEpochMilliseconds(),
            urlToken = share.urlToken,
            entries = entries,
        )
    }
}