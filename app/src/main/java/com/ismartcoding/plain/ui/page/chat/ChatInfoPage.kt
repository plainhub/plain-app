package com.ismartcoding.plain.ui.page.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.ismartcoding.plain.R
import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.db.AppDatabase
import com.ismartcoding.plain.db.DPeer
import com.ismartcoding.plain.enums.ButtonType
import com.ismartcoding.plain.enums.DeviceType
import com.ismartcoding.plain.features.locale.LocaleHelper.getString
import com.ismartcoding.plain.helpers.PhoneHelper
import com.ismartcoding.plain.preferences.DeviceNamePreference
import com.ismartcoding.plain.ui.base.BottomSpace
import com.ismartcoding.plain.ui.base.NavigationBackIcon
import com.ismartcoding.plain.ui.base.PCard
import com.ismartcoding.plain.ui.base.PDialogListItem
import com.ismartcoding.plain.ui.base.PListItem
import com.ismartcoding.plain.ui.base.POutlinedButton
import com.ismartcoding.plain.ui.base.PScaffold
import com.ismartcoding.plain.ui.base.PTopAppBar
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.helpers.DialogHelper
import com.ismartcoding.plain.ui.models.ChatListViewModel
import com.ismartcoding.plain.ui.models.ChatViewModel
import com.ismartcoding.plain.ui.page.chat.components.ChannelMembersDialog
import com.ismartcoding.plain.ui.page.chat.components.RenameChannelDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.text.ifEmpty

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ChatInfoPage(
    navController: NavHostController,
    chatVM: ChatViewModel,
    chatListVM: ChatListViewModel,
) {
    val context = LocalContext.current
    val chatState = chatVM.chatState.collectAsState()
    val peer = chatState.value.peer
    val channel = chatState.value.channel

    // Use chatListVM.channels as the authoritative source — it is kept fresh by
    // loadPeers() after every add/remove mutation, unlike chatState.channel.
    val liveChannel = chatListVM.channels.find { it.id == channel?.id } ?: channel
    val isOwner = liveChannel?.owner == "me"

    var showRenameDialog by remember { mutableStateOf(false) }
    var showMembersDialog by remember { mutableStateOf(false) }
    var selectedMemberPeer by remember { mutableStateOf<DPeer?>(null) }

    // Resolve member DPeer objects from DB for showing device icons.
    // Key on liveChannel.members so the grid updates immediately after mutations.
    // Uses a single IN-clause query to avoid N individual DB round-trips.
    // If TempData.clientId appears in the member list (owner's own id stored in members),
    // it won't exist in the peers table — synthesise a placeholder DPeer for it.
    val memberPeers = produceState(initialValue = emptyList<DPeer>(), key1 = liveChannel?.members) {
        value = withContext(Dispatchers.IO) {
            liveChannel?.getPeersAsync() ?: return@withContext emptyList()
        }
    }

    val clearMessagesText = stringResource(R.string.clear_messages)
    val clearMessagesConfirmText = stringResource(R.string.clear_messages_confirm)
    val cancelText = stringResource(R.string.cancel)
    val deleteChannelText = stringResource(R.string.delete_channel)
    val deleteChannelWarningText = stringResource(R.string.delete_channel_warning)
    val leaveChannelText = stringResource(R.string.leave_channel)
    val leaveChannelWarningText = stringResource(R.string.leave_channel_warning)
    val deleteDeviceText = stringResource(R.string.delete_device)
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
            // --- Channel: Members grid (WeChat-style) ---
            if (liveChannel != null) {
                item {
                    FlowRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        memberPeers.value
                            .forEach { memberPeer ->
                                MemberGridItem(
                                    name = memberPeer.name.ifBlank { memberPeer.getBestIp() },
                                    iconRes = DeviceType.fromValue(memberPeer.deviceType).getIcon(),
                                    onClick = { selectedMemberPeer = memberPeer },
                                )
                            }
                        // "+" add button — only shown to channel owner
                        if (isOwner) {
                            AddMemberGridItem(
                                onClick = { showMembersDialog = true },
                            )
                        }
                    }
                }

                item { VerticalSpace(dp = 16.dp) }

                // Channel name (clickable to rename — owner only)
                item {
                    PCard {
                        PListItem(
                            modifier = if (isOwner) Modifier.clickable { showRenameDialog = true } else Modifier,
                            title = stringResource(R.string.channel_name),
                            value = liveChannel.name,
                            showMore = isOwner,
                        )
                    }
                }
            }

            // --- Peer (Device) info section ---
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

            // --- Action buttons ---
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

            // Delete channel button (owner only)
            if (liveChannel != null && isOwner) {
                item {
                    VerticalSpace(dp = 16.dp)
                    POutlinedButton(
                        text = deleteChannelText,
                        type = ButtonType.DANGER,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                            .padding(horizontal = 16.dp),
                        onClick = {
                            DialogHelper.showConfirmDialog(
                                title = deleteChannelText,
                                message = deleteChannelWarningText,
                                confirmButton = Pair(deleteChannelText) {
                                    chatListVM.removeChannel(context, liveChannel.id)
                                    navController.popBackStack(navController.graph.startDestinationId, false)
                                },
                                dismissButton = Pair(cancelText) {},
                            )
                        },
                    )
                }
            }

            // Leave channel button (non-owner only)
            if (liveChannel != null && !isOwner) {
                item {
                    VerticalSpace(dp = 16.dp)
                    POutlinedButton(
                        text = leaveChannelText,
                        type = ButtonType.DANGER,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                            .padding(horizontal = 16.dp),
                        onClick = {
                            DialogHelper.showConfirmDialog(
                                title = leaveChannelText,
                                message = leaveChannelWarningText,
                                confirmButton = Pair(leaveChannelText) {
                                    chatListVM.leaveChannel(context, liveChannel.id)
                                    navController.popBackStack(navController.graph.startDestinationId, false)
                                },
                                dismissButton = Pair(cancelText) {},
                            )
                        },
                    )
                }
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
                                confirmButton = Pair(deleteDeviceText) {
                                    chatListVM.removePeer(context, peer.id)
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

    // Rename channel dialog
    if (showRenameDialog && liveChannel != null) {
        RenameChannelDialog(
            currentName = liveChannel.name,
            onDismiss = { showRenameDialog = false },
            onConfirm = { newName ->
                showRenameDialog = false
                chatListVM.renameChannel(liveChannel.id, newName)
            },
        )
    }

    // Peer info dialog — shown when tapping a channel member
    selectedMemberPeer?.let { sp ->
        AlertDialog(
            containerColor = MaterialTheme.colorScheme.surface,
            onDismissRequest = { selectedMemberPeer = null },
            confirmButton = {
                Button(onClick = { selectedMemberPeer = null }) {
                    Text(stringResource(R.string.close))
                }
            },
            title = {
                Text(
                    text = sp.name.ifBlank { sp.getBestIp() },
                    style = MaterialTheme.typography.titleLarge,
                )
            },
            text = {
                Column {
                    PDialogListItem(
                        title = stringResource(R.string.peer_id),
                        value = sp.id,
                    )
                    PDialogListItem(
                        title = stringResource(R.string.ip_address),
                        value = sp.getBestIp(),
                    )
                    PDialogListItem(
                        title = stringResource(R.string.port),
                        value = sp.port.toString(),
                    )
                    PDialogListItem(
                        title = stringResource(R.string.device_type),
                        value = DeviceType.fromValue(sp.deviceType).getText(),
                    )
                    val status = sp.getStatusText()
                    if (status.isNotEmpty()) {
                        PDialogListItem(
                            title = stringResource(R.string.status),
                            value = status,
                        )
                    }
                }
            },
        )
    }

    // Manage members dialog — pass liveChannel so currentMembers is always fresh
    if (showMembersDialog && liveChannel != null) {
        ChannelMembersDialog(
            channel = liveChannel,
            pairedPeers = chatListVM.pairedPeers.toList(),
            onAddMember = { peerId ->
                chatListVM.addChannelMember(liveChannel.id, peerId)
            },
            onRemoveMember = { peerId ->
                chatListVM.removeChannelMember(liveChannel.id, peerId)
            },
            onDismiss = { showMembersDialog = false },
        )
    }
}

@Composable
private fun MemberGridItem(
    name: String,
    iconRes: Int,
    onClick: () -> Unit = {},
) {
    Column(
        modifier = Modifier
            .width(68.dp)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = name,
                modifier = Modifier.size(34.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
        }
        VerticalSpace(dp = 4.dp)
        Text(
            text = name,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun AddMemberGridItem(
    onClick: () -> Unit,
) {
    val borderColor = MaterialTheme.colorScheme.outlineVariant
    Column(
        modifier = Modifier
            .width(68.dp)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(10.dp))
                .drawBehind {
                    val strokeWidth = 1.5.dp.toPx()
                    val dashOn = 6.dp.toPx()
                    val dashOff = 4.dp.toPx()
                    val radius = 10.dp.toPx()
                    drawRoundRect(
                        color = borderColor,
                        topLeft = Offset(strokeWidth / 2, strokeWidth / 2),
                        size = Size(size.width - strokeWidth, size.height - strokeWidth),
                        cornerRadius = CornerRadius(radius, radius),
                        style = Stroke(
                            width = strokeWidth,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(dashOn, dashOff), 0f),
                        ),
                    )
                },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.plus),
                contentDescription = stringResource(R.string.manage_members),
                modifier = Modifier.size(26.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
        }
        VerticalSpace(dp = 4.dp)
        Text(
            text = stringResource(R.string.add_member),
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
            maxLines = 1,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
