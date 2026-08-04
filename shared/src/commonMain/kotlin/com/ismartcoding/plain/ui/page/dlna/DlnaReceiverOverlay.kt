package com.ismartcoding.plain.ui.page.dlna

import com.ismartcoding.plain.i18n.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.enums.ButtonSize
import com.ismartcoding.plain.features.dlna.DlnaMediaType
import com.ismartcoding.plain.features.dlna.DlnaPlaybackState
import com.ismartcoding.plain.features.dlna.DlnaRendererState
import com.ismartcoding.plain.helpers.TimeHelper
import com.ismartcoding.plain.platform.PBackHandler
import com.ismartcoding.plain.platform.createDownloadTempFile
import com.ismartcoding.plain.platform.fetchUrlToStream
import com.ismartcoding.plain.platform.saveTempFileToDownloads
import com.ismartcoding.plain.ui.base.PFilledButton
import com.ismartcoding.plain.ui.helpers.DialogHelper
import com.ismartcoding.plain.web.http.StreamSink
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString

/**
 * Global overlay that renders the DLNA cast-request dialog and media player on
 * top of any screen. This allows the receiver to accept casts regardless of
 * which page the user is on — the engine ([DlnaReceiverEngine]) runs
 * independently and promotes cast requests to [DlnaRendererState.pendingCastRequest]
 * / [DlnaRendererState.mediaUri], which this overlay observes.
 *
 * Should be placed inside the root [Main] composable's Box so it covers the
 * whole screen when active.
 */
@Composable
fun DlnaReceiverOverlay() {
    DlnaCastRequestDialog()
    DlnaMediaPlayerOverlay()
}

@Composable
private fun DlnaCastRequestDialog() {
    val pending by DlnaRendererState.pendingCastRequest.collectAsState()
    val pendingRequest = pending ?: return
    var rememberChoice by remember { mutableStateOf(false) }
    val displayName = pendingRequest.senderName.ifEmpty {
        pendingRequest.senderIp.ifEmpty { stringResource(Res.string.unknown) }
    }
    AlertDialog(
        onDismissRequest = {},
        title = { Text(stringResource(Res.string.dlna_cast_request_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(Res.string.dlna_cast_request_desc, displayName))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Checkbox(
                        checked = rememberChoice,
                        onCheckedChange = { rememberChoice = it },
                    )
                    Text(
                        text = stringResource(Res.string.dlna_cast_remember_choice),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        },
        confirmButton = {
            PFilledButton(
                text = stringResource(Res.string.dlna_cast_accept),
                buttonSize = ButtonSize.MEDIUM,
                onClick = { DlnaRendererState.acceptCastRequest(rememberChoice) },
            )
        },
        dismissButton = {
            OutlinedButton(onClick = { DlnaRendererState.rejectCastRequest(rememberChoice) }) {
                Text(stringResource(Res.string.dlna_cast_reject))
            }
        },
    )
}

@Composable
private fun DlnaMediaPlayerOverlay() {
    val mediaUri by DlnaRendererState.mediaUri.collectAsState()
    val mediaType by DlnaRendererState.mediaType.collectAsState()
    val playbackState by DlnaRendererState.playbackState.collectAsState()
    val hasMedia = mediaUri.isNotEmpty() && playbackState != DlnaPlaybackState.NO_MEDIA_PRESENT
    if (!hasMedia) return

    val exitAction = {
        DlnaRendererState.mediaUri.value = ""
        DlnaRendererState.playbackState.value = DlnaPlaybackState.NO_MEDIA_PRESENT
    }
    PBackHandler { exitAction() }
    when (mediaType) {
        DlnaMediaType.AUDIO -> DlnaReceiverAudioPlayerContent(onExit = exitAction)
        DlnaMediaType.IMAGE -> DlnaReceiverImageViewerContent(onExit = exitAction)
        else -> DlnaReceiverVideoPlayerContent(onExit = exitAction)
    }
}

// ── Download button (shared by video / image / audio player content) ───────

private enum class DlnaDownloadState { IDLE, DOWNLOADING, SUCCESS, FAILED }

/**
 * Download icon button for the DLNA receiver playback UI. Downloads the
 * currently playing media ([DlnaRendererState.mediaUri]) directly to the
 * system Downloads directory via [fetchUrlToStream] + [saveTempFileToDownloads].
 *
 * Shows a [CircularProgressIndicator] while downloading, a check icon on
 * success, and reverts to the download icon after a brief delay. A toast
 * notification is shown after the download completes (success or failure).
 */
@Composable
fun DlnaDownloadIconButton(
    modifier: Modifier = Modifier,
    iconTint: Color = Color.White,
) {
    val mediaUri by DlnaRendererState.mediaUri.collectAsState()
    val mediaType by DlnaRendererState.mediaType.collectAsState()
    val mediaTitle by DlnaRendererState.mediaTitle.collectAsState()
    var downloadState by remember { mutableStateOf(DlnaDownloadState.IDLE) }
    val scope = rememberCoroutineScope()

    IconButton(
        onClick = {
            if (downloadState == DlnaDownloadState.DOWNLOADING) return@IconButton
            downloadState = DlnaDownloadState.DOWNLOADING
            scope.launch {
                val success = downloadDlnaMedia(mediaUri, mediaType, mediaTitle)
                downloadState = if (success) DlnaDownloadState.SUCCESS else DlnaDownloadState.FAILED
                delay(2000)
                downloadState = DlnaDownloadState.IDLE
            }
        },
        modifier = modifier,
    ) {
        when (downloadState) {
            DlnaDownloadState.DOWNLOADING -> CircularProgressIndicator(
                color = iconTint,
                strokeWidth = 2.dp,
                modifier = Modifier.size(24.dp),
            )
            DlnaDownloadState.SUCCESS -> Icon(
                painter = painterResource(Res.drawable.check),
                contentDescription = stringResource(Res.string.download),
                tint = iconTint,
                modifier = Modifier.size(24.dp),
            )
            else -> Icon(
                painter = painterResource(Res.drawable.download),
                contentDescription = stringResource(Res.string.download),
                tint = iconTint,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

/** Adapts [DownloadTempFileHandle] to the [StreamSink] interface for [fetchUrlToStream]. */
private class DownloadTempFileSink(private val handle: com.ismartcoding.plain.platform.DownloadTempFileHandle) : StreamSink {
    override suspend fun write(bytes: ByteArray) { handle.write(bytes, 0, bytes.size) }
    override suspend fun write(bytes: ByteArray, offset: Int, length: Int) { handle.write(bytes, offset, length) }
    override suspend fun flush() {}
    override suspend fun close() { handle.close() }
}

/**
 * Downloads [url] to a temp file via [fetchUrlToStream], then moves it to the
 * system Downloads directory via [saveTempFileToDownloads]. Shows a toast
 * notification with the saved path on success, or an error message on failure.
 */
private suspend fun downloadDlnaMedia(url: String, mediaType: DlnaMediaType, title: String): Boolean {
    if (url.isEmpty()) return false
    val ext = defaultExtensionFor(mediaType)
    val baseName = title.ifEmpty { "dlna_${TimeHelper.nowMillis()}" }
    val filename = "$baseName.$ext"
    val handle = createDownloadTempFile("dlna_${TimeHelper.nowMillis()}")
    val sink = DownloadTempFileSink(handle)
    try {
        val (status, _) = fetchUrlToStream(url, sink)
        if (status !in 200..299) {
            handle.delete()
            DialogHelper.showMessage(getString(Res.string.dlna_download_failed))
            return false
        }
        val savedPath = saveTempFileToDownloads(handle, filename)
        if (savedPath.isNotEmpty()) {
            DialogHelper.showMessage(getString(Res.string.dlna_download_success, savedPath))
            return true
        } else {
            DialogHelper.showMessage(getString(Res.string.dlna_download_failed))
            return false
        }
    } catch (_: Exception) {
        handle.delete()
        DialogHelper.showMessage(getString(Res.string.dlna_download_failed))
        return false
    }
}

private fun defaultExtensionFor(mediaType: DlnaMediaType): String = when (mediaType) {
    DlnaMediaType.VIDEO -> "mp4"
    DlnaMediaType.AUDIO -> "mp3"
    DlnaMediaType.IMAGE -> "jpg"
    else -> "bin"
}
