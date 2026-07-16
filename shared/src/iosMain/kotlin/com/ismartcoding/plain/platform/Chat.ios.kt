package com.ismartcoding.plain.platform

import androidx.compose.ui.focus.FocusManager
import com.ismartcoding.plain.db.DChat
import com.ismartcoding.plain.events.PickFileResultEvent
import com.ismartcoding.plain.ui.models.ChatViewModel
import com.ismartcoding.plain.ui.models.PeerViewModel

/**
 * iOS stub implementation. The chat file-selection flow depends on Android's
 * ContentResolver and is a no-op on iOS.
 */
actual object ChatPlatformOps {
    actual fun handleFileSelection(
        event: PickFileResultEvent,
        chatVM: ChatViewModel,
        peerVM: PeerViewModel,
        focusManager: FocusManager,
    ) {
        // No-op on iOS: file picking flow is Android-only.
    }
}

actual suspend fun updateChatMessageTextAsync(item: DChat, newText: String): Boolean {
    // iOS stub: link-preview reconciliation relies on Android helpers; the text
    // itself should still be persisted via the shared ChatDbHelper if needed.
    return false
}
