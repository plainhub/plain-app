package com.ismartcoding.plain.features.shares

import com.ismartcoding.plain.graphql.Mutation
import com.ismartcoding.plain.graphql.Query
import com.ismartcoding.plain.models.Share
import com.ismartcoding.plain.models.ShareInput
import com.ismartcoding.plain.rust.graphql.GraphqlClient
import com.ismartcoding.plain.rust.graphql.GraphqlApi

object SharesClient : SharesApi()

open class SharesApi internal constructor(private val api: GraphqlClient = GraphqlApi) {
    suspend fun createShare(input: ShareInput): Share = api.execute(Mutation.createShare(input))
    suspend fun updateShare(id: String, input: ShareInput): Share = api.execute(Mutation.updateShare(id, input))
    suspend fun deleteShare(id: String) { api.execute(Mutation.deleteShare(id)) }
    suspend fun listShares(): List<Share> = api.execute(Query.shares())
    suspend fun getShare(id: String): Share? = api.execute(Query.share(id))
}
