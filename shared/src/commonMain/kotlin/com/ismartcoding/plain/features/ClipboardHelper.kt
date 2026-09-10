package com.ismartcoding.plain.features

import com.ismartcoding.plain.db.DClipboard
import com.ismartcoding.plain.lib.withIO
import com.ismartcoding.plain.platform.AppDatabase

object ClipboardHelper {
    private val dao by lazy {
        AppDatabase.instance.clipboardDao()
    }

    suspend fun getPage(limit: Int, offset: Int, query: String = ""): List<DClipboard> = withIO {
        dao.getPage(limit, offset, likePattern(query))
    }

    suspend fun count(query: String = ""): Int = withIO {
        dao.count(likePattern(query))
    }

    private fun likePattern(query: String): String =
        "%" + query.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%"

    suspend fun getLatestByHash(hash: String): DClipboard? = withIO {
        dao.getLatestByHash(hash)
    }

    suspend fun insert(entry: DClipboard) = withIO {
        dao.insert(entry)
    }

    suspend fun deleteByIds(ids: List<String>) = withIO {
        dao.deleteByIds(ids)
    }

    suspend fun clear() = withIO {
        dao.clear()
    }

    /** Resolves a source client id to the peer name; empty source = this device. */
    suspend fun getSourceName(source: String): String = withIO {
        if (source.isBlank()) "" else AppDatabase.instance.peerDao().getById(source)?.name ?: source
    }
}
