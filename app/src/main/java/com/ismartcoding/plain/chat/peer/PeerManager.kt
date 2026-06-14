package com.ismartcoding.plain.chat.peer

import android.content.Context
import com.ismartcoding.plain.chat.ChatCacheManager
import com.ismartcoding.plain.chat.ChatDbHelper
import com.ismartcoding.plain.db.AppDatabase

object PeerManager {
    suspend fun deletePeer(context: Context, peerId: String): Boolean {
        val peerDao = AppDatabase.instance.peerDao()
        if (peerDao.getById(peerId) == null) return false

        ChatDbHelper.deleteAllChatsAsync(context, peerId)
        val isChannelMember = AppDatabase.instance.chatChannelDao().getAll().any { it.hasMember(peerId) }
        if (isChannelMember) {
            val peer = peerDao.getById(peerId)!!
            peer.key = ""
            peer.status = "channel"
            peerDao.update(peer)
        } else {
            peerDao.delete(peerId)
        }
        ChatCacheManager.loadKeyCacheAsync()
        return true
    }
}
