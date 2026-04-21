package com.ismartcoding.plain.web.schemas

import com.ismartcoding.lib.kgraphql.schema.dsl.SchemaBuilder
import com.ismartcoding.lib.kgraphql.schema.execution.Executor
import com.ismartcoding.plain.MainApp
import com.ismartcoding.plain.features.Permission
import com.ismartcoding.plain.features.file.FileSortBy
import com.ismartcoding.plain.features.media.DocsHelper
import com.ismartcoding.plain.web.models.DocExtGroup
import com.ismartcoding.plain.web.models.toDocModel

fun SchemaBuilder.addDocQueries() {
    query("docs") {
        configure {
            executor = Executor.DataLoaderPrepared
        }
        resolver { offset: Int, limit: Int, query: String, sortBy: FileSortBy ->
            val context = MainApp.instance
            Permission.WRITE_EXTERNAL_STORAGE.checkAsync(context)
            DocsHelper.searchAsync(context, query, limit, offset, sortBy).map { it.toDocModel() }
        }
    }

    query("docCount") {
        resolver { query: String ->
            if (Permission.WRITE_EXTERNAL_STORAGE.enabledAndCanAsync(MainApp.instance)) {
                DocsHelper.countAsync(MainApp.instance, query)
            } else {
                0
            }
        }
    }

    query("docExtGroups") {
        resolver { ->
            if (Permission.WRITE_EXTERNAL_STORAGE.enabledAndCanAsync(MainApp.instance)) {
                DocsHelper.getDocExtGroupsAsync(MainApp.instance).map { DocExtGroup(it.first, it.second) }
            } else {
                emptyList()
            }
        }
    }
}

