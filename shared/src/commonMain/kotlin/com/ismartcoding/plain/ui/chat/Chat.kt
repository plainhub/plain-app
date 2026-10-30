package com.ismartcoding.plain.ui.chat

import com.ismartcoding.plain.features.chat.sendPickedAttachments

import androidx.compose.ui.focus.FocusManager
import com.ismartcoding.plain.features.files.PickFileType
import com.ismartcoding.plain.events.PickFileResultEvent
import com.ismartcoding.plain.features.chat.ChatMessageEditor
import com.ismartcoding.plain.models.ChatItem
import com.ismartcoding.plain.lib.coMain

fun handleChatFileSelection(
    event: PickFileResultEvent,
    chatVM: ChatViewModel,
    peerVM: PeerViewModel,
    focusManager: FocusManager,
) {
    coMain {
        val target = chatVM.target.value
        focusManager.clearFocus()
        sendPickedAttachments(listOf(target), event.uris.toList(),
            images = event.type == PickFileType.IMAGE_VIDEO,
            normalize = event.type == PickFileType.IMAGE_VIDEO, chatVM = chatVM)
    }
}

/**
 * Edit a text chat message in-place: persist the new text, reconcile link
 * previews (fetching new ones, deleting obsolete preview images), and emit a
 * `MESSAGE_UPDATED` event so peers and UI stay in sync.
 *
 * Returns true if a change was persisted.
 */
suspend fun updateChatMessageTextAsync(item: ChatItem, newText: String): Boolean {
    return ChatMessageEditor.updateTextAsync(item, newText)
}
