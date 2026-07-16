package com.ismartcoding.plain.web.schemas

import com.ismartcoding.plain.lib.kgraphql.schema.dsl.SchemaBuilder
import com.ismartcoding.plain.lib.kgraphql.schema.execution.Executor
import com.ismartcoding.plain.docs.DDoc
import com.ismartcoding.plain.enums.DataType
import com.ismartcoding.plain.platform.Permission
import com.ismartcoding.plain.platform.checkEnabledAsync
import com.ismartcoding.plain.features.checkEnabledAsync
import com.ismartcoding.plain.platform.enabledAndIsGrantedAsync
import com.ismartcoding.plain.features.file.FileSortBy
import com.ismartcoding.plain.platform.countMedia
import com.ismartcoding.plain.platform.getDocExtGroups
import com.ismartcoding.plain.platform.searchMedia
import com.ismartcoding.plain.web.loaders.TagsLoader
import com.ismartcoding.plain.web.models.Doc
import com.ismartcoding.plain.web.models.DocExtGroup
import com.ismartcoding.plain.web.models.toDocModel

fun SchemaBuilder.addDocSchema() {
    query("docs") {
        configure {
            executor = Executor.DataLoaderPrepared
        }
        resolver("offset", "limit", "query", "sortBy") { offset: Int, limit: Int, query: String, sortBy: FileSortBy ->
            Permission.WRITE_EXTERNAL_STORAGE.checkEnabledAsync()
            searchMedia(DataType.DOC, query, limit, offset, sortBy)
                .filterIsInstance<DDoc>()
                .map { it.toDocModel() }
        }
        type<Doc> {
            dataProperty("tags") {
                prepare { item -> item.id.value }
                loader { ids ->
                    TagsLoader.load(ids, DataType.DOC)
                }
            }
        }
    }

    query("docCount") {
        resolver("query") { query: String ->
            if (Permission.WRITE_EXTERNAL_STORAGE.enabledAndIsGrantedAsync()) {
                countMedia(DataType.DOC, query)
            } else {
                0
            }
        }
    }

    query("docExtGroups") {
        resolver { ->
            if (Permission.WRITE_EXTERNAL_STORAGE.enabledAndIsGrantedAsync()) {
                getDocExtGroups("").map { DocExtGroup(it.first, it.second) }
            } else {
                emptyList()
            }
        }
    }
}
