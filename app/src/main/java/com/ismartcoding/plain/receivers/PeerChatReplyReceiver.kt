package com.ismartcoding.plain.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.RemoteInput
import com.ismartcoding.lib.helpers.CoroutinesHelper.coIO
import com.ismartcoding.lib.logcat.LogCat
import com.ismartcoding.plain.chat.PeerChatHelper
import com.ismartcoding.plain.db.AppDatabase
import com.ismartcoding.plain.db.DMessageContent
import com.ismartcoding.plain.db.DMessageText
import com.ismartcoding.plain.db.DMessageType
import com.ismartcoding.plain.chat.ChatDbHelper

class PeerChatReplyReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val remoteInput = RemoteInput.getResultsFromIntent(intent)
        val replyText = remoteInput?.getCharSequence(KEY_TEXT_REPLY)?.toString()?.trim() ?: return
        if (replyText.isEmpty()) return

        val peerId = intent.getStringExtra(EXTRA_PEER_ID) ?: return
        // Notification ID must match the one used when posting - dismiss it to
        // clear the "reply loading" spinner on all OEMs (especially MIUI).
        val notificationId = ("peer_chat_$peerId").hashCode()

        coIO {
            val peer = AppDatabase.instance.peerDao().getById(peerId)
            if (peer == null) {
                LogCat.e("PeerChatReplyReceiver: peer not found for id=$peerId")
                NotificationManagerCompat.from(context).cancel(notificationId)
                return@coIO
            }
            val content = DMessageContent(DMessageType.TEXT.value, DMessageText(replyText))
            val item = ChatDbHelper.sendAsync(content, fromId = "me", toId = peerId, peer = peer)
            val success = PeerChatHelper.sendToPeerAsync(peer, item.content)
            val finalStatus = if (success) "sent" else "failed"
            ChatDbHelper.updateStatusAsync(item.id, finalStatus)
            if (success) {
                LogCat.d("PeerChatReplyReceiver: reply sent to peer $peerId")
            } else {
                LogCat.e("PeerChatReplyReceiver: failed to send reply to peer $peerId")
            }
            // Always cancel the notification so the reply spinner clears on MIUI and other OEMs.
            NotificationManagerCompat.from(context).cancel(notificationId)
        }
    }

    companion object {
        const val KEY_TEXT_REPLY = "key_text_reply"
        const val EXTRA_PEER_ID = "extra_peer_id"
    }
}
