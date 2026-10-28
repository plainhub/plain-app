package com.ismartcoding.plain.ui.chat

import androidx.compose.foundation.clickable
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.ismartcoding.plain.models.Peer
import com.ismartcoding.plain.features.chat.models.getBestIp
import com.ismartcoding.plain.features.chat.models.getName
import com.ismartcoding.plain.ui.components.ButtonSize
import com.ismartcoding.plain.ui.components.ButtonType
import com.ismartcoding.plain.platform.system.DeviceType
import com.ismartcoding.plain.ui.platform.system.getIcon
import com.ismartcoding.plain.i18n.Res
import com.ismartcoding.plain.i18n.add_member
import com.ismartcoding.plain.i18n.cancel
import com.ismartcoding.plain.i18n.channel_info
import com.ismartcoding.plain.i18n.channel_name
import com.ismartcoding.plain.i18n.clear_messages
import com.ismartcoding.plain.i18n.clear_messages_confirm
import com.ismartcoding.plain.i18n.delete_channel
import com.ismartcoding.plain.i18n.delete_channel_warning
import com.ismartcoding.plain.i18n.invite
import com.ismartcoding.plain.i18n.leave_channel
import com.ismartcoding.plain.i18n.leave_channel_warning
import com.ismartcoding.plain.i18n.members
import com.ismartcoding.plain.i18n.messages_cleared
import com.ismartcoding.plain.lib.extensions.toSortName
import com.ismartcoding.plain.ui.base.BottomSpace
import com.ismartcoding.plain.ui.base.PCard
import com.ismartcoding.plain.ui.base.PListItem
import com.ismartcoding.plain.ui.base.POutlinedButton
import com.ismartcoding.plain.ui.base.PScaffold
import com.ismartcoding.plain.ui.base.PTopAppBar
import com.ismartcoding.plain.ui.base.PSectionTitle
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.components.DialogHelper
import com.ismartcoding.plain.ui.navigation.Routing
import com.ismartcoding.plain.ui.chat.components.ChannelMemberListItem
import com.ismartcoding.plain.ui.chat.components.PeerIconWithStatus
import com.ismartcoding.plain.ui.chat.components.PeerMember
import com.ismartcoding.plain.ui.chat.components.RenameChannelDialog
import com.ismartcoding.plain.ui.theme.PlainTheme
import org.jetbrains.compose.resources.stringResource
import com.ismartcoding.plain.Prefs
import com.ismartcoding.plain.platform.system.rustStartupConfig

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChannelInfoPage(
    navController: NavHostController, chatVM: ChatViewModel, peerVM: PeerViewModel, channelVM: ChannelViewModel,
) {
    val chatTarget = chatVM.target.collectAsState()
    val peers by peerVM.peersMap.collectAsState()
    LaunchedEffect(peerVM) { peerVM.observe() }
    LaunchedEffect(channelVM) { channelVM.observe() }
    val channels = channelVM.channels.collectAsState()
    val liveChannel = channels.value.find { it.id == chatTarget.value.toId }
    val showRenameDialog = remember { mutableStateOf(false) }
    val selectedMemberPeerId = remember { mutableStateOf<String?>(null) }

    val ownerPeerId: String? = remember(liveChannel?.ownerId) {
        if (liveChannel?.ownerId.isNullOrEmpty()) null
        else if (liveChannel.actions.isOwner) Prefs.clientId.value
        else liveChannel.ownerId
    }
    val deviceName by Prefs.deviceName.collectAsState()
    val memberPeers: List<PeerMember> = remember(liveChannel?.members, ownerPeerId, peers, deviceName) {
        liveChannel?.members?.mapNotNull { m ->
            if (m.isMe()) {
                PeerMember(m, deviceName, DeviceType.valueOf(rustStartupConfig.deviceType), isOwner = m.peerId == ownerPeerId)
            } else {
                val peer = peers[m.peerId] ?: return@mapNotNull null
                PeerMember(m, peer.getName(), peer.deviceType, peer, isOwner = m.peerId == ownerPeerId)
            }
        } ?: emptyList()
    }
    LaunchedEffect(memberPeers, selectedMemberPeerId.value) {
        if (memberPeers.none { it.id == selectedMemberPeerId.value }) selectedMemberPeerId.value = null
    }
    val inviteLabel = stringResource(Res.string.invite)

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
    val addablePeers: List<Peer> = if (liveChannel?.actions?.canManageMembers == true) {
        val presentIds = liveChannel.members.map { it.peerId }.toMutableSet().apply {
            ownerPeerId?.let { add(it) }
        }
        peers.values.filter { it.isPaired() && it.id !in presentIds }
            .sortedBy { it.getName().toSortName() }
    } else emptyList()

    PScaffold(topBar = {
        PTopAppBar(
            onNavigateBack = { navController.navigateUp() },
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
                    PCard(modifier = Modifier.padding(horizontal = PlainTheme.PAGE_HORIZONTAL_MARGIN)) {
                        PListItem(
                            modifier = if (liveChannel.actions.canRename) Modifier.clickable { showRenameDialog.value = true } else Modifier,
                            title = stringResource(Res.string.channel_name),
                            value = liveChannel.name,
                            showMore = liveChannel.actions.canRename,
                        )
                    }
                }

                item { VerticalSpace(dp = 16.dp) }
                item { PSectionTitle(title = "${stringResource(Res.string.members)} (${displayMembers.size})") }
                item {
                    PCard(modifier = Modifier.padding(horizontal = PlainTheme.PAGE_HORIZONTAL_MARGIN)) {
                        displayMembers.forEach { pm ->
                            val canManage = liveChannel.actions.canManageMembers && !pm.isSelf && !pm.isOwner
                            val selfPendingInvite = pm.isSelf && pm.isPending()
                            val ownerName = if (liveChannel.actions.isOwner) deviceName else peers[liveChannel.ownerId]?.getName().orEmpty()
                            ChannelMemberListItem(
                                member = pm,
                                onClick = when {
                                    selfPendingInvite -> {
                                        {
                                            navController.navigate(
                                                Routing.ChannelInviteRequest(
                                                    channelId = liveChannel.id,
                                                    channelName = liveChannel.name,
                                                    ownerPeerId = liveChannel.ownerId,
                                                    ownerPeerName = ownerName,
                                                )
                                            )
                                        }
                                    }
                                    !pm.isSelf -> { { selectedMemberPeerId.value = pm.id } }
                                    else -> null
                                },
                                onRemove = if (canManage) {
                                    { channelVM.kickMember(liveChannel.id, pm.id) }
                                } else null,
                                isLoading = pm.id in channelVM.pendingIds,
                            )
                        }
                    }
                }

                if (liveChannel.actions.canManageMembers && addablePeers.isNotEmpty()) {
                    item { VerticalSpace(dp = 16.dp) }
                    item { PSectionTitle(title = stringResource(Res.string.add_member)) }
                    item {
                        PCard(modifier = Modifier.padding(horizontal = PlainTheme.PAGE_HORIZONTAL_MARGIN)) {
                            addablePeers.forEach { peer ->
                                PListItem(
                                    title = peer.getName(),
                                    subtitle = peer.getBestIp(),
                                    start = {
                                        PeerIconWithStatus(
                                            icon = peer.deviceType.getIcon(),
                                            online = null
                                        )
                                    },
                                    action = {
                                        POutlinedButton(
                                            text = inviteLabel,
                                            onClick = { channelVM.inviteMember(liveChannel.id, peer.id) },
                                            buttonSize = ButtonSize.SMALL,
                                            isLoading = peer.id in channelVM.pendingIds,
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
                            confirmButton = Pair(clearMessagesText) { chatVM.clearAllMessages(); navController.navigateUp(); DialogHelper.showSuccess(Res.string.messages_cleared) },
                            dismissButton = Pair(cancelText) {})
                    })
            }
            if (liveChannel != null && liveChannel.actions.canDelete) {
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
                                    channelVM.removeChannel(liveChannel.id)
                                    navController.popBackStack(navController.graph.startDestinationId, false)
                                },
                                dismissButton = Pair(cancelText) {})
                        })
                }
            }
            if (liveChannel != null && liveChannel.actions.canLeave) {
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

    memberPeers.find { it.id == selectedMemberPeerId.value }?.let { sp ->
        MemberInfoDialog(
            peerMember = sp,
            canManageMembers = liveChannel?.actions?.canManageMembers == true,
            liveChannel = liveChannel,
            channelVM = channelVM,
            onDismiss = { selectedMemberPeerId.value = null },
        )
    }
}
