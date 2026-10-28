package com.ismartcoding.plain.features.telephony.sms

import com.ismartcoding.plain.graphql.Query
import com.ismartcoding.plain.models.Sms
import com.ismartcoding.plain.rust.graphql.GraphqlApi

object RustSmsQuery {
    suspend fun search(query: String, limit: Int, offset: Int): List<Sms> =
        GraphqlApi.execute(Query.sms(query, offset, limit))

    suspend fun count(query: String): Int =
        GraphqlApi.execute(Query.smsCount(query))
}
