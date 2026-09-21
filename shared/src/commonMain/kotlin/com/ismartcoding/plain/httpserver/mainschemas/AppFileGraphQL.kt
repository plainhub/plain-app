package com.ismartcoding.plain.httpserver.mainschemas

import com.ismartcoding.plain.lib.kgraphql.annotations.GraphQLQuery
import com.ismartcoding.plain.lib.kgraphql.schema.dsl.SchemaBuilder
import com.ismartcoding.plain.helpers.QueryHelper
import com.ismartcoding.plain.platform.AppDatabase
import com.ismartcoding.plain.ui.page.appfiles.AppFileDisplayNameHelper
import com.ismartcoding.plain.httpserver.models.AppFile
import com.ismartcoding.plain.httpserver.models.toModel

@GraphQLQuery
suspend fun appFiles(offset: Int, limit: Int, query: String): List<AppFile> {
    val fileDao = AppDatabase.instance.appFileDao()
    val chatDao = AppDatabase.instance.chatDao()
    val text = QueryHelper.textOf(query).trim()
    val files = if (text.isEmpty()) fileDao.getPage(limit, offset) else fileDao.getPageText("%$text%", limit, offset)
    val nameMap = AppFileDisplayNameHelper.buildNameMap(chatDao.getAll())
    return files.map { it.toModel(AppFileDisplayNameHelper.resolveDisplayName(it, nameMap)) }
}

@GraphQLQuery
suspend fun appFileCount(query: String): Int {
    val text = QueryHelper.textOf(query).trim()
    val dao = AppDatabase.instance.appFileDao()
    return if (text.isEmpty()) dao.count() else dao.countText("%$text%")
}

fun SchemaBuilder.addAppFileSchema() {
}
