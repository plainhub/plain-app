package com.ismartcoding.plain.web

import com.ismartcoding.plain.db.AppDatabase
import com.ismartcoding.plain.db.DSession
import com.ismartcoding.plain.helpers.TimeHelper

object SessionList {
    fun getItemsAsync(): List<DSession> {
        return AppDatabase.instance.sessionDao().getAll()
    }

    fun addOrUpdateAsync(
        clientId: String,
        updateItem: (DSession) -> Unit,
    ) {
        var item = AppDatabase.instance.sessionDao().getByClientId(clientId)
        var isInsert = false
        if (item == null) {
            item = DSession()
            item.clientId = clientId
            isInsert = true
        } else {
            item.updatedAt = TimeHelper.now()
        }

        updateItem(item)

        if (isInsert) {
            AppDatabase.instance.sessionDao().insert(item)
        } else {
            AppDatabase.instance.sessionDao().update(item)
        }
    }

    fun deleteAsync(clientId: String) {
        AppDatabase.instance.sessionDao().delete(clientId)
    }
}
