package com.ismartcoding.plain.web.schemas

import com.ismartcoding.plain.lib.kgraphql.annotations.GraphQLMutation
import com.ismartcoding.plain.lib.kgraphql.annotations.GraphQLQuery
import com.ismartcoding.plain.lib.kgraphql.schema.dsl.SchemaBuilder
import com.ismartcoding.plain.platform.deleteUploadedChunks
import com.ismartcoding.plain.platform.listUploadedChunks
import com.ismartcoding.plain.platform.mergeUploadedChunks

@GraphQLQuery
suspend fun uploadedChunks(fileId: String): List<String> {
    return listUploadedChunks(fileId)
}

@GraphQLMutation
suspend fun deleteChunks(fileId: String): Boolean {
    return deleteUploadedChunks(fileId)
}

@GraphQLMutation
suspend fun mergeChunks(fileId: String, totalChunks: Int, path: String, replace: Boolean, isAppFile: Boolean): String {
    return mergeUploadedChunks(fileId, totalChunks, path, replace, isAppFile)
}

fun SchemaBuilder.addFileUploadSchema() {
}
