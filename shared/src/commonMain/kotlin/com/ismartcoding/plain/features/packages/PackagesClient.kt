package com.ismartcoding.plain.features.packages

import com.ismartcoding.plain.features.files.FileSortBy
import com.ismartcoding.plain.graphql.Query
import com.ismartcoding.plain.models.Package as AppPackage
import com.ismartcoding.plain.rust.graphql.GraphqlClient
import com.ismartcoding.plain.rust.graphql.GraphqlApi

object PackagesClient : PackagesApi()

open class PackagesApi internal constructor(protected val api: GraphqlClient = GraphqlApi) {
    suspend fun search(query: String, limit: Int, offset: Int, sortBy: FileSortBy): List<AppPackage> =
        api.execute(Query.packages(query, offset, limit, sortBy))

    suspend fun find(id: String): AppPackage? = search(
        com.ismartcoding.plain.rust.graphql.selectionQuery(listOf(id)), 1, 0, FileSortBy.NAME_ASC,
    ).singleOrNull()

    suspend fun uninstall(ids: List<String>): Boolean = api.execute(com.ismartcoding.plain.graphql.Mutation.uninstallPackages(ids))

    suspend fun manifest(id: String): String = api.execute(Query.packageManifest(id))

    suspend fun count(query: String): Int = api.execute(Query.packageCount(query))
}
