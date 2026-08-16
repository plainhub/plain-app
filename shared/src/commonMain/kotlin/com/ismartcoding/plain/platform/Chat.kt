package com.ismartcoding.plain.platform

import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.unit.IntSize
import com.ismartcoding.plain.chat.peer.PeerCacher
import com.ismartcoding.plain.db.DChat
import com.ismartcoding.plain.db.DMessageFile
import com.ismartcoding.plain.enums.PickFileType
import com.ismartcoding.plain.events.PickFileResultEvent
import com.ismartcoding.plain.features.ChatMessageEditor
import com.ismartcoding.plain.helpers.StringHelper
import com.ismartcoding.plain.lib.coMain
import com.ismartcoding.plain.lib.withIO
import com.ismartcoding.plain.lib.extensions.getFilenameWithoutExtension
import com.ismartcoding.plain.extensions.getFinalPath
import com.ismartcoding.plain.lib.extensions.isImageFast
import com.ismartcoding.plain.lib.extensions.isVideoFast
import com.ismartcoding.plain.ui.helpers.DialogHelper
import com.ismartcoding.plain.ui.models.ChatViewModel
import com.ismartcoding.plain.ui.models.PeerViewModel
import kotlinx.coroutines.delay

/**
 * Handle a file-pick result: create placeholder chat messages, import the
 * selected files into the content-addressable store, and update the messages
 * with final metadata (size, intrinsic dimensions, duration).
 *
 * Platform-specific steps (URI query, file import, media metadata) go through
 * the low-level expects [queryPickedFileInfo], [importChatFile],
 * [getImageIntrinsicSize], [getImageRotation], [getVideoIntrinsicSize],
 * [getMediaDuration]. On iOS these return null/zero (no document picker),
 * so the flow is effectively a no-op until the picker ships.
 */
fun handleChatFileSelection(
    event: PickFileResultEvent,
    chatVM: ChatViewModel,
    peerVM: PeerViewModel,
    focusManager: FocusManager,
) {
    coMain {
        val placeholders = mutableListOf<Pair<DMessageFile, PickedFileInfo>>()
        event.uris.forEach { uriStr ->
            val info = queryPickedFileInfo(uriStr) ?: return@forEach
            var fileName = info.displayName
            if (event.type == PickFileType.IMAGE_VIDEO) {
                val extension = getExtensionFromMimeType(info.mimeType)
                if (extension.isNotEmpty()) {
                    fileName = fileName.getFilenameWithoutExtension() + "." + extension
                }
            }
            placeholders.add(
                DMessageFile(
                    id = StringHelper.shortUUID(),
                    uri = uriStr,
                    size = info.size,
                    fileName = fileName,
                ) to info,
            )
        }
        if (placeholders.isEmpty()) return@coMain

        val placeholderItems = placeholders.map { it.first }
        val isImageVideo = event.type == PickFileType.IMAGE_VIDEO
        val messageId = chatVM.sendFilesImmediate(placeholderItems, isImageVideo)
        delay(200)
        focusManager.clearFocus()

        withIO {
            val finalItems = mutableListOf<DMessageFile>()
            placeholders.forEach { (placeholder, info) ->
                try {
                    val fidUri = importChatFile(placeholder.uri, info.mimeType)
                        ?: run { finalItems.add(placeholder); return@forEach }
                    val realPath = fidUri.getFinalPath()
                    val intrinsicSize = if (placeholder.fileName.isImageFast()) {
                        getImageIntrinsicSize(realPath, getImageRotation(realPath))
                    } else if (placeholder.fileName.isVideoFast()) {
                        getVideoIntrinsicSize(realPath)
                    } else {
                        IntSize.Zero
                    }
                    finalItems.add(
                        DMessageFile(
                            id = placeholder.id,
                            uri = fidUri,
                            size = placeholder.size,
                            duration = getMediaDuration(realPath),
                            width = intrinsicSize.width,
                            height = intrinsicSize.height,
                            summary = placeholder.summary,
                            fileName = placeholder.fileName,
                        ),
                    )
                } catch (ex: Exception) {
                    DialogHelper.showMessage(ex)
                    finalItems.add(placeholder)
                }
            }
            chatVM.updateFilesMessage(messageId, finalItems, isImageVideo, PeerCacher.getOnlinePeerIds())
        }
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
