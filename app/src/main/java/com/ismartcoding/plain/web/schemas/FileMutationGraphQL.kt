package com.ismartcoding.plain.web.schemas

import com.ismartcoding.lib.kgraphql.GraphQLError
import com.ismartcoding.lib.kgraphql.schema.dsl.SchemaBuilder
import com.ismartcoding.lib.extensions.getFinalPath
import com.ismartcoding.lib.extensions.scanFileByConnection
import com.ismartcoding.plain.MainApp
import com.ismartcoding.plain.data.DFavoriteFolder
import com.ismartcoding.plain.extensions.newPath
import com.ismartcoding.plain.features.Permission
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
        resolver { paths: List<String> ->
            val context = MainApp.instance
            Permission.WRITE_EXTERNAL_STORAGE.checkAsync(context)
            FilePathValidator.requireAllSafe(paths)
            paths.forEach {
                File(it).deleteRecursively()
            }
            context.scanFileByConnection(paths.toTypedArray())
            true
        }
    }
    mutation("createDir") {
        resolver { path: String ->
            Permission.WRITE_EXTERNAL_STORAGE.checkAsync(MainApp.instance)
            FileSystemHelper.createDirectory(path).toModel()
        }
    }
    mutation("renameFile") {
        resolver { path: String, name: String ->
            Permission.WRITE_EXTERNAL_STORAGE.checkAsync(MainApp.instance)
            FilePathValidator.requireAllSafe(listOf(path))
            val dst = FileHelper.rename(path, name)
            if (dst != null) {
                MainApp.instance.scanFileByConnection(path)
                MainApp.instance.scanFileByConnection(dst)
            }
            dst != null
        }
    }
    mutation("writeTextFile") {
        resolver { path: String, content: String, overwrite: Boolean ->
            Permission.WRITE_EXTERNAL_STORAGE.checkAsync(MainApp.instance)
            FilePathValidator.requireAllSafe(listOf(path))
            val resolvedPath = path.getFinalPath(MainApp.instance)
            val file = File(resolvedPath)
            if (!overwrite && file.exists()) {
                throw GraphQLError("File already exists")
            }
            file.writeText(content)
            MainApp.instance.scanFileByConnection(resolvedPath)
            file.toModel()
        }
    }
    mutation("copyFile") {
        resolver { src: String, dst: String, overwrite: Boolean ->
            Permission.WRITE_EXTERNAL_STORAGE.checkAsync(MainApp.instance)
            FilePathValidator.requireAllSafe(listOf(src, dst))
            val dstFile = File(dst)
            if (overwrite || !dstFile.exists()) {
                File(src).copyRecursively(dstFile, overwrite)
            } else {
                File(src)
                    .copyRecursively(File(dstFile.newPath()), false)
            }
            MainApp.instance.scanFileByConnection(dstFile)
            true
        }
    }
    mutation("moveFile") {
        resolver { src: String, dst: String, overwrite: Boolean ->
            Permission.WRITE_EXTERNAL_STORAGE.checkAsync(MainApp.instance)
            FilePathValidator.requireAllSafe(listOf(src, dst))
            val dstFile = File(dst)
            if (overwrite || !dstFile.exists()) {
                Path(src).moveTo(Path(dst), overwrite)
            } else {
                Path(src).moveTo(Path(dstFile.newPath()), false)
            }
            MainApp.instance.scanFileByConnection(src)
            MainApp.instance.scanFileByConnection(dstFile)
            true
        }
    }
    mutation("addFavoriteFolder") {
        resolver { rootPath: String, fullPath: String ->
            val context = MainApp.instance
            val current = FavoriteFoldersPreference.getValueAsync(context)
                .firstOrNull { it.fullPath == fullPath }
            val folder = DFavoriteFolder(rootPath, fullPath, alias = current?.alias)
            val updatedFolders = FavoriteFoldersPreference.addAsync(context, folder)
            updatedFolders.map { it.toModel() }
        }
    }
    mutation("removeFavoriteFolder") {
        resolver { fullPath: String ->
            val context = MainApp.instance
            val updatedFolders = FavoriteFoldersPreference.removeAsync(context, fullPath)
            updatedFolders.map { it.toModel() }
        }
    }
    mutation("setFavoriteFolderAlias") {
        resolver { fullPath: String, alias: String ->
            val context = MainApp.instance
            val trimmed = alias.trim()
            val updated = FavoriteFoldersPreference.getValueAsync(context)
                .map {
                    if (it.fullPath == fullPath) {
                        it.copy(alias = trimmed)
                    } else {
                        it
                    }
                }
            FavoriteFoldersPreference.putAsync(context, updated)
            updated.map { it.toModel() }
        }
    }
}
