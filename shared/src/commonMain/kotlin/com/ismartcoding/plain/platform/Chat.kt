package com.ismartcoding.plain.platform

import androidx.compose.ui.focus.FocusManager
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
