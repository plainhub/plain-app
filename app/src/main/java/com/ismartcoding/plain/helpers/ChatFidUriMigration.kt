package com.ismartcoding.plain.helpers

import android.content.Context
import com.ismartcoding.lib.extensions.appDir
import com.ismartcoding.lib.logcat.LogCat
import com.ismartcoding.plain.db.AppDatabase
import com.ismartcoding.plain.db.ChatItemDataUpdate
import com.ismartcoding.plain.db.DMessageFiles
import com.ismartcoding.plain.db.DMessageImages
import com.ismartcoding.plain.db.DMessageType
import java.io.File

/**
 * One-time migration: add lowercase file extension to all legacy fid: URIs
 * stored as `fid:{hash}` (without extension).
 *
 * For each DChat with images/files content, finds items with bare fid:{hash}
 * URIs, derives the extension from the DAppFile mimeType, renames the file on
 * disk, updates DAppFile.realPath, and rewrites the DChat content.
 *
 * Guarded by [FidUriExtMigratedPreference] so it only runs once.
 */
object ChatFidUriMigration {

    fun run(context: Context) {
        val chatDao = AppDatabase.instance.chatDao()
        val fileDao = AppDatabase.instance.appFileDao()
        val renamedHashes = mutableSetOf<String>()

        val chats = chatDao.getAll()
        for (chat in chats) {
            val type = chat.content.type
            if (type != DMessageType.IMAGES.value && type != DMessageType.FILES.value) continue

            var changed = false

            when (type) {
                DMessageType.IMAGES.value -> {
                    val imgs = chat.content.value as? DMessageImages ?: continue
                    val newItems = imgs.items.map { item ->
                        if (item.uri.startsWith("fid:") && !item.uri.removePrefix("fid:").contains(".")) {
                            val hash = item.uri.removePrefix("fid:")
                            val dFile = fileDao.getById(hash) ?: return@map item
                            val ext = AppFileStore.extFromMime(dFile.mimeType)
                            if (ext.isEmpty()) return@map item
                            if (hash !in renamedHashes) {
                                renameLegacyFile(context, hash, ext, fileDao, dFile)
                                renamedHashes += hash
                            }
                            changed = true
                            item.copy(uri = "fid:$hash.$ext")
                        } else {
                            item
                        }
                    }
                    if (changed) {
                        chat.content.value = DMessageImages(newItems)
                    }
                }

                DMessageType.FILES.value -> {
                    val files = chat.content.value as? DMessageFiles ?: continue
                    val newItems = files.items.map { item ->
                        if (item.uri.startsWith("fid:") && !item.uri.removePrefix("fid:").contains(".")) {
                            val hash = item.uri.removePrefix("fid:")
                            val dFile = fileDao.getById(hash) ?: return@map item
                            val ext = AppFileStore.extFromMime(dFile.mimeType)
                            if (ext.isEmpty()) return@map item
                            if (hash !in renamedHashes) {
                                renameLegacyFile(context, hash, ext, fileDao, dFile)
                                renamedHashes += hash
                            }
                            changed = true
                            item.copy(uri = "fid:$hash.$ext")
                        } else {
                            item
                        }
                    }
                    if (changed) {
                        chat.content.value = DMessageFiles(newItems)
                    }
                }
            }

            if (changed) {
                chatDao.updateData(ChatItemDataUpdate(id = chat.id, content = chat.content))
                LogCat.d("ChatFidUriMigration: updated chat ${chat.id}")
            }
        }

        LogCat.d("ChatFidUriMigration: done, processed ${chats.size} chats")
    }

    private fun renameLegacyFile(
        context: Context,
        hash: String,
        ext: String,
        fileDao: com.ismartcoding.plain.db.AppFileDao,
        dFile: com.ismartcoding.plain.db.DAppFile,
    ) {
        val base = context.appDir()
        val legacyPath = "$base/${hash.substring(0, 2)}/${hash.substring(2, 4)}/$hash"
        val newPath = "$legacyPath.$ext"
        val legacyFile = File(legacyPath)
        val newFile = File(newPath)
        if (legacyFile.exists() && !newFile.exists()) {
            legacyFile.renameTo(newFile)
        }
        if (dFile.realPath != newPath) {
            dFile.realPath = newPath
            fileDao.update(dFile)
        }
    }
}
