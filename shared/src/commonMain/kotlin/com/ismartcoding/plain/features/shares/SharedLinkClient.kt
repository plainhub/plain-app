package com.ismartcoding.plain.features.shares

import com.ismartcoding.plain.features.chat.models.MessageShare
import com.ismartcoding.plain.graphql.Mutation
import com.ismartcoding.plain.graphql.Query
import com.ismartcoding.plain.rust.graphql.GraphqlApi

object SharedLinkClient : SharedLinkApi()

open class SharedLinkApi internal constructor(private val api: com.ismartcoding.plain.rust.graphql.GraphqlClient = GraphqlApi) {
    suspend fun ownLink(id: String, host: String? = null): SharedLink =
        api.execute(Query.shareLink(id, host))

    suspend fun initialLink(messageId: String, expected: MessageShare): SharedLink =
        api.execute(Query.initialSharedLink(messageId, expected))

    suspend fun browse(messageId: String, expected: MessageShare, relativePath: String?): SharedBrowseResult? =
        api.execute(Mutation.browseSharedLink(messageId, expected, relativePath))

    suspend fun cacheFile(
        link: SharedLink,
        urlToken: String,
        entry: SharedFileDto,
        thumbnail: Boolean = false,
    ): String =
        api.execute(Mutation.cacheSharedFile(link, urlToken, entry, thumbnail))
}
