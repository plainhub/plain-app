package com.ismartcoding.plain.features.keyvalue

import com.ismartcoding.plain.graphql.Mutation
import com.ismartcoding.plain.graphql.Query
import com.ismartcoding.plain.lib.JsonHelper
import com.ismartcoding.plain.rust.graphql.GraphqlApi

object KeyValueClient {
    suspend inline fun <reified T> get(key: String, default: T): T =
        GraphqlApi.execute(Query.keyValue(key, default))

    suspend inline fun <reified T> set(key: String, value: T) {
        GraphqlApi.execute(Mutation.setKeyValue(key, JsonHelper.jsonEncodeToElement(value)))
    }
}
