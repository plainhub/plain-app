package com.ismartcoding.plain.ui.page.chat

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.ismartcoding.plain.R
import com.ismartcoding.plain.enums.ButtonType
import com.ismartcoding.plain.enums.DeviceType
import com.ismartcoding.plain.ui.base.BottomSpace
import com.ismartcoding.plain.ui.base.NavigationBackIcon
import com.ismartcoding.plain.ui.base.PCard
import com.ismartcoding.plain.ui.base.PListItem
import com.ismartcoding.plain.ui.base.POutlinedButton
import com.ismartcoding.plain.ui.base.PScaffold
import com.ismartcoding.plain.ui.base.PTopAppBar
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.helpers.DialogHelper
import com.ismartcoding.plain.ui.models.ChatType
import com.ismartcoding.plain.ui.models.ChatViewModel
import com.ismartcoding.plain.ui.models.PeerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PeerChatInfoPage(
    navController: NavHostController,
    chatVM: ChatViewModel,
    peerVM: PeerViewModel,
) {
    val context = LocalContext.current
    val chatState = chatVM.chatState.collectAsState()
    val peer = peerVM.pairedPeers.find { it.id == chatState.value.toId }

    val clearMessagesText = stringResource(R.string.clear_messages)
    val clearMessagesConfirmText = stringResource(R.string.clear_messages_confirm)
    val cancelText = stringResource(R.string.cancel)
    val deleteDeviceText = stringResource(R.string.delete_device)
    val deleteText = stringResource(R.string.delete)
    val deleteDeviceWarningText = stringResource(R.string.delete_peer_warning)

    PScaffold(
        topBar = {
            PTopAppBar(
                navController = navController,
                navigationIcon = {
                    NavigationBackIcon { navController.navigateUp() }
                },
                title = stringResource(R.string.chat_info),
            )
        },
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = paddingValues.calculateTopPadding()),
        ) {
            // Peer info card
            if (peer != null) {
                item {
                    PCard {
                        PListItem(
                            title = stringResource(R.string.peer_id),
                            value = peer.id,
                        )
                        PListItem(
                            title = stringResource(R.string.ip_address),
                            value = peer.getBestIp(),
                        )
                        PListItem(
                            title = stringResource(R.string.port),
                            value = peer.port.toString(),
                        )
                        PListItem(
                            title = stringResource(R.string.device_type),
                            value = DeviceType.fromValue(peer.deviceType).getText(),
                        )
                        val status = peer.getStatusText()
                        if (status.isNotEmpty()) {
                            PListItem(
                                title = stringResource(R.string.status),
                                value = status,
                            )
                        }
                    }
                }
            }

            item { VerticalSpace(dp = 24.dp) }

            // Clear messages button
            item {
                POutlinedButton(
                    text = clearMessagesText,
                    type = ButtonType.DANGER,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .padding(horizontal = 16.dp),
                    onClick = {
                        DialogHelper.showConfirmDialog(
                            title = clearMessagesText,
                            message = clearMessagesConfirmText,
                            confirmButton = Pair(clearMessagesText) {
                                chatVM.clearAllMessages(context)
                                navController.navigateUp()
                                DialogHelper.showSuccess(R.string.messages_cleared)
                            },
                            dismissButton = Pair(cancelText) {},
                        )
                    },
                )
            }

            // Delete device button
            if (peer != null) {
                item {
                    VerticalSpace(dp = 16.dp)
                    POutlinedButton(
                        text = deleteDeviceText,
                        type = ButtonType.DANGER,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                            .padding(horizontal = 16.dp),
                        onClick = {
                            DialogHelper.showConfirmDialog(
                                title = deleteDeviceText,
                                message = deleteDeviceWarningText,
                                confirmButton = Pair(deleteText) {
                                    peerVM.removePeer(context, peer.id)
                                    navController.popBackStack(navController.graph.startDestinationId, false)
                                },
                                dismissButton = Pair(cancelText) {},
                            )
                        },
                    )
                }
            }

            item {
                BottomSpace(paddingValues)
            }
        }
    }
}
