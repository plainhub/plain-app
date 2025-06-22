package com.ismartcoding.plain.web.models

import com.ismartcoding.plain.data.DFavoriteFolder

data class FavoriteFolder(
    val rootPath: String,
    val fullPath: String
)

fun DFavoriteFolder.toModel(): FavoriteFolder {
    return FavoriteFolder(rootPath, fullPath)
}