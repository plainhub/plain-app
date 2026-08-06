package com.ismartcoding.plain.httpserver.mainschemas

import com.ismartcoding.plain.lib.kgraphql.annotations.GraphQLQuery
import com.ismartcoding.plain.lib.kgraphql.schema.dsl.SchemaBuilder
import com.ismartcoding.plain.platform.AppDatabase
import com.ismartcoding.plain.ui.page.appfiles.AppFileDisplayNameHelper
import com.ismartcoding.plain.httpserver.models.AppFile
import com.ismartcoding.plain.httpserver.models.toModel

@GraphQLQuery
suspend fun appFiles(offset: Int, limit: Int): List<AppFile> {
    val fileDao = AppDatabase.instance.appFileDao()
    val chatDao = AppDatabase.instance.chatDao()
    val files = fileDao.getPage(limit, offset)
    val nameMap = AppFileDisplayNameHelper.buildNameMap(chatDao.getAll())
    return files.map { it.toModel(AppFileDisplayNameHelper.resolveDisplayName(it, nameMap)) }
}

@GraphQLQuery
suspend fun appFileCount(): Int {
    return AppDatabase.instance.appFileDao().count()
}

fun SchemaBuilder.addAppFileSchema() {
}
