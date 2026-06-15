package com.ismartcoding.plain.ui.page.chat

import com.ismartcoding.plain.i18n.*

import androidx.compose.foundation.clickable
import com.ismartcoding.plain.ui.models.leaveChannel
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import com.ismartcoding.plain.ui.extensions.collectAsStateValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.db.DPeer
import com.ismartcoding.plain.db.canLeave
import com.ismartcoding.plain.db.getBestIp
import com.ismartcoding.plain.db.getPeersAsync
import com.ismartcoding.plain.db.isJoined
import com.ismartcoding.plain.db.isOwnedByMe
import com.ismartcoding.plain.enums.ButtonType
import com.ismartcoding.plain.enums.DeviceType
import com.ismartcoding.plain.ui.base.BottomSpace
import com.ismartcoding.plain.ui.base.NavigationBackIcon
import com.ismartcoding.plain.ui.base.PCard
import com.ismartcoding.plain.ui.base.Subtitle
import com.ismartcoding.plain.ui.base.PListItem
import com.ismartcoding.plain.ui.base.POutlinedButton
import com.ismartcoding.plain.ui.base.PScaffold
import com.ismartcoding.plain.ui.base.PTopAppBar
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.helpers.DialogHelper
import com.ismartcoding.plain.ui.models.ChannelViewModel
import com.ismartcoding.plain.ui.models.ChatViewModel
import com.ismartcoding.plain.ui.models.PeerViewModel
import com.ismartcoding.plain.ui.models.clearAllMessages
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChannelChatInfoPage(
    navController: NavHostController, chatVM: ChatViewModel, peerVM: PeerViewModel, channelVM: ChannelViewModel,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val chatState = chatVM.chatState.collectAsState()
    val channels = channelVM.channels.collectAsStateValue()
    val liveChannel = channels.find { it.id == chatState.value.target.toId }
    val ownedByMe = liveChannel?.isOwnedByMe() == true

    val showRenameDialog = remember { mutableStateOf(false) }
    val showMembersDialog = remember { mutableStateOf(false) }
    val selectedMemberPeer = remember { mutableStateOf<DPeer?>(null) }
    val selectedPendingMemberPeer = remember { mutableStateOf<DPeer?>(null) }

    val memberPeers = produceState(initialValue = emptyList<DPeer>(), key1 = liveChannel?.members) {
        value = withContext(Dispatchers.IO) { liveChannel?.getPeersAsync() ?: return@withContext emptyList() }
    }
    val joinedMemberPeers = memberPeers.value.filter { peer -> liveChannel?.findMember(peer.id)?.isJoined() != false }
    val pendingMemberPeers = memberPeers.value.filter { peer -> liveChannel?.findMember(peer.id)?.isPending() == true }
    val visibleJoinedMembers = joinedMemberPeers.filter { it.id != TempData.clientId }

    val meLabel = stringResource(Res.string.me)
    val ownerDisplayName: String = liveChannel?.owner?.let { ownerId ->
        if (ownerId == "me" || ownerId == TempData.clientId) {
            TempData.deviceName.value.ifBlank { meLabel }
        } else {
            com.ismartcoding.plain.chat.ChatCacheManager.peerMap[ownerId]?.name?.ifBlank { null }
                ?: memberPeers.value.find { it.id == ownerId }?.name?.ifBlank { null }
                ?: memberPeers.value.find { it.id == ownerId }?.getBestIp()
                ?: ownerId
        }
    } ?: ""

    PScaffold(topBar = {
        PTopAppBar(navController = navController, navigationIcon = { NavigationBackIcon { navController.navigateUp() } }, title = stringResource(Res.string.channel_info))
    }) { paddingValues ->
        LazyColumn(modifier = Modifier
            .fillMaxSize()
            .padding(top = paddingValues.calculateTopPadding())) {
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
                        PListItem(
                            title = stringResource(Res.string.owner),
                            value = ownerDisplayName,
                        )
                    }
                }

                item { VerticalSpace(dp = 16.dp) }
                item { Subtitle(text = "${stringResource(Res.string.members)} (${visibleJoinedMembers.size})") }
                item {
                    PCard {
                        visibleJoinedMembers.forEach { peer ->
                            PListItem(
                                modifier = Modifier.clickable { selectedMemberPeer.value = peer },
                                title = peer.name.ifBlank { peer.getBestIp() },
                                subtitle = DeviceType.fromValue(peer.deviceType).getText(),
                                showMore = true,
                            )
                        }
                        if (ownedByMe) {
                            PListItem(
                                modifier = Modifier.clickable { showMembersDialog.value = true },
                                title = stringResource(Res.string.manage_members),
                                showMore = true,
                            )
                        }
                    }
                }

                if (ownedByMe && pendingMemberPeers.isNotEmpty()) {
                    item { VerticalSpace(dp = 16.dp) }
                    item { Subtitle(text = "${stringResource(Res.string.pending_members)} (${pendingMemberPeers.size})") }
                    item {
                        PCard {
                            pendingMemberPeers.forEach { peer ->
                                PListItem(
                                    modifier = Modifier.clickable { selectedPendingMemberPeer.value = peer },
                                    title = peer.name.ifBlank { peer.getBestIp() },
                                    subtitle = DeviceType.fromValue(peer.deviceType).getText(),
                                    showMore = true,
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
                POutlinedButton(text = clearMessagesText, type = ButtonType.DANGER, modifier = Modifier
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
                    val cancelText = stringResource(Res.string.cancel); VerticalSpace(dp = 16.dp); POutlinedButton(
                    text = deleteChannelText,
                    type = ButtonType.DANGER,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    onClick = {
                        DialogHelper.showConfirmDialog(
                            title = deleteChannelText,
                            message = deleteChannelWarningText,
                            confirmButton = Pair(deleteChannelText) { channelVM.removeChannel(context, liveChannel.id); navController.popBackStack(navController.graph.startDestinationId, false) },
                            dismissButton = Pair(cancelText) {})
                    })
                }
            }
            if (liveChannel != null && liveChannel.canLeave()) {
                item {
                    val leaveChannelText = stringResource(Res.string.leave_channel);
                    val leaveChannelWarningText = stringResource(Res.string.leave_channel_warning);
                    val cancelText = stringResource(Res.string.cancel); VerticalSpace(dp = 16.dp); POutlinedButton(
                    text = leaveChannelText,
                    type = ButtonType.DANGER,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    onClick = {
                        DialogHelper.showConfirmDialog(
                            title = leaveChannelText,
                            message = leaveChannelWarningText,
                            confirmButton = Pair(leaveChannelText) { channelVM.leaveChannel(context, liveChannel.id); navController.navigateUp() },
                            dismissButton = Pair(cancelText) {})
                    })
                }
            }
            if (liveChannel != null && !ownedByMe && !liveChannel.isJoined()) {
                item {
                    val deleteChannelText = stringResource(Res.string.delete_channel);
                    val deleteChannelWarningText = stringResource(Res.string.delete_channel_warning);
                    val cancelText = stringResource(Res.string.cancel); VerticalSpace(dp = 16.dp); POutlinedButton(
                    text = deleteChannelText,
                    type = ButtonType.DANGER,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    onClick = {
                        DialogHelper.showConfirmDialog(
                            title = deleteChannelText,
                            message = deleteChannelWarningText,
                            confirmButton = Pair(deleteChannelText) { channelVM.removeChannel(context, liveChannel.id); navController.popBackStack(navController.graph.startDestinationId, false) },
                            dismissButton = Pair(cancelText) {})
                    })
                }
            }
            item { BottomSpace(paddingValues) }
        }
    }

    ChannelChatInfoDialogs(
        liveChannel = liveChannel, isOwner = ownedByMe,
        showRenameDialog = showRenameDialog.value, onDismissRename = { showRenameDialog.value = false },
        channelVM = channelVM, selectedMemberPeer = selectedMemberPeer, selectedPendingMemberPeer = selectedPendingMemberPeer,
        showMembersDialog = showMembersDialog.value, onDismissMembers = { showMembersDialog.value = false },
        pairedPeers = peerVM.pairedPeers.toList(),
    )
}
