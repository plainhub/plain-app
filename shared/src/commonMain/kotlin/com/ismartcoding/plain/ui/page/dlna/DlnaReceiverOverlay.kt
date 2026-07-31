package com.ismartcoding.plain.ui.page.dlna

import com.ismartcoding.plain.i18n.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.enums.ButtonSize
import com.ismartcoding.plain.features.dlna.DlnaMediaType
import com.ismartcoding.plain.features.dlna.DlnaPlaybackState
import com.ismartcoding.plain.features.dlna.DlnaRendererState
import com.ismartcoding.plain.platform.DlnaReceiverAudioPlayerContent
import com.ismartcoding.plain.platform.DlnaReceiverImageViewerContent
import com.ismartcoding.plain.platform.DlnaReceiverVideoPlayerContent
import com.ismartcoding.plain.platform.PBackHandler
import com.ismartcoding.plain.ui.base.PFilledButton

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
