package com.ismartcoding.plain.web.schemas

import com.ismartcoding.plain.lib.kgraphql.schema.dsl.SchemaBuilder
import com.ismartcoding.plain.lib.kgraphql.schema.execution.Executor
import com.ismartcoding.plain.data.DVideo
import com.ismartcoding.plain.enums.DataType
import com.ismartcoding.plain.platform.Permission
import com.ismartcoding.plain.platform.checkEnabledAsync
import com.ismartcoding.plain.features.checkEnabledAsync
import com.ismartcoding.plain.platform.enabledAndIsGrantedAsync
import com.ismartcoding.plain.features.file.FileSortBy
import com.ismartcoding.plain.platform.countMedia
import com.ismartcoding.plain.platform.searchMedia
import com.ismartcoding.plain.web.loaders.TagsLoader
import com.ismartcoding.plain.web.models.Video
import com.ismartcoding.plain.web.models.toModel

fun SchemaBuilder.addVideoSchema() {
    query("videos") {
        configure {
            executor = Executor.DataLoaderPrepared
        }
        resolver("offset", "limit", "query", "sortBy") { offset: Int, limit: Int, query: String, sortBy: FileSortBy ->
            Permission.WRITE_EXTERNAL_STORAGE.checkEnabledAsync()
            searchMedia(DataType.VIDEO, query, limit, offset, sortBy)
                .filterIsInstance<DVideo>()
                .map { it.toModel() }
        }
        type<Video> {
            dataProperty("tags") {
                prepare { item -> item.id.value }
                loader { ids ->
                    TagsLoader.load(ids, DataType.VIDEO)
                }
            }
        }
    }
    query("videoCount") {
        resolver("query") { query: String ->
            if (Permission.WRITE_EXTERNAL_STORAGE.enabledAndIsGrantedAsync()) {
                countMedia(DataType.VIDEO, query)
            } else {
                0
            }
        }
    }
}
