package com.ismartcoding.plain.web.schemas

import com.ismartcoding.plain.lib.kgraphql.schema.dsl.SchemaBuilder
import com.ismartcoding.plain.lib.extensions.getFinalPath
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
import com.ismartcoding.plain.web.models.toModel

fun SchemaBuilder.addFileMutationSchema() {
    mutation("deleteFiles") {
        resolver("paths") { paths: List<String> ->
            Permission.WRITE_EXTERNAL_STORAGE.checkEnabledAsync()
            FilePathValidator.requireAllSafe(paths)
            paths.forEach { deleteFileOrDir(it) }
            scanFiles(paths.toTypedArray())
            true
        }
    }
    mutation("createDir") {
        resolver("path") { path: String ->
            Permission.WRITE_EXTERNAL_STORAGE.checkEnabledAsync()
            createDirectory(path).toModel()
        }
    }
    mutation("renameFile") {
        resolver("path", "name") { path: String, name: String ->
            Permission.WRITE_EXTERNAL_STORAGE.checkEnabledAsync()
            FilePathValidator.requireAllSafe(listOf(path))
            renameAndScanFile(path, name) != null
        }
    }
    mutation("writeTextFile") {
        resolver("path", "content", "overwrite") { path: String, content: String, overwrite: Boolean ->
            Permission.WRITE_EXTERNAL_STORAGE.checkEnabledAsync()
            FilePathValidator.requireAllSafe(listOf(path))
            val resolvedPath = path.getFinalPath()
            writeFileText(resolvedPath, content, overwrite).toModel()
        }
    }
    mutation("copyFile") {
        resolver("src", "dst", "overwrite") { src: String, dst: String, overwrite: Boolean ->
            Permission.WRITE_EXTERNAL_STORAGE.checkEnabledAsync()
            FilePathValidator.requireAllSafe(listOf(src, dst))
            val finalDst = if (overwrite) dst else getNewPath(dst)
            copyFileOrDir(src, finalDst)
            scanFiles(arrayOf(finalDst))
            true
        }
    }
    mutation("moveFile") {
        resolver("src", "dst", "overwrite") { src: String, dst: String, overwrite: Boolean ->
            Permission.WRITE_EXTERNAL_STORAGE.checkEnabledAsync()
            FilePathValidator.requireAllSafe(listOf(src, dst))
            val finalDst = if (overwrite) dst else getNewPath(dst)
            moveFileOrDir(src, finalDst)
            scanFiles(arrayOf(src, finalDst))
            true
        }
    }
    mutation("addFavoriteFolder") {
        resolver("rootPath", "fullPath") { rootPath: String, fullPath: String ->
            val current = FavoriteFoldersPreference.getValueAsync()
                .firstOrNull { it.fullPath == fullPath }
            val folder = DFavoriteFolder(rootPath, fullPath, alias = current?.alias)
            val updatedFolders = FavoriteFoldersPreference.addAsync(folder)
            updatedFolders.map { it.toModel() }
        }
    }
    mutation("removeFavoriteFolder") {
        resolver("fullPath") { fullPath: String ->
            val updatedFolders = FavoriteFoldersPreference.removeAsync(fullPath)
            updatedFolders.map { it.toModel() }
        }
    }
    mutation("setFavoriteFolderAlias") {
        resolver("fullPath", "alias") { fullPath: String, alias: String ->
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
            updated.map { it.toModel() }
        }
    }
}
