package com.ismartcoding.plain.features.contacts

import com.ismartcoding.plain.graphql.Query
import com.ismartcoding.plain.models.Contact
import com.ismartcoding.plain.rust.graphql.GraphqlApi

object ContactsClient {
    suspend fun search(query: String, limit: Int, offset: Int): List<Contact> =
        GraphqlApi.execute(Query.contacts(query, limit, offset))

    suspend fun count(query: String): Int = GraphqlApi.execute(Query.contactCount(query))
}
