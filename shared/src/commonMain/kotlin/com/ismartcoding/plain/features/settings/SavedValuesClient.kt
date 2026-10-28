package com.ismartcoding.plain.features.settings

import com.ismartcoding.plain.graphql.Mutation
import com.ismartcoding.plain.graphql.Query
import com.ismartcoding.plain.lib.JsonHelper
import com.ismartcoding.plain.rust.graphql.GraphqlApi

object SavedValuesClient {
    suspend inline fun <reified T> get(key: String, default: T): T =
        GraphqlApi.execute(Query.savedValue(key, default))

    suspend inline fun <reified T> set(key: String, value: T) {
        GraphqlApi.execute(Mutation.setSavedValue(key, JsonHelper.jsonEncodeToElement(value)))
    }
}
