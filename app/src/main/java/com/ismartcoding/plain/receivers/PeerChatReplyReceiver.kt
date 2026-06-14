package com.ismartcoding.plain.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.RemoteInput
import com.ismartcoding.lib.helpers.CoroutinesHelper.coIO
import com.ismartcoding.plain.chat.ChatSender
import com.ismartcoding.plain.chat.data.ChatTarget
import com.ismartcoding.plain.db.DMessageContent
import com.ismartcoding.plain.db.DMessageText
import com.ismartcoding.plain.db.DMessageType

class PeerChatReplyReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val remoteInput = RemoteInput.getResultsFromIntent(intent)
        val replyText = remoteInput?.getCharSequence(KEY_TEXT_REPLY)?.toString()?.trim() ?: return
        if (replyText.isEmpty()) return

        val targetId = intent.getStringExtra(EXTRA_TARGET_ID) ?: return
        val notificationId = ("chat_$targetId").hashCode()
        val target = ChatTarget.parseId(targetId)

        coIO {
            val content = DMessageContent(DMessageType.TEXT.value, DMessageText(replyText))
            val item = ChatSender.createChatItem(target, content)
            ChatSender.send(item, target, emptySet())
            NotificationManagerCompat.from(context).cancel(notificationId)
        }
    }

    companion object {
        const val KEY_TEXT_REPLY = "key_text_reply"
        const val EXTRA_TARGET_ID = "extra_target_id"
    }
}
