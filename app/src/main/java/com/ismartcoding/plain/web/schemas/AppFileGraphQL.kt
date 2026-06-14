package com.ismartcoding.plain.web.schemas

import com.ismartcoding.lib.kgraphql.schema.dsl.SchemaBuilder
import com.ismartcoding.plain.db.AppDatabase
import com.ismartcoding.plain.ui.page.appfiles.AppFileDisplayNameHelper
import com.ismartcoding.plain.web.models.toModel

fun SchemaBuilder.addAppFileSchema() {
    query("appFiles") {
        resolver { offset: Int, limit: Int ->
            val dao = AppDatabase.instance.appFileDao()
            val chatDao = AppDatabase.instance.chatDao()
            val files = dao.getPage(limit, offset)
            val nameMap = AppFileDisplayNameHelper.buildNameMap(chatDao.getAll())
            files.map { it.toModel(AppFileDisplayNameHelper.resolveDisplayName(it, nameMap)) }
        }
    }
    query("appFileCount") {
        resolver { ->
            AppDatabase.instance.appFileDao().count()
        }
    }
}
