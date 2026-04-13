package com.ismartcoding.plain.ui.page.dlna

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.ismartcoding.plain.R
import com.ismartcoding.plain.features.dlna.DlnaMediaType
import com.ismartcoding.plain.features.dlna.DlnaPlaybackState
import com.ismartcoding.plain.features.dlna.DlnaRendererState
import com.ismartcoding.plain.ui.base.AlertType
import com.ismartcoding.plain.ui.base.BottomSpace
import com.ismartcoding.plain.ui.base.PAlert
import com.ismartcoding.plain.ui.base.PBanner
import com.ismartcoding.plain.ui.base.PIconButton
import com.ismartcoding.plain.ui.base.POutlinedButton
import com.ismartcoding.plain.ui.base.PScaffold
import com.ismartcoding.plain.ui.base.PTopAppBar
import com.ismartcoding.plain.ui.models.DlnaReceiverViewModel
import com.ismartcoding.plain.ui.nav.Routing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DlnaReceiverPage(
    navController: NavHostController,
    vm: DlnaReceiverViewModel = viewModel(),
) {
    val context = LocalContext.current

    DisposableEffect(Unit) {
        vm.startReceiver(context)
        onDispose { vm.stopReceiver(context) }
    }

    val mediaUri by DlnaRendererState.mediaUri.collectAsState()
    val mediaType by DlnaRendererState.mediaType.collectAsState()
    val playbackState by DlnaRendererState.playbackState.collectAsState()
    val pendingCastRequest by DlnaRendererState.pendingCastRequest.collectAsState()
    val startError by DlnaRendererState.startError.collectAsState()
    val hasMedia = mediaUri.isNotEmpty() && playbackState != DlnaPlaybackState.NO_MEDIA_PRESENT

    pendingCastRequest?.let { pending ->
        var rememberChoice by remember { mutableStateOf(false) }
        val displayName = pending.senderName.ifEmpty { pending.senderIp.ifEmpty { stringResource(R.string.unknown) } }
        AlertDialog(
            onDismissRequest = {},
            title = { Text(stringResource(R.string.dlna_cast_request_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(R.string.dlna_cast_request_desc, displayName))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Checkbox(
                            checked = rememberChoice,
                            onCheckedChange = { rememberChoice = it },
                        )
                        Text(
                            text = stringResource(R.string.dlna_cast_remember_choice),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = { vm.acceptCastRequest(context, rememberChoice) }) {
                    Text(stringResource(R.string.dlna_cast_accept))
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { vm.rejectCastRequest(context, rememberChoice) }) {
                    Text(stringResource(R.string.dlna_cast_reject))
                }
            },
        )
    }

    if (hasMedia) {
        val exitAction = {
            DlnaRendererState.mediaUri.value = ""
            DlnaRendererState.playbackState.value = DlnaPlaybackState.NO_MEDIA_PRESENT
        }
        BackHandler { exitAction() }
        when (mediaType) {
            DlnaMediaType.AUDIO -> DlnaReceiverAudioPlayer(vm = vm, onExit = exitAction)
            DlnaMediaType.IMAGE -> DlnaReceiverImageViewer(onExit = exitAction)
            else -> DlnaReceiverVideoPlayer(vm = vm, onExit = exitAction)
        }
        return
    }

    PScaffold(
        topBar = {
            PTopAppBar(
                navController = navController,
                title = stringResource(R.string.dlna_receiver),
                actions = {
                    PIconButton(
                        icon = R.drawable.history,
                        contentDescription = stringResource(R.string.dlna_cast_history),
                        tint = MaterialTheme.colorScheme.onSurface
                    ) { navController.navigate(Routing.DlnaCastHistory) }
                },
            )
        },
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = paddingValues.calculateTopPadding()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (startError.isNotEmpty()) {
                item {
                    PAlert(
                        description = stringResource(R.string.dlna_receiver_start_error),
                        type = AlertType.ERROR,
                        actions = {
                            POutlinedButton(
                                stringResource(R.string.retry),
                                onClick = { vm.retryReceiver(context) }
                            )
                        },
                    )
                }
            }
            item { DlnaReceiverWaitingScreen() }
            item { BottomSpace() }
        }
    }
}


