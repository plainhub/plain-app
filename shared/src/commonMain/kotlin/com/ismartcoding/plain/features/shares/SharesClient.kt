package com.ismartcoding.plain.features.shares

import com.ismartcoding.plain.graphql.Mutation
import com.ismartcoding.plain.graphql.Query
import com.ismartcoding.plain.models.ShareRecord
import com.ismartcoding.plain.rust.graphql.GraphqlApi
import kotlin.time.Instant

object SharesClient {
    suspend fun createShare(
        name: String,
        realPaths: List<String>,
        readOnly: Boolean,
        expiresAt: Instant?,
    ): ShareRecord =
        GraphqlApi.execute(Mutation.createShare(name, realPaths, readOnly, expiresAt))

    suspend fun updateShare(
        id: String,
        name: String,
        expiresAt: Instant?,
        realPaths: List<String>? = null,
    ): ShareRecord =
        GraphqlApi.execute(Mutation.updateShare(id, name, expiresAt, realPaths))

    suspend fun deleteShare(id: String) {
        GraphqlApi.execute(Mutation.deleteShare(id))
    }

    suspend fun listShares(): List<ShareRecord> =
        GraphqlApi.execute(Query.shareRecords())

    suspend fun getShare(id: String): ShareRecord? =
        GraphqlApi.execute(Query.shareRecord(id))

    suspend fun buildLink(
        share: ShareRecord,
        host: String? = null,
    ): String = SharedLinkClient.pageUrl(SharedLinkClient.ownLink(share.id, host))
}
