package com.ismartcoding.plain.platform

import androidx.compose.ui.focus.FocusManager
import com.ismartcoding.plain.db.DChat
import com.ismartcoding.plain.events.PickFileResultEvent
import com.ismartcoding.plain.ui.models.ChatViewModel
import com.ismartcoding.plain.ui.models.PeerViewModel

/**
 * Chat-specific platform abstraction for the file-selection send-message flow.
 *
 * General-purpose platform operations (BLE, file ops, audio player, media
 * metadata, network) live in `com.ismartcoding.plain.platform` as top-level
 * expect/actual functions (e.g. [com.ismartcoding.plain.platform.isBleReady],
 * [com.ismartcoding.plain.platform.saveFileToDownloads]).
 */
expect object ChatPlatformOps {
    /**
     * Handle a file-pick result: create placeholder chat messages, import the
     * selected files, and update the messages with final metadata.
     */
    fun handleFileSelection(
        event: PickFileResultEvent,
        chatVM: ChatViewModel,
        peerVM: PeerViewModel,
        focusManager: FocusManager,
    )
}

/**
 * Edit a text chat message in-place: persist the new text, reconcile link
 * previews (fetching new ones, deleting obsolete preview images), and emit a
 * `MESSAGE_UPDATED` event so peers and UI stay in sync.
 *
 * Equivalent to the Android-only `ChatMessageEditor.updateTextAsync(context, item, newText)`
 * but uses the platform's app context internally so it can be called from
 * commonMain. Returns true if a change was persisted.
 */
expect suspend fun updateChatMessageTextAsync(item: DChat, newText: String): Boolean
