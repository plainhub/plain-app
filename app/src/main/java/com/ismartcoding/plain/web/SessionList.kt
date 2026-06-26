package com.ismartcoding.plain.web

import com.ismartcoding.plain.lib.helpers.CryptoHelper
import com.ismartcoding.plain.helpers.StringHelper
import com.ismartcoding.plain.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.plain.db.AppDatabase
import com.ismartcoding.plain.db.DSession
import com.ismartcoding.plain.helpers.TimeHelper

object SessionList {
    suspend fun getItemsAsync(): List<DSession> = withIO {
        AppDatabase.instance.sessionDao().getAll()
    }

    suspend fun getByClientIdAsync(clientId: String): DSession? = withIO {
        AppDatabase.instance.sessionDao().getByClientId(clientId)
    }

    suspend fun addOrUpdateAsync(
        clientId: String,
        updateItem: (DSession) -> Unit,
    ) = withIO {
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

    suspend fun deleteAsync(clientId: String) = withIO {
        AppDatabase.instance.sessionDao().delete(clientId)
    }

    suspend fun createCustomTokenAsync(name: String): DSession = withIO {
        val item = DSession()
        item.clientId = StringHelper.shortUUID()
        item.name = name
        item.type = DSession.TYPE_CUSTOM
        item.token = CryptoHelper.generateChaCha20Key()
        AppDatabase.instance.sessionDao().insert(item)
        item
    }

    suspend fun renameAsync(clientId: String, name: String): Boolean = withIO {
        val item = AppDatabase.instance.sessionDao().getByClientId(clientId) ?: return@withIO false
        item.name = name
        item.updatedAt = TimeHelper.now()
        AppDatabase.instance.sessionDao().update(item)
        true
    }
}
