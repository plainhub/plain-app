package com.ismartcoding.plain.ui.page.chat

import com.ismartcoding.plain.i18n.*

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.ismartcoding.plain.db.getBestIp
import com.ismartcoding.plain.enums.DeviceType
import com.ismartcoding.plain.ui.base.BottomSpace
import com.ismartcoding.plain.ui.base.NavigationBackIcon
import com.ismartcoding.plain.ui.base.PCard
import com.ismartcoding.plain.ui.base.PListItem
import com.ismartcoding.plain.ui.base.PScaffold
import com.ismartcoding.plain.ui.base.PTopAppBar
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.helpers.DialogHelper
import com.ismartcoding.plain.ui.models.ChatViewModel
import com.ismartcoding.plain.ui.models.PeerViewModel
import com.ismartcoding.plain.ui.models.clearAllMessages

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

    val clearMessagesText = stringResource(Res.string.clear_messages)
    val clearMessagesConfirmText = stringResource(Res.string.clear_messages_confirm)
    val cancelText = stringResource(Res.string.cancel)
    val deleteDeviceText = stringResource(Res.string.delete_device)
    val deleteText = stringResource(Res.string.delete)
    val deleteDeviceWarningText = stringResource(Res.string.delete_peer_warning)

    PScaffold(
        topBar = {
            PTopAppBar(
                navController = navController,
                navigationIcon = {
                    NavigationBackIcon { navController.navigateUp() }
                },
                title = stringResource(Res.string.chat_info),
            )
        },
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = paddingValues.calculateTopPadding()),
        ) {
            if (peer != null) {
                item {
                    PCard {
                        PListItem(title = stringResource(Res.string.peer_id), value = peer.id)
                        PListItem(title = stringResource(Res.string.ip_address), value = peer.getBestIp())
                        PListItem(title = stringResource(Res.string.port), value = peer.port.toString())
                        PListItem(title = stringResource(Res.string.device_type), value = DeviceType.fromValue(peer.deviceType).getText())
                        val status = peer.getStatusText()
                        if (status.isNotEmpty()) {
                            PListItem(title = stringResource(Res.string.status), value = status)
                        }
                    }
                }
            }

            item { VerticalSpace(dp = 24.dp) }

            clearMessagesItem(
                clearMessagesText = clearMessagesText,
                clearMessagesConfirmText = clearMessagesConfirmText,
                cancelText = cancelText,
                onClear = {
                    chatVM.clearAllMessages(context)
                    navController.navigateUp()
                    DialogHelper.showSuccess(Res.string.messages_cleared)
                },
            )

            if (peer != null) {
                deleteDeviceItem(
                    deleteDeviceText = deleteDeviceText,
                    deleteText = deleteText,
                    deleteDeviceWarningText = deleteDeviceWarningText,
                    cancelText = cancelText,
                    onDelete = {
                        peerVM.removePeer(context, peer.id)
                        navController.popBackStack(navController.graph.startDestinationId, false)
                    },
                )
            }

            item { BottomSpace(paddingValues) }
        }
    }
}
