package com.ismartcoding.plain.web

import com.ismartcoding.lib.helpers.CryptoHelper
import com.ismartcoding.lib.helpers.StringHelper
import com.ismartcoding.plain.db.AppDatabase
import com.ismartcoding.plain.db.DSession
import com.ismartcoding.plain.helpers.TimeHelper

object SessionList {
    fun getItemsAsync(): List<DSession> {
        return AppDatabase.instance.sessionDao().getAll()
    }

    fun getByClientIdAsync(clientId: String): DSession? {
        return AppDatabase.instance.sessionDao().getByClientId(clientId)
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
            item.type = DSession.TYPE_WEB
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

    fun createCustomTokenAsync(name: String): DSession {
        val item = DSession()
        item.clientId = StringHelper.shortUUID()
        item.name = name
        item.type = DSession.TYPE_CUSTOM
        item.token = CryptoHelper.generateChaCha20Key()
        AppDatabase.instance.sessionDao().insert(item)
        return item
    }

    fun renameAsync(clientId: String, name: String): Boolean {
        val item = AppDatabase.instance.sessionDao().getByClientId(clientId) ?: return false
        item.name = name
        item.updatedAt = TimeHelper.now()
        AppDatabase.instance.sessionDao().update(item)
        return true
    }
}
