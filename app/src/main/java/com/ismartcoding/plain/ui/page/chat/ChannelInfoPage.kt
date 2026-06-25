package com.ismartcoding.plain.ui.page.chat

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.ismartcoding.plain.lib.extensions.toSortName
import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.chat.channel.ChannelCacher
import com.ismartcoding.plain.chat.peer.PeerCacher
import com.ismartcoding.plain.db.DChatChannel
import com.ismartcoding.plain.db.DPeer
import com.ismartcoding.plain.db.getBestIp
import com.ismartcoding.plain.db.getName
import com.ismartcoding.plain.db.getOwner
import com.ismartcoding.plain.db.mePeer
import com.ismartcoding.plain.enums.ButtonSize
import com.ismartcoding.plain.enums.ButtonType
import com.ismartcoding.plain.enums.DeviceType
import com.ismartcoding.plain.i18n.Res
import com.ismartcoding.plain.i18n.add_member
import com.ismartcoding.plain.i18n.cancel
import com.ismartcoding.plain.i18n.channel_info
import com.ismartcoding.plain.i18n.channel_name
import com.ismartcoding.plain.i18n.clear_messages
import com.ismartcoding.plain.i18n.clear_messages_confirm
import com.ismartcoding.plain.i18n.close
import com.ismartcoding.plain.i18n.delete_channel
import com.ismartcoding.plain.i18n.delete_channel_warning
import com.ismartcoding.plain.i18n.device_type
import com.ismartcoding.plain.i18n.invite
import com.ismartcoding.plain.i18n.ip_address
import com.ismartcoding.plain.i18n.kick_member
import com.ismartcoding.plain.i18n.leave_channel
import com.ismartcoding.plain.i18n.leave_channel_warning
import com.ismartcoding.plain.i18n.members
import com.ismartcoding.plain.i18n.messages_cleared
import com.ismartcoding.plain.i18n.peer_id
import com.ismartcoding.plain.i18n.port
import com.ismartcoding.plain.i18n.resend_invite
import com.ismartcoding.plain.i18n.status
import com.ismartcoding.plain.ui.base.BottomSpace
import com.ismartcoding.plain.ui.base.PCard
import com.ismartcoding.plain.ui.base.PDialogListItem
import com.ismartcoding.plain.ui.base.PFilledButton
import com.ismartcoding.plain.ui.base.PListItem
import com.ismartcoding.plain.ui.base.POutlinedButton
import com.ismartcoding.plain.ui.base.PScaffold
import com.ismartcoding.plain.ui.base.PTopAppBar
import com.ismartcoding.plain.ui.base.Subtitle
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.helpers.DialogHelper
import com.ismartcoding.plain.ui.models.ChannelViewModel
import com.ismartcoding.plain.ui.models.ChatViewModel
import com.ismartcoding.plain.ui.models.PeerViewModel
import com.ismartcoding.plain.ui.nav.Routing
import com.ismartcoding.plain.ui.page.chat.components.ChannelMemberListItem
import com.ismartcoding.plain.ui.page.chat.components.PeerIconWithStatus
import com.ismartcoding.plain.ui.page.chat.components.PeerMember
import com.ismartcoding.plain.ui.page.chat.components.RenameChannelDialog
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChannelInfoPage(
    navController: NavHostController, chatVM: ChatViewModel, peerVM: PeerViewModel, channelVM: ChannelViewModel,
) {
    val context = LocalContext.current
    val chatTarget = chatVM.target.collectAsState()
    val liveChannel = ChannelCacher.getChannel(chatTarget.value.toId)
    val ownedByMe = liveChannel?.isOwnedByMe() == true
    val showRenameDialog = remember { mutableStateOf(false) }
    val selectedMemberPeer = remember { mutableStateOf<PeerMember?>(null) }

    val ownerPeerId: String? = remember(liveChannel?.owner) {
        if (liveChannel?.owner.isNullOrEmpty()) null
        else if (liveChannel.isOwnedByMe()) TempData.clientId
        else liveChannel.owner
    }
    val memberPeers: List<PeerMember> = remember(liveChannel?.members, ownerPeerId) {
        liveChannel?.members?.mapNotNull { m ->
            val peer = if (m.isMe()) mePeer() else PeerCacher.getPeer(m.id) ?: return@mapNotNull null
            PeerMember(peer, m, isSelf = m.isMe(), isOwner = m.id == ownerPeerId)
        } ?: emptyList()
    }
    val inviteLabel = stringResource(Res.string.invite)

    // Members shown in the list: joined members first (owner pinned to top, then A-Z),
    // followed by pending members sorted A-Z by display name.
    val joinedMembers: List<PeerMember> = memberPeers.filter { it.isJoined() }
    val pendingMembers: List<PeerMember> = memberPeers.filter { it.isPending() }
    val displayMembers: List<PeerMember> = buildList {
        val ownerMember = joinedMembers.find { it.isOwner }
        if (ownerMember != null) {
            add(ownerMember)
            addAll(
                joinedMembers
                    .filter { !it.isOwner }
                    .sortedBy { it.displayName() },
            )
        } else {
            addAll(joinedMembers.sortedBy { it.displayName() })
        }
        addAll(pendingMembers.sortedBy { it.displayName() })
    }
    // Paired peers that are not yet members of this channel (and not the owner). A-Z.
    val addablePeers: List<DPeer> = if (ownedByMe) {
        val presentIds = liveChannel.members.map { it.id }.toMutableSet().apply {
            ownerPeerId?.let { add(it) }
        }
        PeerCacher.pairedPeers.collectAsState().value
            .filter { it.id !in presentIds }
            .sortedBy { it.getName().toSortName() }
    } else emptyList()

    PScaffold(topBar = {
        PTopAppBar(
            navController = navController,
            title = stringResource(Res.string.channel_info)
        )
    }) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = paddingValues.calculateTopPadding())
        ) {
            if (liveChannel != null) {
                item { VerticalSpace(dp = 16.dp) }
                item {
                    PCard {
                        PListItem(
                            modifier = if (ownedByMe) Modifier.clickable { showRenameDialog.value = true } else Modifier,
                            title = stringResource(Res.string.channel_name),
                            value = liveChannel.name,
                            showMore = ownedByMe,
                        )
                    }
                }

                item { VerticalSpace(dp = 16.dp) }
                item { Subtitle(text = "${stringResource(Res.string.members)} (${displayMembers.size})") }
                item {
                    PCard {
                        displayMembers.forEach { pm ->
                            val canManage = ownedByMe && !pm.isSelf && !pm.isOwner
                            val selfPendingInvite = pm.isSelf && pm.isPending()
                            val ownerPeer = if (selfPendingInvite) liveChannel.getOwner() else null
                            ChannelMemberListItem(
                                member = pm,
                                onClick = when {
                                    selfPendingInvite -> {
                                        {
                                            navController.navigate(
                                                Routing.ChannelInviteRequest(
                                                    channelId = liveChannel.id,
                                                    channelName = liveChannel.name,
                                                    ownerPeerId = liveChannel.owner,
                                                    ownerPeerName = ownerPeer?.getName() ?: "",
                                                )
                                            )
                                        }
                                    }
                                    !pm.isSelf -> { { selectedMemberPeer.value = pm } }
                                    else -> null
                                },
                                onRemove = if (canManage) {
                                    { channelVM.kickMember(liveChannel.id, pm.peer.id) }
                                } else null,
                                isLoading = pm.peer.id in channelVM.kickingIds,
                            )
                        }
                    }
                }

                if (ownedByMe && addablePeers.isNotEmpty()) {
                    item { VerticalSpace(dp = 16.dp) }
                    item { Subtitle(text = stringResource(Res.string.add_member)) }
                    item {
                        PCard {
                            addablePeers.forEach { peer ->
                                PListItem(
                                    title = peer.getName(),
                                    subtitle = peer.getBestIp(),
                                    start = {
                                        PeerIconWithStatus(
                                            icon = DeviceType.fromValue(peer.deviceType).getIcon(),
                                            title = peer.getName(),
                                            online = null
                                        )
                                    },
                                    action = {
                                        POutlinedButton(
                                            text = inviteLabel,
                                            onClick = { channelVM.inviteMember(liveChannel.id, peer.id) },
                                            buttonSize = ButtonSize.SMALL,
                                            isLoading = peer.id in channelVM.invitingIds,
                                        )
                                    },
                                )
                            }
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
                    text = clearMessagesText, type = ButtonType.DANGER, modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp), onClick = {
                        DialogHelper.showConfirmDialog(
                            title = clearMessagesText,
                            message = clearMessagesConfirmText,
                            confirmButton = Pair(clearMessagesText) { chatVM.clearAllMessages(context); navController.navigateUp(); DialogHelper.showSuccess(Res.string.messages_cleared) },
                            dismissButton = Pair(cancelText) {})
                    })
            }
            if (liveChannel != null && ownedByMe) {
                item {
                    val deleteChannelText = stringResource(Res.string.delete_channel);
                    val deleteChannelWarningText = stringResource(Res.string.delete_channel_warning);
                    val cancelText = stringResource(Res.string.cancel); VerticalSpace(dp = 16.dp)
                    POutlinedButton(
                        text = deleteChannelText,
                        type = ButtonType.DANGER,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        onClick = {
                            DialogHelper.showConfirmDialog(
                                title = deleteChannelText,
                                message = deleteChannelWarningText,
                                confirmButton = Pair(deleteChannelText) {
                                    channelVM.removeChannel(context, liveChannel.id)
                                    navController.popBackStack(navController.graph.startDestinationId, false)
                                },
                                dismissButton = Pair(cancelText) {})
                        })
                }
            }
            if (liveChannel != null && liveChannel.canLeave()) {
                item {
                    val leaveChannelText = stringResource(Res.string.leave_channel)
                    val leaveChannelWarningText = stringResource(Res.string.leave_channel_warning)
                    val cancelText = stringResource(Res.string.cancel); VerticalSpace(dp = 16.dp)
                    POutlinedButton(
                        text = leaveChannelText,
                        type = ButtonType.DANGER,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        onClick = {
                            DialogHelper.showConfirmDialog(
                                title = leaveChannelText,
                                message = leaveChannelWarningText,
                                confirmButton = Pair(leaveChannelText) {
                                    channelVM.leaveChannel(liveChannel.id)
                                    navController.navigateUp()
                                },
                                dismissButton = Pair(cancelText) {})
                        })
                }
            }
            if (liveChannel != null && !ownedByMe && !liveChannel.isJoined()) {
                item {
                    val deleteChannelText = stringResource(Res.string.delete_channel);
                    val deleteChannelWarningText = stringResource(Res.string.delete_channel_warning);
                    val cancelText = stringResource(Res.string.cancel)
                    VerticalSpace(dp = 16.dp)
                    POutlinedButton(
                        text = deleteChannelText,
                        type = ButtonType.DANGER,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        onClick = {
                            DialogHelper.showConfirmDialog(
                                title = deleteChannelText,
                                message = deleteChannelWarningText,
                                confirmButton = Pair(deleteChannelText) {
                                    channelVM.removeChannel(context, liveChannel.id)
                                    navController.popBackStack(navController.graph.startDestinationId, false)
                                },
                                dismissButton = Pair(cancelText) {})
                        })
                }
            }
            item { BottomSpace(paddingValues) }
        }
    }

    if (showRenameDialog.value && liveChannel != null) {
        RenameChannelDialog(
            currentName = liveChannel.name,
            onDismiss = {
                showRenameDialog.value = false
            },
            onConfirm = { newName ->
                showRenameDialog.value = false
                channelVM.renameChannel(liveChannel.id, newName)
            },
        )
    }

    selectedMemberPeer.value?.let { sp ->
        MemberInfoDialog(
            peerMember = sp,
            ownedByMe,
            liveChannel = liveChannel,
            channelVM = channelVM,
            onDismiss = { selectedMemberPeer.value = null },
        )
    }
}

@Composable
private fun MemberInfoDialog(
    peerMember: PeerMember,
    ownedByMe: Boolean,
    liveChannel: DChatChannel?,
    channelVM: ChannelViewModel,
    onDismiss: () -> Unit,
) {
    val peer = peerMember.peer
    AlertDialog(
        containerColor = MaterialTheme.colorScheme.surface,
        onDismissRequest = onDismiss,
        confirmButton = {
            PFilledButton(
                text = stringResource(Res.string.close),
                buttonSize = ButtonSize.MEDIUM,
                onClick = onDismiss,
            )
        },
        dismissButton = if (ownedByMe && !peerMember.isOwner && !peerMember.isSelf && liveChannel != null) {
            {
                if (peerMember.isPending()) {
                    PFilledButton(
                        text = stringResource(Res.string.resend_invite),
                        buttonSize = ButtonSize.MEDIUM,
                        onClick = {
                            channelVM.resendInvite(liveChannel.id, peer.id)
                            onDismiss()
                        },
                        type = ButtonType.TERTIARY,
                        isLoading = peer.id in channelVM.invitingIds,
                    )
                } else {
                    PFilledButton(
                        text = stringResource(Res.string.kick_member),
                        buttonSize = ButtonSize.MEDIUM,
                        onClick = {
                            channelVM.kickMember(liveChannel.id, peer.id)
                            onDismiss()
                        },
                        type = ButtonType.DANGER,
                        isLoading = peer.id in channelVM.kickingIds,
                    )
                }
            }
        } else null,
        title = { Text(text = peer.getName(), style = MaterialTheme.typography.titleLarge) },
        text = {
            Column {
                PDialogListItem(title = stringResource(Res.string.peer_id), value = peer.id)
                PDialogListItem(title = stringResource(Res.string.ip_address), value = peer.getBestIp())
                PDialogListItem(title = stringResource(Res.string.port), value = peer.port.toString())
                PDialogListItem(title = stringResource(Res.string.device_type), value = DeviceType.fromValue(peer.deviceType).getText())
                val status = peer.getStatusText()
                if (status.isNotEmpty()) {
                    PDialogListItem(title = stringResource(Res.string.status), value = status)
                }
            }
        },
    )
}


