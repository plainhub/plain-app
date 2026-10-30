package com.ismartcoding.plain.ui.dlna

import com.ismartcoding.plain.features.files.FilesClient

import com.ismartcoding.plain.i18n.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.ismartcoding.plain.ui.components.ButtonSize
import com.ismartcoding.plain.lib.dlna.DlnaMediaType
import com.ismartcoding.plain.features.dlna.DlnaPlaybackState
import com.ismartcoding.plain.features.dlna.DlnaPlayerReports
import com.ismartcoding.plain.features.dlna.DlnaRendererState
import com.ismartcoding.plain.lib.TimeHelper
import com.ismartcoding.plain.features.files.RemoteFiles
import com.ismartcoding.plain.platform.files.saveFileToDownloads
import com.ismartcoding.plain.platform.files.deleteFileAt
import com.ismartcoding.plain.lib.withIO
import com.ismartcoding.plain.ui.navigation.PBackHandler
import com.ismartcoding.plain.ui.components.CheckCircle
import com.ismartcoding.plain.ui.components.SaveToSheet
import com.ismartcoding.plain.ui.base.PFilledButton
import com.ismartcoding.plain.ui.components.DialogHelper
import com.ismartcoding.plain.ui.base.HorizontalSpace
import com.ismartcoding.plain.ui.theme.dialogSheetBackground
import com.ismartcoding.plain.ui.state.launchSafe
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import com.ismartcoding.plain.ui.resources.Res as UiRes
import com.ismartcoding.plain.ui.resources.check as ui_drawable_check
import com.ismartcoding.plain.ui.resources.download as ui_drawable_download
import com.ismartcoding.plain.i18n.download
import kotlinx.coroutines.CancellationException

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
    LaunchedEffect(Unit) {
        launchSafe { DlnaRendererState.refresh() }
    }
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
        containerColor = MaterialTheme.colorScheme.dialogSheetBackground,
        onDismissRequest = {},
        title = { Text(stringResource(Res.string.dlna_cast_request_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(Res.string.dlna_cast_request_desc, displayName))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    CheckCircle(selected = rememberChoice, onClick = { rememberChoice = !rememberChoice })
                    HorizontalSpace(8.dp)
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

    val exitAction = { DlnaPlayerReports.reportStopped(mediaUri) }
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
 * system Downloads directory.
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
    var showSaveSheet by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun startDownload(dir: String?) {
        if (downloadState == DlnaDownloadState.DOWNLOADING) return
        downloadState = DlnaDownloadState.DOWNLOADING
        scope.launch {
            val success = downloadDlnaMedia(mediaUri, mediaType, mediaTitle, dir)
            downloadState = if (success) DlnaDownloadState.SUCCESS else DlnaDownloadState.FAILED
            delay(2000)
            downloadState = DlnaDownloadState.IDLE
        }
    }

    IconButton(
        onClick = {
            if (downloadState == DlnaDownloadState.DOWNLOADING) return@IconButton
            showSaveSheet = true
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
                painter = painterResource(UiRes.drawable.ui_drawable_check),
                contentDescription = stringResource(Res.string.download),
                tint = iconTint,
                modifier = Modifier.size(24.dp),
            )
            else -> Icon(
                painter = painterResource(UiRes.drawable.ui_drawable_download),
                contentDescription = stringResource(Res.string.download),
                tint = iconTint,
                modifier = Modifier.size(24.dp),
            )
        }
    }

    if (showSaveSheet) {
        SaveToSheet(
            title = mediaTitle.ifEmpty { mediaType.name.lowercase().replaceFirstChar { it.uppercase() } },
            onDismiss = { showSaveSheet = false },
            onDownloads = {
                showSaveSheet = false
                startDownload(null)
            },
            onDirectory = { dir ->
                showSaveSheet = false
                startDownload(dir)
            },
        )
    }
}

private suspend fun downloadDlnaMedia(url: String, mediaType: DlnaMediaType, title: String, dir: String? = null): Boolean {
    if (url.isEmpty()) return false
    val filename = "${title.ifEmpty { "dlna_${TimeHelper.nowMillis()}" }}.${defaultExtensionFor(mediaType)}"
    var temporary: String? = null
    return try {
        val path = RemoteFiles.download(url, filename)
        temporary = path
        val saved = withIO { if (dir == null) saveFileToDownloads(path, filename) else FilesClient.copyToDirectory(path, dir, filename) }
        if (saved.isEmpty()) {
            DialogHelper.showMessage(getString(Res.string.dlna_download_failed))
            false
        } else {
            DialogHelper.showMessage(getString(Res.string.dlna_download_success, saved))
            true
        }
    } catch (cancelled: CancellationException) { throw cancelled }
    catch (_: Exception) {
        DialogHelper.showMessage(getString(Res.string.dlna_download_failed))
        false
    } finally { temporary?.let { withIO { deleteFileAt(it) } } }
}

private fun defaultExtensionFor(mediaType: DlnaMediaType): String = when (mediaType) {
    DlnaMediaType.VIDEO -> "mp4"
    DlnaMediaType.AUDIO -> "mp3"
    DlnaMediaType.IMAGE -> "jpg"
    else -> "bin"
}
