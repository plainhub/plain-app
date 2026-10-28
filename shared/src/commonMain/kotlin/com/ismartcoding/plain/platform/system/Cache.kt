package com.ismartcoding.plain.platform.system

import com.ismartcoding.plain.graphql.Mutation
import com.ismartcoding.plain.graphql.Query
import com.ismartcoding.plain.rust.graphql.GraphqlApi

suspend fun getCacheSize(): Long = GraphqlApi.execute(Query.appCacheSize())

suspend fun clearCacheAsync() {
    check(GraphqlApi.execute(Mutation.clearAppCache()))
}
