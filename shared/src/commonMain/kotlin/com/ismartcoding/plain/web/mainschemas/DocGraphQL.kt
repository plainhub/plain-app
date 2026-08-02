package com.ismartcoding.plain.web.mainschemas

import com.ismartcoding.plain.lib.kgraphql.annotations.GraphQLQuery
import com.ismartcoding.plain.lib.kgraphql.schema.dsl.SchemaBuilder
import com.ismartcoding.plain.docs.DDoc
import com.ismartcoding.plain.enums.DataType
import com.ismartcoding.plain.platform.Permission
import com.ismartcoding.plain.platform.checkEnabledAsync
import com.ismartcoding.plain.platform.enabledAndIsGrantedAsync
import com.ismartcoding.plain.features.file.FileSortBy
import com.ismartcoding.plain.platform.countMedia
import com.ismartcoding.plain.platform.getDocExtGroups
import com.ismartcoding.plain.platform.searchMedia
import com.ismartcoding.plain.web.loaders.TagsLoader
import com.ismartcoding.plain.web.models.Doc
import com.ismartcoding.plain.web.models.DocExtGroup
import com.ismartcoding.plain.web.models.toDocModel

@GraphQLQuery
suspend fun docCount(query: String): Int {
    return if (Permission.WRITE_EXTERNAL_STORAGE.enabledAndIsGrantedAsync()) {
        countMedia(DataType.DOC, query)
    } else {
        0
    }
}

@GraphQLQuery
suspend fun docExtGroups(): List<DocExtGroup> {
    return if (Permission.WRITE_EXTERNAL_STORAGE.enabledAndIsGrantedAsync()) {
        getDocExtGroups("").map { DocExtGroup(it.first, it.second) }
    } else {
        emptyList()
    }
}

@GraphQLQuery
suspend fun docs(offset: Int, limit: Int, query: String, sortBy: FileSortBy): List<Doc> {
    Permission.WRITE_EXTERNAL_STORAGE.checkEnabledAsync()
    return searchMedia(DataType.DOC, query, limit, offset, sortBy)
        .filterIsInstance<DDoc>()
        .map { it.toDocModel() }
}

fun SchemaBuilder.addDocSchema() {
    type<Doc> {
        dataProperty("tags") {
            prepare { item -> item.id.value }
            loader { ids ->
                TagsLoader.load(ids, DataType.DOC)
            }
        }
    }
}
