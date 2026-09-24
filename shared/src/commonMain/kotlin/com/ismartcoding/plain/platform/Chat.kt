package com.ismartcoding.plain.platform

import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.unit.IntSize
import com.ismartcoding.plain.Constants
import com.ismartcoding.plain.chat.peer.PeerCacher
import com.ismartcoding.plain.db.DChat
import com.ismartcoding.plain.db.DMessageContent
import com.ismartcoding.plain.db.DMessageFile
import com.ismartcoding.plain.db.DMessageText
import com.ismartcoding.plain.db.MessageType
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
import com.ismartcoding.plain.ui.helpers.DialogHelper.showMessage
import com.ismartcoding.plain.chat.ChatViewModel
import com.ismartcoding.plain.ui.models.PeerViewModel
import kotlinx.coroutines.delay

/**
 * Message content for a text of any length: plain text below
 * [Constants.MAX_MESSAGE_LENGTH], a file-backed long-text message above it.
 */
fun textMessageContent(text: String): DMessageContent =
    if (text.length > Constants.MAX_MESSAGE_LENGTH) {
        createLongTextFile(text)
    } else {
        DMessageContent(MessageType.TEXT, DMessageText(text))
    }

/**
 * Query display info for each picked/shared URI and build placeholder message
 * files (final `fid:` URIs are filled in later by [importPickedFiles]).
 * When [normalizeExtension] is set, the display name's extension is replaced
 * by the one derived from the MIME type (gallery pickers often lose it).
 */
fun buildPickedFilePlaceholders(uris: Set<String>, normalizeExtension: Boolean): List<Pair<DMessageFile, PickedFileInfo>> {
    val placeholders = mutableListOf<Pair<DMessageFile, PickedFileInfo>>()
    uris.forEach { uriStr ->
        val info = queryPickedFileInfo(uriStr) ?: return@forEach
        var fileName = info.displayName
        if (normalizeExtension) {
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
    return placeholders
}

/**
 * Import placeholder files into the content-addressable store and enrich them
 * with final metadata (duration, intrinsic dimensions). Files that fail to
 * import keep their placeholder source URI so the message stays visible.
 */
suspend fun importPickedFiles(placeholders: List<Pair<DMessageFile, PickedFileInfo>>): List<DMessageFile> = withIO {
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
                    durationMs = getMediaDurationMs(realPath),
                    width = intrinsicSize.width,
                    height = intrinsicSize.height,
                    summary = placeholder.summary,
                    fileName = placeholder.fileName,
                ),
            )
        } catch (ex: Exception) {
            showMessage(ex)
            finalItems.add(placeholder)
        }
    }
    finalItems
}

/**
 * Handle a file-pick result: create placeholder chat messages, import the
 * selected files into the content-addressable store, and update the messages
 * with final metadata (size, intrinsic dimensions, duration).
 *
 * Platform-specific steps (URI query, file import, media metadata) go through
 * the shared [buildPickedFilePlaceholders] / [importPickedFiles] helpers,
 * which the external share flow also uses.
 */
fun handleChatFileSelection(
    event: PickFileResultEvent,
    chatVM: ChatViewModel,
    peerVM: PeerViewModel,
    focusManager: FocusManager,
) {
    coMain {
        val placeholders = buildPickedFilePlaceholders(event.uris, normalizeExtension = event.type == PickFileType.IMAGE_VIDEO)
        if (placeholders.isEmpty()) return@coMain

        val placeholderItems = placeholders.map { it.first }
        val isImageVideo = event.type == PickFileType.IMAGE_VIDEO
        val messageId = chatVM.sendFilesImmediate(placeholderItems, isImageVideo)
        delay(200)
        focusManager.clearFocus()

        val finalItems = importPickedFiles(placeholders)
        chatVM.updateFilesMessage(messageId, finalItems, isImageVideo, PeerCacher.getOnlinePeerIds())
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
