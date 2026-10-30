package com.ismartcoding.plain.features.shares

import com.ismartcoding.plain.graphql.Mutation
import com.ismartcoding.plain.graphql.Query
import com.ismartcoding.plain.models.ShareRecord
import com.ismartcoding.plain.rust.graphql.GraphqlClient
import com.ismartcoding.plain.rust.graphql.GraphqlApi
import kotlin.time.Instant

object SharesClient : SharesApi()

open class SharesApi internal constructor(
    protected val api: GraphqlClient = GraphqlApi,
) {
    suspend fun createShare(
        name: String,
        realPaths: List<String>,
        readOnly: Boolean,
        expiresAt: Instant?,
    ): ShareRecord =
        api.execute(Mutation.createShare(name, realPaths, readOnly, expiresAt))

    suspend fun updateShare(
        id: String,
        name: String,
        expiresAt: Instant?,
        realPaths: List<String>? = null,
    ): ShareRecord =
        api.execute(Mutation.updateShare(id, name, expiresAt, realPaths))

    suspend fun deleteShare(id: String) {
        api.execute(Mutation.deleteShare(id))
    }

    suspend fun listShares(): List<ShareRecord> =
        api.execute(Query.shareRecords())

    suspend fun getShare(id: String): ShareRecord? =
        api.execute(Query.shareRecord(id))

    suspend fun buildLink(
        share: ShareRecord,
        host: String? = null,
    ): String = api.execute(Query.shareLink(share.id, host)).pageUrl
}
