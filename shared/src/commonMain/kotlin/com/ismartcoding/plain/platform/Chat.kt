package com.ismartcoding.plain.platform

import androidx.compose.ui.focus.FocusManager
import com.ismartcoding.plain.db.DChat
import com.ismartcoding.plain.enums.PickFileType
import com.ismartcoding.plain.events.PickFileResultEvent
import com.ismartcoding.plain.features.ChatMessageEditor
import com.ismartcoding.plain.lib.coMain
import com.ismartcoding.plain.chat.ChatManager
import com.ismartcoding.plain.chat.ChatViewModel
import com.ismartcoding.plain.ui.models.PeerViewModel

fun handleChatFileSelection(
    event: PickFileResultEvent,
    chatVM: ChatViewModel,
    peerVM: PeerViewModel,
    focusManager: FocusManager,
) {
    coMain {
        val target = ChatViewModel.target.value
        focusManager.clearFocus()
        ChatManager.sharePicked(target, event.uris.toList(), event.type == PickFileType.IMAGE_VIDEO, event.type == PickFileType.IMAGE_VIDEO)
    }
}

/**
 * Edit a text chat message in-place: persist the new text, reconcile link
 * previews (fetching new ones, deleting obsolete preview images), and emit a
 * `MESSAGE_UPDATED` event so peers and UI stay in sync.
 *
 * Returns true if a change was persisted.
 */
suspend fun updateChatMessageTextAsync(item: DChat, newText: String): Boolean {
    return ChatMessageEditor.updateTextAsync(item, newText)
}
