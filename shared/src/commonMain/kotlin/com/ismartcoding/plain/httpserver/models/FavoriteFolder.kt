package com.ismartcoding.plain.httpserver.models

import com.ismartcoding.plain.data.DFavoriteFolder

data class FavoriteFolder(
    val rootPath: String,
    val fullPath: String,
    val alias: String? = null,
)

fun DFavoriteFolder.toModel(): FavoriteFolder {
    return FavoriteFolder(rootPath, fullPath, alias)
}