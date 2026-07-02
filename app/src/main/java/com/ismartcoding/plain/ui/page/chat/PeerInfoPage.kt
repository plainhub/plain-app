package com.ismartcoding.plain.ui.page.chat

import com.ismartcoding.plain.i18n.*

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.ismartcoding.plain.chat.peer.PeerCacher
import com.ismartcoding.plain.chat.peer.transport.WifiAwareTransport
import com.ismartcoding.plain.db.getBestIp
import com.ismartcoding.plain.enums.ButtonType
import com.ismartcoding.plain.enums.DeviceType
import com.ismartcoding.plain.preferences.DeveloperModePreference
import com.ismartcoding.plain.ui.base.BottomSpace
import com.ismartcoding.plain.ui.base.NavigationBackIcon
import com.ismartcoding.plain.ui.base.PCard
import com.ismartcoding.plain.ui.base.PListItem
import com.ismartcoding.plain.ui.base.POutlinedButton
import com.ismartcoding.plain.ui.base.PScaffold
import com.ismartcoding.plain.ui.base.PTopAppBar
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.helpers.DialogHelper
import com.ismartcoding.plain.ui.models.ChatViewModel
import com.ismartcoding.plain.ui.models.PeerViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PeerInfoPage(
    navController: NavHostController,
    chatVM: ChatViewModel,
    peerVM: PeerViewModel,
) {
    val context = LocalContext.current
    val chatTarget = chatVM.target.collectAsState()
    val peer = PeerCacher.getPeer(chatTarget.value.toId)
    var developerMode by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { developerMode = DeveloperModePreference.getAsync() }

    PScaffold(
        topBar = {
            PTopAppBar(
                navController = navController,
                title = stringResource(Res.string.peer_info),
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

            item {
                val clearMessagesText = stringResource(Res.string.clear_messages)
                val clearMessagesConfirmText = stringResource(Res.string.clear_messages_confirm)
                val cancelText = stringResource(Res.string.cancel)
                POutlinedButton(
                    text = clearMessagesText,
                    type = ButtonType.DANGER,
                    modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth(),
                    onClick = {
                        DialogHelper.showConfirmDialog(
                            title = clearMessagesText,
                            message = clearMessagesConfirmText,
                            confirmButton = Pair(clearMessagesText) {
                                chatVM.clearAllMessages(context)
                                navController.navigateUp()
                                DialogHelper.showSuccess(Res.string.messages_cleared)
                            },
                            dismissButton = Pair(cancelText) {},
                        )
                    },
                )
            }

            if (peer != null) {
                item {
                    val deleteDeviceText = stringResource(Res.string.delete_device)
                    val deleteText = stringResource(Res.string.delete)
                    val deleteDeviceWarningText = stringResource(Res.string.delete_peer_warning)
                    val cancelText = stringResource(Res.string.cancel)
                    VerticalSpace(dp = 16.dp)
                    POutlinedButton(
                        text = deleteDeviceText,
                        type = ButtonType.DANGER,
                        modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth(),
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

            item { BottomSpace(paddingValues) }
        }
    }
}

private fun agoString(seconds: Long): String {
    return when {
        seconds < 60 -> "${seconds}s"
        seconds < 3600 -> "${seconds / 60}m ${seconds % 60}s"
        else -> "${seconds / 3600}h ${(seconds % 3600) / 60}m"
    }
}
