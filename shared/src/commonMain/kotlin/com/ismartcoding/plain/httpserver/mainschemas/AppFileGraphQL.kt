package com.ismartcoding.plain.httpserver.mainschemas

import com.ismartcoding.plain.lib.kgraphql.annotations.GraphQLQuery
import com.ismartcoding.plain.lib.kgraphql.schema.dsl.SchemaBuilder
import com.ismartcoding.plain.httpserver.models.AppFile
import com.ismartcoding.plain.httpserver.models.toModel

@GraphQLQuery
suspend fun appFiles(offset: Int, limit: Int, query: String): List<AppFile> =
    com.ismartcoding.plain.helpers.AppFileStore.page(offset,limit,query).map { it.appFile.toModel(it.fileName) }

@GraphQLQuery
suspend fun appFileCount(query: String): Int = com.ismartcoding.plain.helpers.AppFileStore.count(query)

fun SchemaBuilder.addAppFileSchema() {
}
