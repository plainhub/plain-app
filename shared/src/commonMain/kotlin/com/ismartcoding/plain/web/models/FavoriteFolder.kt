package com.ismartcoding.plain.web.models

import com.ismartcoding.plain.data.DFavoriteFolder
import com.ismartcoding.plain.lib.kgraphql.annotations.GraphQLType

@GraphQLType
data class FavoriteFolder(
    val rootPath: String,
    val fullPath: String,
    val alias: String? = null,
)

fun DFavoriteFolder.toModel(): FavoriteFolder {
    return FavoriteFolder(rootPath, fullPath, alias)
}