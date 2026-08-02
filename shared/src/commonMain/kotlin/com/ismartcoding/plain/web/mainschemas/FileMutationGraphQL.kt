package com.ismartcoding.plain.web.mainschemas

import com.ismartcoding.plain.lib.kgraphql.annotations.GraphQLMutation
import com.ismartcoding.plain.lib.kgraphql.schema.dsl.SchemaBuilder
import com.ismartcoding.plain.extensions.getFinalPath
import com.ismartcoding.plain.data.DFavoriteFolder
import com.ismartcoding.plain.platform.Permission
import com.ismartcoding.plain.platform.checkEnabledAsync
import com.ismartcoding.plain.platform.copyFileOrDir
import com.ismartcoding.plain.platform.createDirectory
import com.ismartcoding.plain.platform.deleteFileOrDir
import com.ismartcoding.plain.platform.getNewPath
import com.ismartcoding.plain.platform.moveFileOrDir
import com.ismartcoding.plain.platform.renameAndScanFile
import com.ismartcoding.plain.platform.scanFiles
import com.ismartcoding.plain.platform.writeFileText
import com.ismartcoding.plain.helpers.FilePathValidator
import com.ismartcoding.plain.preferences.FavoriteFoldersPreference
import com.ismartcoding.plain.web.models.FavoriteFolder
import com.ismartcoding.plain.web.models.File
import com.ismartcoding.plain.web.models.toModel

@GraphQLMutation
suspend fun deleteFiles(paths: List<String>): Boolean {
    Permission.WRITE_EXTERNAL_STORAGE.checkEnabledAsync()
    FilePathValidator.requireAllSafe(paths)
    paths.forEach { deleteFileOrDir(it) }
    scanFiles(paths.toTypedArray())
    return true
}

@GraphQLMutation
suspend fun createDir(path: String): File {
    Permission.WRITE_EXTERNAL_STORAGE.checkEnabledAsync()
    return createDirectory(path).toModel()
}

@GraphQLMutation
suspend fun renameFile(path: String, name: String): Boolean {
    Permission.WRITE_EXTERNAL_STORAGE.checkEnabledAsync()
    FilePathValidator.requireAllSafe(listOf(path))
    return renameAndScanFile(path, name) != null
}

@GraphQLMutation
suspend fun writeTextFile(path: String, content: String, overwrite: Boolean): File {
    Permission.WRITE_EXTERNAL_STORAGE.checkEnabledAsync()
    FilePathValidator.requireAllSafe(listOf(path))
    val resolvedPath = path.getFinalPath()
    return writeFileText(resolvedPath, content, overwrite).toModel()
}

@GraphQLMutation
suspend fun copyFile(src: String, dst: String, overwrite: Boolean): Boolean {
    Permission.WRITE_EXTERNAL_STORAGE.checkEnabledAsync()
    FilePathValidator.requireAllSafe(listOf(src, dst))
    val finalDst = if (overwrite) dst else getNewPath(dst)
    copyFileOrDir(src, finalDst)
    scanFiles(arrayOf(finalDst))
    return true
}

@GraphQLMutation
suspend fun moveFile(src: String, dst: String, overwrite: Boolean): Boolean {
    Permission.WRITE_EXTERNAL_STORAGE.checkEnabledAsync()
    FilePathValidator.requireAllSafe(listOf(src, dst))
    val finalDst = if (overwrite) dst else getNewPath(dst)
    moveFileOrDir(src, finalDst)
    scanFiles(arrayOf(src, finalDst))
    return true
}

@GraphQLMutation
suspend fun addFavoriteFolder(rootPath: String, fullPath: String): List<FavoriteFolder> {
    val current = FavoriteFoldersPreference.getValueAsync()
        .firstOrNull { it.fullPath == fullPath }
    val folder = DFavoriteFolder(rootPath, fullPath, alias = current?.alias)
    val updatedFolders = FavoriteFoldersPreference.addAsync(folder)
    return updatedFolders.map { it.toModel() }
}

@GraphQLMutation
suspend fun removeFavoriteFolder(fullPath: String): List<FavoriteFolder> {
    val updatedFolders = FavoriteFoldersPreference.removeAsync(fullPath)
    return updatedFolders.map { it.toModel() }
}

@GraphQLMutation
suspend fun setFavoriteFolderAlias(fullPath: String, alias: String): List<FavoriteFolder> {
    val trimmed = alias.trim()
    val updated = FavoriteFoldersPreference.getValueAsync()
        .map {
            if (it.fullPath == fullPath) {
                it.copy(alias = trimmed)
            } else {
                it
            }
        }
    FavoriteFoldersPreference.putAsync(updated)
    return updated.map { it.toModel() }
}

fun SchemaBuilder.addFileMutationSchema() {
}
