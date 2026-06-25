package com.ismartcoding.plain.web.schemas

import com.ismartcoding.plain.lib.kgraphql.schema.dsl.SchemaBuilder
import com.ismartcoding.plain.lib.kgraphql.schema.execution.Executor
import com.ismartcoding.plain.MainApp
import com.ismartcoding.plain.enums.DataType
import com.ismartcoding.plain.features.Permission
import com.ismartcoding.plain.features.file.FileSortBy
import com.ismartcoding.plain.docs.DocMediaStoreHelper
import com.ismartcoding.plain.web.loaders.TagsLoader
import com.ismartcoding.plain.web.models.Doc
import com.ismartcoding.plain.web.models.DocExtGroup
import com.ismartcoding.plain.web.models.toDocModel

fun SchemaBuilder.addDocSchema() {
    query("docs") {
        configure {
            executor = Executor.DataLoaderPrepared
        }
        resolver { offset: Int, limit: Int, query: String, sortBy: FileSortBy ->
            val context = MainApp.instance
            Permission.WRITE_EXTERNAL_STORAGE.checkAsync(context)
            DocMediaStoreHelper.searchAsync(context, query, limit, offset, sortBy).map { it.toDocModel() }
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
        resolver { query: String ->
            if (Permission.WRITE_EXTERNAL_STORAGE.enabledAndCanAsync(MainApp.instance)) {
                DocMediaStoreHelper.countAsync(MainApp.instance, query)
            } else {
                0
            }
        }
    }

    query("docExtGroups") {
        resolver { ->
            if (Permission.WRITE_EXTERNAL_STORAGE.enabledAndCanAsync(MainApp.instance)) {
                DocMediaStoreHelper.getDocExtGroupsAsync(MainApp.instance).map { DocExtGroup(it.first, it.second) }
            } else {
                emptyList()
            }
        }
    }
}

