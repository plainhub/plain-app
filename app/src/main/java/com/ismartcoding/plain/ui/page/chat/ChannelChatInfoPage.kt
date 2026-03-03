package com.ismartcoding.plain.ui.page.chat

import androidx.compose.foundation.background
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
import com.ismartcoding.plain.ui.extensions.collectAsStateValue
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
import com.ismartcoding.plain.db.DChatChannel
import com.ismartcoding.plain.db.DPeer
import com.ismartcoding.plain.enums.ButtonType
import com.ismartcoding.plain.enums.DeviceType
import com.ismartcoding.plain.ui.base.BottomSpace
import com.ismartcoding.plain.ui.base.NavigationBackIcon
import com.ismartcoding.plain.ui.base.PCard
import com.ismartcoding.plain.ui.base.Subtitle
import com.ismartcoding.plain.ui.base.PDialogListItem
import com.ismartcoding.plain.ui.base.PListItem
import com.ismartcoding.plain.ui.base.POutlinedButton
import com.ismartcoding.plain.ui.base.PScaffold
import com.ismartcoding.plain.ui.base.PTopAppBar
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.helpers.DialogHelper
import com.ismartcoding.plain.ui.models.ChannelViewModel
import com.ismartcoding.plain.ui.models.ChatViewModel
import com.ismartcoding.plain.ui.models.PeerViewModel
import com.ismartcoding.plain.ui.page.chat.components.ChannelMembersDialog
import com.ismartcoding.plain.ui.page.chat.components.RenameChannelDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ChannelChatInfoPage(
    navController: NavHostController,
    chatVM: ChatViewModel,
    peerVM: PeerViewModel,
    channelVM: ChannelViewModel,
) {
    val context = LocalContext.current
    val chatState = chatVM.chatState.collectAsState()
    val channels = channelVM.channels.collectAsStateValue()

    // liveChannel comes directly from ChannelViewModel's channels StateFlow — always fresh
    val liveChannel = channels.find { it.id == chatState.value.toId }
    val isOwner = liveChannel?.owner == "me"

    var showRenameDialog by remember { mutableStateOf(false) }
    var showMembersDialog by remember { mutableStateOf(false) }
    var selectedMemberPeer by remember { mutableStateOf<DPeer?>(null) }
    var selectedPendingMemberPeer by remember { mutableStateOf<DPeer?>(null) }

    // Resolve member DPeer objects from DB for showing device icons.
    // Key on liveChannel.members so the grid updates immediately after mutations.
    val memberPeers = produceState(initialValue = emptyList<DPeer>(), key1 = liveChannel?.members) {
        value = withContext(Dispatchers.IO) {
            liveChannel?.getPeersAsync() ?: return@withContext emptyList()
        }
    }

    // Split into joined vs pending based on the channel's member status list
    val joinedMemberPeers = memberPeers.value.filter { peer ->
        liveChannel?.findMember(peer.id)?.isJoined() != false
    }
    val pendingMemberPeers = memberPeers.value.filter { peer ->
        liveChannel?.findMember(peer.id)?.isPending() == true
    }

    val clearMessagesText = stringResource(R.string.clear_messages)
    val clearMessagesConfirmText = stringResource(R.string.clear_messages_confirm)
    val cancelText = stringResource(R.string.cancel)
    val deleteChannelText = stringResource(R.string.delete_channel)
    val deleteChannelWarningText = stringResource(R.string.delete_channel_warning)
    val leaveChannelText = stringResource(R.string.leave_channel)
    val leaveChannelWarningText = stringResource(R.string.leave_channel_warning)
    val kickText = stringResource(R.string.kick_member)

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
            if (liveChannel != null) {
                // Joined members header
                item {
                    Subtitle(text = "${stringResource(R.string.members)} (${joinedMemberPeers.size})")
                }

                // Joined members + "+" add button
                item {
                    FlowRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        joinedMemberPeers.forEach { memberPeer ->
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

                // Pending members section — owner-only, only shown when there are pending invites
                if (isOwner && pendingMemberPeers.isNotEmpty()) {
                    item { VerticalSpace(dp = 16.dp) }
                    item {
                        Subtitle(text = "${stringResource(R.string.pending_members)} (${pendingMemberPeers.size})")
                    }
                    item {
                        FlowRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            pendingMemberPeers.forEach { memberPeer ->
                                MemberGridItem(
                                    name = memberPeer.name.ifBlank { memberPeer.getBestIp() },
                                    iconRes = DeviceType.fromValue(memberPeer.deviceType).getIcon(),
                                    onClick = { selectedPendingMemberPeer = memberPeer },
                                )
                            }
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
                                    channelVM.removeChannel(context, liveChannel.id)
                                    navController.popBackStack(navController.graph.startDestinationId, false)
                                },
                                dismissButton = Pair(cancelText) {},
                            )
                        },
                    )
                }
            }

            // Leave channel button (non-owner, still joined)
            if (liveChannel != null && !isOwner && liveChannel.status == DChatChannel.STATUS_JOINED) {
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
                                    channelVM.leaveChannel(context, liveChannel.id)
                                    navController.navigateUp()
                                },
                                dismissButton = Pair(cancelText) {},
                            )
                        },
                    )
                }
            }

            // Delete channel button (non-owner who has left or been kicked)
            if (liveChannel != null && !isOwner &&
                (liveChannel.status == DChatChannel.STATUS_LEFT || liveChannel.status == DChatChannel.STATUS_KICKED)
            ) {
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
                                    channelVM.removeChannel(context, liveChannel.id)
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
                channelVM.renameChannel(liveChannel.id, newName)
            },
        )
    }

    // Member info dialog — shown when tapping a joined channel member
    // Shows "Kick" button (red, dismissButton slot) for the owner — not for self
    selectedMemberPeer?.let { sp ->
        val isSelf = sp.id == liveChannel?.owner || sp.id == com.ismartcoding.plain.TempData.clientId
        AlertDialog(
            containerColor = MaterialTheme.colorScheme.surface,
            onDismissRequest = { selectedMemberPeer = null },
            confirmButton = {
                Button(onClick = { selectedMemberPeer = null }) {
                    Text(stringResource(R.string.close))
                }
            },
            dismissButton = if (isOwner && !isSelf && liveChannel != null) {
                {
                    Button(
                        onClick = {
                            channelVM.removeChannelMember(liveChannel.id, sp.id)
                            selectedMemberPeer = null
                        },
                        colors = ButtonType.DANGER.getColors(),
                    ) {
                        Text(kickText)
                    }
                }
            } else null,
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

    // Pending member action dialog — shown when tapping a pending channel member
    selectedPendingMemberPeer?.let { sp ->
        if (liveChannel != null) {
            AlertDialog(
                containerColor = MaterialTheme.colorScheme.surface,
                onDismissRequest = { selectedPendingMemberPeer = null },
                confirmButton = {
                    Button(onClick = { selectedPendingMemberPeer = null }) {
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
                            modifier = Modifier.clickable {
                                channelVM.resendInvite(liveChannel.id, sp.id)
                                selectedPendingMemberPeer = null
                            },
                            title = stringResource(R.string.resend_invite),
                        )
                        PDialogListItem(
                            modifier = Modifier.clickable {
                                channelVM.removeChannelMember(liveChannel.id, sp.id)
                                selectedPendingMemberPeer = null
                            },
                            title = stringResource(R.string.remove_member),
                        )
                    }
                },
            )
        }
    }

    // Manage members dialog — pass liveChannel so currentMembers is always fresh
    if (showMembersDialog && liveChannel != null) {
        ChannelMembersDialog(
            channel = liveChannel,
            pairedPeers = peerVM.pairedPeers.toList(),
            onAddMember = { peerId ->
                channelVM.addChannelMember(liveChannel.id, peerId)
            },
            onRemoveMember = { peerId ->
                channelVM.removeChannelMember(liveChannel.id, peerId)
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
