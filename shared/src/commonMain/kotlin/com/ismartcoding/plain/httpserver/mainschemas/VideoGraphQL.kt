package com.ismartcoding.plain.httpserver.mainschemas

import com.ismartcoding.plain.lib.kgraphql.annotations.GraphQLQuery
import com.ismartcoding.plain.lib.kgraphql.schema.dsl.SchemaBuilder
import com.ismartcoding.plain.data.DVideo
import com.ismartcoding.plain.enums.DataType
import com.ismartcoding.plain.platform.Permission
import com.ismartcoding.plain.platform.checkEnabledAsync
import com.ismartcoding.plain.platform.enabledAndIsGrantedAsync
import com.ismartcoding.plain.features.file.FileSortBy
import com.ismartcoding.plain.platform.countMedia
import com.ismartcoding.plain.platform.searchMedia
import com.ismartcoding.plain.httpserver.loaders.TagsLoader
import com.ismartcoding.plain.httpserver.models.Video
import com.ismartcoding.plain.httpserver.models.toModel

@GraphQLQuery
suspend fun videoCount(query: String): Int {
    return if (Permission.WRITE_EXTERNAL_STORAGE.enabledAndIsGrantedAsync()) {
        countMedia(DataType.VIDEO, query)
    } else {
        0
    }
}

@GraphQLQuery
suspend fun videos(offset: Int, limit: Int, query: String, sortBy: FileSortBy): List<Video> {
    Permission.WRITE_EXTERNAL_STORAGE.checkEnabledAsync()
    return searchMedia(DataType.VIDEO, query, limit, offset, sortBy)
        .filterIsInstance<DVideo>()
        .map { it.toModel() }
}

fun SchemaBuilder.addVideoSchema() {
    type<Video> {
        dataProperty("tags") {
            prepare { item -> item.id.value }
            loader { ids ->
                TagsLoader.load(ids, DataType.VIDEO)
            }
        }
    }
}
