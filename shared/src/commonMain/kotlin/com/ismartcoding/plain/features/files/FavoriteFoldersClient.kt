package com.ismartcoding.plain.features.files

import com.ismartcoding.plain.graphql.Mutation
import com.ismartcoding.plain.graphql.Query
import com.ismartcoding.plain.models.FavoriteFolder
import com.ismartcoding.plain.rust.graphql.GraphqlRequest
import com.ismartcoding.plain.rust.graphql.GraphqlClient
import com.ismartcoding.plain.rust.graphql.GraphqlApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

object FavoriteFoldersClient : FavoriteFoldersApi()

open class FavoriteFoldersApi internal constructor(
    protected val api: GraphqlClient = GraphqlApi,
) {
    private val lock = Mutex()
    private val state = MutableStateFlow<List<FavoriteFolder>>(emptyList())
    val items = state.asStateFlow()

    suspend fun refresh(): List<FavoriteFolder> = update(Query.favoriteFolders())

    suspend fun add(rootPath: String, fullPath: String): List<FavoriteFolder> =
        update(Mutation.addFavoriteFolder(rootPath, fullPath))

    suspend fun remove(fullPath: String): List<FavoriteFolder> =
        update(Mutation.removeFavoriteFolder(fullPath))

    suspend fun rename(fullPath: String, alias: String): List<FavoriteFolder> =
        update(Mutation.setFavoriteFolderAlias(fullPath, alias))

    private suspend fun update(request: GraphqlRequest<List<FavoriteFolder>>): List<FavoriteFolder> = lock.withLock {
        api.execute(request).also { state.value = it }
    }
}
