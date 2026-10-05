package com.ismartcoding.plain.chat

import com.ismartcoding.plain.chat.data.ChatTarget
import com.ismartcoding.plain.db.DMessageContent
import com.ismartcoding.plain.db.DShare
import kotlin.time.Instant

object ShareSendHelper {
    suspend fun sendAsync(targets: List<ChatTarget>, uris: List<String>, text: String?, caption: String?): Boolean =
        RustChatService.share(targets, uris, text, caption)

    suspend fun sendContentAsync(targets: List<ChatTarget>, content: DMessageContent) {
        RustChatService.sendMany(targets, content)
    }

    suspend fun sendFolderShareAsync(targets: List<ChatTarget>, dirPath: String, name: String, expiresAt: Instant?): Boolean =
        RustChatService.folder(targets, dirPath, name, expiresAt?.toString())

    suspend fun buildShareContent(share: DShare, paths: List<String>): DMessageContent =
        RustChatService.shareContent(share.id)
}
