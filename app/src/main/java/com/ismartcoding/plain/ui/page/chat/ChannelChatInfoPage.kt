package com.ismartcoding.plain.ui.page.chat

import androidx.compose.foundation.clickable
import com.ismartcoding.plain.ui.models.leaveChannel
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ChannelChatInfoPage(
    navController: NavHostController, chatVM: ChatViewModel, peerVM: PeerViewModel, channelVM: ChannelViewModel,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val chatState = chatVM.chatState.collectAsState()
    val channels = channelVM.channels.collectAsStateValue()
    val liveChannel = channels.find { it.id == chatState.value.toId }
    val isOwner = liveChannel?.owner == "me"

    val showRenameDialog = remember { mutableStateOf(false) }
    val showMembersDialog = remember { mutableStateOf(false) }
    val selectedMemberPeer = remember { mutableStateOf<DPeer?>(null) }
    val selectedPendingMemberPeer = remember { mutableStateOf<DPeer?>(null) }

    val memberPeers = produceState(initialValue = emptyList<DPeer>(), key1 = liveChannel?.members) {
        value = withContext(Dispatchers.IO) { liveChannel?.getPeersAsync() ?: return@withContext emptyList() }
    }
    val joinedMemberPeers = memberPeers.value.filter { peer -> liveChannel?.findMember(peer.id)?.isJoined() != false }
    val pendingMemberPeers = memberPeers.value.filter { peer -> liveChannel?.findMember(peer.id)?.isPending() == true }

    val clearMessagesText = stringResource(R.string.clear_messages)
    val clearMessagesConfirmText = stringResource(R.string.clear_messages_confirm)
    val cancelText = stringResource(R.string.cancel)
    val deleteChannelText = stringResource(R.string.delete_channel)
    val deleteChannelWarningText = stringResource(R.string.delete_channel_warning)
    val leaveChannelText = stringResource(R.string.leave_channel)
    val leaveChannelWarningText = stringResource(R.string.leave_channel_warning)

    PScaffold(topBar = {
        PTopAppBar(navController = navController, navigationIcon = { NavigationBackIcon { navController.navigateUp() } }, title = stringResource(R.string.chat_info))
    }) { paddingValues ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(top = paddingValues.calculateTopPadding())) {
            if (liveChannel != null) {
                item { Subtitle(text = "${stringResource(R.string.members)} (${joinedMemberPeers.size})") }
                item {
                    FlowRow(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        joinedMemberPeers.forEach { MemberGridItem(name = it.name.ifBlank { it.getBestIp() }, iconRes = DeviceType.fromValue(it.deviceType).getIcon(), onClick = { selectedMemberPeer.value = it }) }
                        if (isOwner) AddMemberGridItem(onClick = { showMembersDialog.value = true })
                    }
                }
                if (isOwner && pendingMemberPeers.isNotEmpty()) {
                    item { VerticalSpace(dp = 16.dp) }
                    item { Subtitle(text = "${stringResource(R.string.pending_members)} (${pendingMemberPeers.size})") }
                    item {
                        FlowRow(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            pendingMemberPeers.forEach { MemberGridItem(name = it.name.ifBlank { it.getBestIp() }, iconRes = DeviceType.fromValue(it.deviceType).getIcon(), onClick = { selectedPendingMemberPeer.value = it }) }
                        }
                    }
                }
                item { VerticalSpace(dp = 16.dp) }
                item { PCard { PListItem(modifier = if (isOwner) Modifier.clickable { showRenameDialog.value = true } else Modifier, title = stringResource(R.string.channel_name), value = liveChannel.name, showMore = isOwner) } }
            }
            item { VerticalSpace(dp = 24.dp) }
            item {
                POutlinedButton(text = clearMessagesText, type = ButtonType.DANGER, modifier = Modifier.fillMaxWidth().height(40.dp).padding(horizontal = 16.dp), onClick = {
                    DialogHelper.showConfirmDialog(title = clearMessagesText, message = clearMessagesConfirmText, confirmButton = Pair(clearMessagesText) { chatVM.clearAllMessages(context); navController.navigateUp(); DialogHelper.showSuccess(R.string.messages_cleared) }, dismissButton = Pair(cancelText) {})
                })
            }
            if (liveChannel != null && isOwner) {
                item { VerticalSpace(dp = 16.dp); POutlinedButton(text = deleteChannelText, type = ButtonType.DANGER, modifier = Modifier.fillMaxWidth().height(40.dp).padding(horizontal = 16.dp), onClick = {
                    DialogHelper.showConfirmDialog(title = deleteChannelText, message = deleteChannelWarningText, confirmButton = Pair(deleteChannelText) { channelVM.removeChannel(context, liveChannel.id); navController.popBackStack(navController.graph.startDestinationId, false) }, dismissButton = Pair(cancelText) {})
                }) }
            }
            if (liveChannel != null && !isOwner && liveChannel.status == DChatChannel.STATUS_JOINED) {
                item { VerticalSpace(dp = 16.dp); POutlinedButton(text = leaveChannelText, type = ButtonType.DANGER, modifier = Modifier.fillMaxWidth().height(40.dp).padding(horizontal = 16.dp), onClick = {
                    DialogHelper.showConfirmDialog(title = leaveChannelText, message = leaveChannelWarningText, confirmButton = Pair(leaveChannelText) { channelVM.leaveChannel(context, liveChannel.id); navController.navigateUp() }, dismissButton = Pair(cancelText) {})
                }) }
            }
            if (liveChannel != null && !isOwner && (liveChannel.status == DChatChannel.STATUS_LEFT || liveChannel.status == DChatChannel.STATUS_KICKED)) {
                item { VerticalSpace(dp = 16.dp); POutlinedButton(text = deleteChannelText, type = ButtonType.DANGER, modifier = Modifier.fillMaxWidth().height(40.dp).padding(horizontal = 16.dp), onClick = {
                    DialogHelper.showConfirmDialog(title = deleteChannelText, message = deleteChannelWarningText, confirmButton = Pair(deleteChannelText) { channelVM.removeChannel(context, liveChannel.id); navController.popBackStack(navController.graph.startDestinationId, false) }, dismissButton = Pair(cancelText) {})
                }) }
            }
            item { BottomSpace(paddingValues) }
        }
    }

    ChannelChatInfoDialogs(
        liveChannel = liveChannel, isOwner = isOwner,
        showRenameDialog = showRenameDialog.value, onDismissRename = { showRenameDialog.value = false },
        channelVM = channelVM, selectedMemberPeer = selectedMemberPeer, selectedPendingMemberPeer = selectedPendingMemberPeer,
        showMembersDialog = showMembersDialog.value, onDismissMembers = { showMembersDialog.value = false },
        pairedPeers = peerVM.pairedPeers.toList(),
    )
}
