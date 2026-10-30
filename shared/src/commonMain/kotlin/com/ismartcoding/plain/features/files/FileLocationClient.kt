package com.ismartcoding.plain.features.files

import com.ismartcoding.plain.graphql.Mutation
import com.ismartcoding.plain.graphql.Query
import com.ismartcoding.plain.lib.JsonHelper
import com.ismartcoding.plain.rust.graphql.GraphqlApi
import com.ismartcoding.plain.rust.graphql.GraphqlClient

object FileLocationClient : FileLocationApi()

open class FileLocationApi internal constructor(private val api: GraphqlClient = GraphqlApi) {
    suspend fun lastPath(): FilePathData = api.execute(Query.savedValue("last_file_path", FilePathData("", "", "")))

    suspend fun saveLastPath(value: FilePathData) {
        api.execute(Mutation.setSavedValue("last_file_path", JsonHelper.jsonEncodeToElement(value)))
    }

    suspend fun recentSaveDirs(): List<String> = api.execute(Query.savedValue("recent_save_dirs", emptyList()))

    suspend fun recordSaveDir(path: String) {
        api.execute(Mutation.setSavedValue("recent_save_dirs", JsonHelper.jsonEncodeToElement((listOf(path) + recentSaveDirs().filterNot { it == path }).take(5))))
    }
}
