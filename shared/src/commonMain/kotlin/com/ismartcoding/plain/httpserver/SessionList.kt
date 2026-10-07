package com.ismartcoding.plain.httpserver

import com.ismartcoding.plain.helpers.StringHelper
import com.ismartcoding.plain.lib.withIO
import com.ismartcoding.plain.platform.AppDatabase
import com.ismartcoding.plain.platform.generateChaCha20Key
import com.ismartcoding.plain.db.DSession
import com.ismartcoding.plain.enums.SessionType
import com.ismartcoding.plain.lib.TimeHelper

object SessionList {
    suspend fun getItemsAsync(): List<DSession> = withIO {
        AppDatabase.instance.sessionDao().getAll()
    }

    suspend fun getByClientIdAsync(clientId: String): DSession? = withIO {
        AppDatabase.instance.sessionDao().getByClientId(clientId)
    }

    suspend fun deleteAsync(clientId: String) = withIO {
        AppDatabase.instance.sessionDao().delete(clientId)
    }

    suspend fun createCustomTokenAsync(name: String): DSession = withIO {
        val item = DSession()
        item.clientId = StringHelper.shortUUID()
        item.name = name
        item.type = SessionType.CUSTOM
        item.token = generateChaCha20Key()
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
