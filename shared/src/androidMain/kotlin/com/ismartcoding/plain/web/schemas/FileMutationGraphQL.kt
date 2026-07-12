package com.ismartcoding.plain.web.schemas
import com.ismartcoding.plain.appContext
import com.ismartcoding.plain.preferences.*

import com.ismartcoding.plain.lib.kgraphql.GraphQLError
import com.ismartcoding.plain.lib.kgraphql.schema.dsl.SchemaBuilder
import com.ismartcoding.plain.lib.extensions.getFinalPath
import com.ismartcoding.plain.lib.extensions.scanFileByConnection
import com.ismartcoding.plain.data.DFavoriteFolder
import com.ismartcoding.plain.extensions.newPath
import com.ismartcoding.plain.platform.Permission
import com.ismartcoding.plain.platform.checkEnabledAsync
import com.ismartcoding.plain.features.checkEnabledAsync
import com.ismartcoding.plain.features.file.FileSystemHelper
import com.ismartcoding.plain.helpers.FileHelper
import com.ismartcoding.plain.helpers.FilePathValidator
import com.ismartcoding.plain.preferences.FavoriteFoldersPreference
import com.ismartcoding.plain.web.models.toModel
import java.io.File
import kotlin.io.path.Path
import kotlin.io.path.moveTo

fun SchemaBuilder.addFileMutationSchema() {
    mutation("deleteFiles") {
        resolver("paths") { paths: List<String> ->
            val context = appContext
            Permission.WRITE_EXTERNAL_STORAGE.checkEnabledAsync()
            FilePathValidator.requireAllSafe(paths)
            paths.forEach {
                File(it).deleteRecursively()
            }
            context.scanFileByConnection(paths.toTypedArray())
            true
        }
    }
    mutation("createDir") {
        resolver("path") { path: String ->
            Permission.WRITE_EXTERNAL_STORAGE.checkEnabledAsync()
            FileSystemHelper.createDirectory(path).toModel()
        }
    }
    mutation("renameFile") {
        resolver("path", "name") { path: String, name: String ->
            Permission.WRITE_EXTERNAL_STORAGE.checkEnabledAsync()
            FilePathValidator.requireAllSafe(listOf(path))
            val dst = FileHelper.rename(path, name)
            if (dst != null) {
                appContext.scanFileByConnection(path)
                appContext.scanFileByConnection(dst)
            }
            dst != null
        }
    }
    mutation("writeTextFile") {
        resolver("path", "content", "overwrite") { path: String, content: String, overwrite: Boolean ->
            Permission.WRITE_EXTERNAL_STORAGE.checkEnabledAsync()
            FilePathValidator.requireAllSafe(listOf(path))
            val resolvedPath = path.getFinalPath()
            val file = File(resolvedPath)
            if (!overwrite && file.exists()) {
                throw GraphQLError("File already exists")
            }
            file.writeText(content)
            appContext.scanFileByConnection(resolvedPath)
            file.toModel()
        }
    }
    mutation("copyFile") {
        resolver("src", "dst", "overwrite") { src: String, dst: String, overwrite: Boolean ->
            Permission.WRITE_EXTERNAL_STORAGE.checkEnabledAsync()
            FilePathValidator.requireAllSafe(listOf(src, dst))
            val dstFile = File(dst)
            if (overwrite || !dstFile.exists()) {
                File(src).copyRecursively(dstFile, overwrite)
            } else {
                File(src)
                    .copyRecursively(File(dstFile.newPath()), false)
            }
            appContext.scanFileByConnection(dstFile)
            true
        }
    }
    mutation("moveFile") {
        resolver("src", "dst", "overwrite") { src: String, dst: String, overwrite: Boolean ->
            Permission.WRITE_EXTERNAL_STORAGE.checkEnabledAsync()
            FilePathValidator.requireAllSafe(listOf(src, dst))
            val dstFile = File(dst)
            if (overwrite || !dstFile.exists()) {
                Path(src).moveTo(Path(dst), overwrite)
            } else {
                Path(src).moveTo(Path(dstFile.newPath()), false)
            }
            appContext.scanFileByConnection(src)
            appContext.scanFileByConnection(dstFile)
            true
        }
    }
    mutation("addFavoriteFolder") {
        resolver("rootPath", "fullPath") { rootPath: String, fullPath: String ->
            val context = appContext
            val current = FavoriteFoldersPreference.getValueAsync()
                .firstOrNull { it.fullPath == fullPath }
            val folder = DFavoriteFolder(rootPath, fullPath, alias = current?.alias)
            val updatedFolders = FavoriteFoldersPreference.addAsync(folder)
            updatedFolders.map { it.toModel() }
        }
    }
    mutation("removeFavoriteFolder") {
        resolver("fullPath") { fullPath: String ->
            val context = appContext
            val updatedFolders = FavoriteFoldersPreference.removeAsync(fullPath)
            updatedFolders.map { it.toModel() }
        }
    }
    mutation("setFavoriteFolderAlias") {
        resolver("fullPath", "alias") { fullPath: String, alias: String ->
            val context = appContext
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
