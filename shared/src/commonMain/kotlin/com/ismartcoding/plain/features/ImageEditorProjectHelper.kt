package com.ismartcoding.plain.features

import com.ismartcoding.plain.db.DImageEditorProject
import com.ismartcoding.plain.events.EventType
import com.ismartcoding.plain.events.WebSocketEvent
import com.ismartcoding.plain.helpers.Base64Lenient
import com.ismartcoding.plain.helpers.TimeHelper
import com.ismartcoding.plain.lib.withIO
import com.ismartcoding.plain.lib.sendEvent
import com.ismartcoding.plain.platform.AppDatabase

object ImageEditorProjectHelper {
    private val dao by lazy { AppDatabase.instance.imageEditorProjectDao() }

    suspend fun listAsync(limit: Int): List<DImageEditorProject> = withIO {
        dao.list(limit)
    }

    suspend fun getByIdAsync(id: String): DImageEditorProject? = withIO {
        dao.getById(id)
    }

    suspend fun addOrUpdateAsync(
        id: String,
        updateItem: DImageEditorProject.() -> Unit,
    ): DImageEditorProject? = withIO {
        val existing = if (id.isNotEmpty()) dao.getById(id) else null
        val project = existing ?: DImageEditorProject(id)
        project.apply {
            updateItem()
            updatedAt = TimeHelper.now()
        }
        dao.upsert(project)
        dao.getById(project.id)
    }

    suspend fun deleteAsync(id: String) = withIO {
        dao.delete(id)
    }

    fun broadcastUpdate(pid: String, updateB64: String) {
        val updateBytes = Base64Lenient.decode(updateB64)
        val pidBytes = pid.encodeToByteArray()
        val frame = ByteArray(1 + pidBytes.size + updateBytes.size)
        frame[0] = pidBytes.size.toByte()
        pidBytes.copyInto(frame, 1)
        updateBytes.copyInto(frame, 1 + pidBytes.size)
        sendEvent(WebSocketEvent(EventType.IMAGE_EDITOR_UPDATE, frame))
    }
}
