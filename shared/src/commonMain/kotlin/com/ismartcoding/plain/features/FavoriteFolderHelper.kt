package com.ismartcoding.plain.features

import com.ismartcoding.plain.api.*
import com.ismartcoding.plain.data.DFavoriteFolder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.*

object FavoriteFolderHelper {
    private const val fields = "rootPath fullPath alias"
    private val lock = Mutex()
    private val state = MutableStateFlow<List<DFavoriteFolder>>(emptyList())
    val items = state.asStateFlow()
    suspend fun refresh(): List<DFavoriteFolder> = lock.withLock {
        apply(RustContentApi.query("favoriteFolders { $fields }").getValue("favoriteFolders"))
    }
    suspend fun add(rootPath: String, fullPath: String): List<DFavoriteFolder> = mutate("addFavoriteFolder(rootPath: ${gql(rootPath)}, fullPath: ${gql(fullPath)})","addFavoriteFolder")
    suspend fun remove(fullPath: String): List<DFavoriteFolder> = mutate("removeFavoriteFolder(fullPath: ${gql(fullPath)})","removeFavoriteFolder")
    suspend fun rename(fullPath: String, alias: String): List<DFavoriteFolder> = mutate("setFavoriteFolderAlias(fullPath: ${gql(fullPath)}, alias: ${gql(alias)})","setFavoriteFolderAlias")
    private suspend fun mutate(selection: String, key: String): List<DFavoriteFolder> = lock.withLock {
        apply(RustContentApi.mutate("$selection { $fields }").getValue(key))
    }
    private fun apply(value: JsonElement): List<DFavoriteFolder> = value.jsonArray.map { item ->
        item.jsonObject.let { DFavoriteFolder(it.string("rootPath"),it.string("fullPath"),it["alias"]?.jsonPrimitive?.contentOrNull) }
    }.also { state.value = it }
}
