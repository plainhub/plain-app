package com.ismartcoding.plain.features.files

import com.ismartcoding.plain.graphql.Mutation
import com.ismartcoding.plain.rust.graphql.GraphqlApi

object RemoteFiles {
    suspend fun cache(url: String, name: String = ""): String =
        GraphqlApi.execute(Mutation.cacheRemoteFile(url, name))
}
