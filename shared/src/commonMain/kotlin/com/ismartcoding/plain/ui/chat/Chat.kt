package com.ismartcoding.plain.ui.chat

import com.ismartcoding.plain.features.chat.ChatMessageEditor
import com.ismartcoding.plain.models.ChatItem

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
