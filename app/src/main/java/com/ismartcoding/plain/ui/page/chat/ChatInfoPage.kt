package com.ismartcoding.plain.ui.page.chat

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.ismartcoding.plain.R
import com.ismartcoding.plain.db.AppDatabase
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
import com.ismartcoding.plain.ui.models.ChatViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatInfoPage(
    navController: NavHostController,
    chatVM: ChatViewModel,
) {
    val context = LocalContext.current
    val chatState = chatVM.chatState.collectAsState()
    val peer = chatState.value.peer
    val channel = chatState.value.channel

    // Resolve member peer names from DB
    val memberNames = produceState(initialValue = emptyList<String>(), key1 = channel?.members) {
        value = channel?.members?.mapNotNull { peerId ->
            AppDatabase.instance.peerDao().getById(peerId)?.name?.takeIf { it.isNotBlank() } ?: peerId
        } ?: emptyList()
    }

    val clearMessagesText = stringResource(R.string.clear_messages)
    val clearMessagesConfirmText = stringResource(R.string.clear_messages_confirm)
    val cancelText = stringResource(R.string.cancel)

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
            if (peer != null) {
                item {
                    PCard {
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
                        PListItem(
                            title = stringResource(R.string.status),
                            value = peer.getStatusText(),
                        )
                    }
                }
            }

            if (channel != null) {
                item {
                    PCard {
                        PListItem(
                            title = stringResource(R.string.name),
                            value = channel.name,
                        )
                        if (memberNames.value.isNotEmpty()) {
                            PListItem(
                                title = stringResource(R.string.members),
                                value = memberNames.value.joinToString(", "),
                            )
                        }
                    }
                }
            }

            item {
                VerticalSpace(dp = 24.dp)
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

            item {
                BottomSpace(paddingValues)
            }
        }
    }
}
