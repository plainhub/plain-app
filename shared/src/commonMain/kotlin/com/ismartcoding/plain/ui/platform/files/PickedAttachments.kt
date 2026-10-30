package com.ismartcoding.plain.ui.platform.files

import com.ismartcoding.plain.features.media.MediaClient

import com.ismartcoding.plain.platform.files.queryPickedFileInfo

import com.ismartcoding.plain.platform.media.getExtensionFromMimeType

import androidx.compose.ui.unit.IntSize
import com.ismartcoding.plain.features.chat.models.MessageFile
import com.ismartcoding.plain.features.files.stored.AppFileStore
import com.ismartcoding.plain.lib.extensions.isAudioFast
import com.ismartcoding.plain.lib.extensions.isImageFast
import com.ismartcoding.plain.lib.extensions.isVideoFast
import com.ismartcoding.plain.lib.withIO
import com.ismartcoding.plain.ui.components.DialogHelper
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

internal object PickedAttachments {
    suspend fun <T> withFiles(uris: List<String>, normalize: Boolean, source: com.ismartcoding.plain.rust.graphql.ContentSource = com.ismartcoding.plain.rust.graphql.ContentSource.Local, send: suspend (List<MessageFile>) -> T): T = withIO {
        val imports = mutableListOf<String>()
        try {
            val attachments = uris.distinct().mapNotNull { uri ->
                try {
                    val info = queryPickedFileInfo(uri) ?: error("Unable to read selected file information")
                    val file = withContext(NonCancellable) {
                        (if (source is com.ismartcoding.plain.rust.graphql.ContentSource.Remote) source.client.importAttachment(uri, info.displayName, info.mimeType)
                        else AppFileStore.importFile(uri, info.displayName, info.mimeType)).also { imports.add(it.id) }
                    }
                    currentCoroutineContext().ensureActive()
                    val path = file.realPath
                    val isVideo = info.mimeType.startsWith("video/") || info.displayName.isVideoFast()
                    val video = if (isVideo) source.media.videoInfo(path) else null
                    val size = when {
                        info.mimeType.startsWith("image/") || info.displayName.isImageFast() ->
                            source.media.imageInfo(path)?.let { IntSize(it.width, it.height) } ?: IntSize.Zero
                        video != null -> IntSize(video.displayWidth, video.displayHeight)
                        else -> IntSize.Zero
                    }
                    val extension = getExtensionFromMimeType(info.mimeType)
                    val name = if (normalize && extension.isNotEmpty()) "${info.displayName.substringBeforeLast('.', info.displayName)}.$extension" else info.displayName
                    val duration = when {
                        video != null -> video.durationMs
                        isVideo || info.mimeType.startsWith("audio/") || info.displayName.isAudioFast() -> source.media.durationMs(path)
                        else -> 0L
                    }
                    MessageFile(uri = AppFileStore.toFidUri(file), size = file.size, fileName = name,
                        width = size.width, height = size.height, durationMs = duration)
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (error: Exception) {
                    DialogHelper.showMessage(error.message ?: "Unable to read selected file")
                    null
                }
            }
            currentCoroutineContext().ensureActive()
            withContext(NonCancellable) { send(attachments) }
        } finally {
            withContext(NonCancellable) {
                var failure: Exception? = null
                imports.forEach { id ->
                    try { source.storedFiles.release(id) } catch (error: Exception) { failure = error }
                }
                failure?.let { throw it }
            }
        }
    }
}
