package com.ismartcoding.plain.ui.chat

import com.ismartcoding.plain.ui.permissions.getText
import com.ismartcoding.plain.ui.permissions.getGrantAccessText

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
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.ismartcoding.plain.features.chat.data.ChatTarget
import com.ismartcoding.plain.ui.components.ButtonType
import com.ismartcoding.plain.platform.system.DeviceType
import com.ismartcoding.plain.ui.platform.system.getText
import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.ui.base.BottomSpace
import com.ismartcoding.plain.ui.components.NavigationBackIcon
import com.ismartcoding.plain.ui.base.PCard
import com.ismartcoding.plain.ui.base.PListItem
import com.ismartcoding.plain.ui.base.POutlinedButton
import com.ismartcoding.plain.ui.base.PScaffold
import com.ismartcoding.plain.ui.base.PTopAppBar
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.components.DialogHelper
import com.ismartcoding.plain.ui.chat.PeerViewModel
import com.ismartcoding.plain.ui.theme.PlainTheme
import kotlinx.coroutines.flow.StateFlow
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PeerInfoPage(
    navController: NavHostController,
    target: StateFlow<ChatTarget>,
    onClearAllMessages: () -> Unit,
    peerVM: PeerViewModel,
) {
    val chatTarget = target.collectAsState()
    val peers by peerVM.peersMap.collectAsState()
    val peer = peers[chatTarget.value.toId]
    LaunchedEffect(peerVM) { peerVM.observe() }

    PScaffold(
        topBar = {
            PTopAppBar(
                onNavigateBack = { navController.navigateUp() },
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
                    PCard(modifier = Modifier.padding(horizontal = PlainTheme.PAGE_HORIZONTAL_MARGIN)) {
                        PListItem(title = stringResource(Res.string.peer_id), value = peer.id)
                        PListItem(title = stringResource(Res.string.ip_address), value = peer.ip)
                        PListItem(title = stringResource(Res.string.port), value = peer.port?.toString().orEmpty())
                        PListItem(title = stringResource(Res.string.device_type), value = peer.deviceType.getText())
                        val status = peer.status.getText()
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
                                onClearAllMessages()
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
                                    peerVM.removePeer(peer.id)
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
