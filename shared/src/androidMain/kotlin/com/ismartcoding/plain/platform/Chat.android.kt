package com.ismartcoding.plain.platform

import android.content.Context
import android.webkit.MimeTypeMap
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.unit.IntSize
import androidx.core.net.toUri
import com.ismartcoding.plain.appContext
import com.ismartcoding.plain.chat.peer.PeerCacher
import com.ismartcoding.plain.db.DChat
import com.ismartcoding.plain.db.DMessageFile
import com.ismartcoding.plain.enums.PickFileType
import com.ismartcoding.plain.events.PickFileResultEvent
import com.ismartcoding.plain.extensions.getDuration
import com.ismartcoding.plain.features.ChatMessageEditor
import com.ismartcoding.plain.helpers.AppFileStore
import com.ismartcoding.plain.helpers.ChatFileSaveHelper
import com.ismartcoding.plain.helpers.ImageHelper
import com.ismartcoding.plain.helpers.VideoHelper
import com.ismartcoding.plain.helpers.coMain
import com.ismartcoding.plain.helpers.withIO
import com.ismartcoding.plain.lib.extensions.getFilenameWithoutExtension
import com.ismartcoding.plain.lib.extensions.isImageFast
import com.ismartcoding.plain.lib.extensions.isVideoFast
import com.ismartcoding.plain.lib.extensions.queryOpenableFile
import com.ismartcoding.plain.ui.helpers.DialogHelper
import com.ismartcoding.plain.ui.models.ChatViewModel
import com.ismartcoding.plain.ui.models.PeerViewModel
import kotlinx.coroutines.delay
import java.io.File

actual object ChatPlatformOps {
    private val context: Context get() = appContext

    actual fun handleFileSelection(
        event: PickFileResultEvent,
        chatVM: ChatViewModel,
        peerVM: PeerViewModel,
        focusManager: FocusManager,
    ) {
        coMain {
            val uriList = event.uris.map { android.net.Uri.parse(it) }
            val placeholderItems = mutableListOf<DMessageFile>()
            uriList.forEach { uri ->
                val file = context.contentResolver.queryOpenableFile(uri)
                if (file != null) {
                    var fileName = file.displayName
                    val mimeType = context.contentResolver.getType(uri) ?: ""
                    if (event.type == PickFileType.IMAGE_VIDEO) {
                        val extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType) ?: ""
                        if (extension.isNotEmpty()) {
                            fileName = fileName.getFilenameWithoutExtension() + "." + extension
                        }
                    }
                    placeholderItems.add(
                        DMessageFile(id = com.ismartcoding.plain.helpers.StringHelper.shortUUID(), uri = uri.toString(), size = file.size, fileName = fileName)
                    )
                }
            }
            if (placeholderItems.isEmpty()) return@coMain

            val isImageVideo = event.type == PickFileType.IMAGE_VIDEO
            val messageId = chatVM.sendFilesImmediate(placeholderItems, isImageVideo)
            delay(200)
            focusManager.clearFocus()

            withIO {
                val finalItems = mutableListOf<DMessageFile>()
                uriList.forEachIndexed { index, uri ->
                    try {
                        val placeholder = placeholderItems[index]
                        val mimeType = context.contentResolver.getType(uri) ?: ""
                        val fidUri = ChatFileSaveHelper.importFromUri(context, uri, mimeType)
                        val realPath = AppFileStore.resolveUri(fidUri)
                        val intrinsicSize = if (placeholder.fileName.isImageFast())
                            ImageHelper.getIntrinsicSize(realPath, ImageHelper.getRotation(realPath))
                        else if (placeholder.fileName.isVideoFast())
                            VideoHelper.getIntrinsicSize(realPath)
                        else IntSize.Zero
                        finalItems.add(
                            DMessageFile(
                                id = placeholder.id, uri = fidUri, size = placeholder.size,
                                duration = File(realPath).getDuration(context),
                                width = intrinsicSize.width, height = intrinsicSize.height,
                                summary = placeholder.summary, fileName = placeholder.fileName,
                            )
                        )
                    } catch (ex: Exception) {
                        DialogHelper.showMessage(ex)
                        ex.printStackTrace()
                        finalItems.add(placeholderItems[index])
                    }
                }
                chatVM.updateFilesMessage(messageId, finalItems, isImageVideo, PeerCacher.getOnlinePeerIds())
            }
        }
    }
}

actual suspend fun updateChatMessageTextAsync(item: DChat, newText: String): Boolean {
    return ChatMessageEditor.updateTextAsync(item, newText)
}
