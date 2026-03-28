package com.ismartcoding.plain.ui.page.chat

import androidx.compose.foundation.clickable
import com.ismartcoding.plain.ui.models.addChannelMember
import com.ismartcoding.plain.ui.models.removeChannelMember
import com.ismartcoding.plain.ui.models.resendInvite
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.ismartcoding.plain.R
import com.ismartcoding.plain.db.DChatChannel
import com.ismartcoding.plain.db.DPeer
import com.ismartcoding.plain.enums.ButtonType
import com.ismartcoding.plain.enums.DeviceType
import com.ismartcoding.plain.ui.base.PDialogListItem
import com.ismartcoding.plain.ui.models.ChannelViewModel
import com.ismartcoding.plain.ui.page.chat.components.ChannelMembersDialog
import com.ismartcoding.plain.ui.page.chat.components.RenameChannelDialog

@Composable
internal fun ChannelChatInfoDialogs(
    liveChannel: DChatChannel?,
    isOwner: Boolean,
    showRenameDialog: Boolean,
    onDismissRename: () -> Unit,
    channelVM: ChannelViewModel,
    selectedMemberPeer: MutableState<DPeer?>,
    selectedPendingMemberPeer: MutableState<DPeer?>,
    showMembersDialog: Boolean,
    onDismissMembers: () -> Unit,
    pairedPeers: List<DPeer>,
) {
    if (showRenameDialog && liveChannel != null) {
        RenameChannelDialog(
            currentName = liveChannel.name,
            onDismiss = onDismissRename,
            onConfirm = { newName -> onDismissRename(); channelVM.renameChannel(liveChannel.id, newName) },
        )
    }

    selectedMemberPeer.value?.let { sp ->
        val isSelf = sp.id == liveChannel?.owner || sp.id == com.ismartcoding.plain.TempData.clientId
        MemberInfoDialog(
            peer = sp, isOwner = isOwner, isSelf = isSelf, liveChannel = liveChannel,
            channelVM = channelVM, onDismiss = { selectedMemberPeer.value = null },
        )
    }

    selectedPendingMemberPeer.value?.let { sp ->
        if (liveChannel != null) {
            PendingMemberDialog(
                peer = sp, liveChannel = liveChannel, channelVM = channelVM,
                onDismiss = { selectedPendingMemberPeer.value = null },
            )
        }
    }

    if (showMembersDialog && liveChannel != null) {
        ChannelMembersDialog(
            channel = liveChannel, pairedPeers = pairedPeers,
            onAddMember = { peerId -> channelVM.addChannelMember(liveChannel.id, peerId) },
            onRemoveMember = { peerId -> channelVM.removeChannelMember(liveChannel.id, peerId) },
            onDismiss = onDismissMembers,
        )
    }
}

@Composable
private fun MemberInfoDialog(
    peer: DPeer, isOwner: Boolean, isSelf: Boolean,
    liveChannel: DChatChannel?, channelVM: ChannelViewModel, onDismiss: () -> Unit,
) {
    AlertDialog(
        containerColor = MaterialTheme.colorScheme.surface,
        onDismissRequest = onDismiss,
        confirmButton = { Button(onClick = onDismiss) { Text(stringResource(R.string.close)) } },
        dismissButton = if (isOwner && !isSelf && liveChannel != null) {
            {
                Button(
                    onClick = { channelVM.removeChannelMember(liveChannel.id, peer.id); onDismiss() },
                    colors = ButtonType.DANGER.getColors(),
                ) { Text(stringResource(R.string.kick_member)) }
            }
        } else null,
        title = { Text(text = peer.name.ifBlank { peer.getBestIp() }, style = MaterialTheme.typography.titleLarge) },
        text = {
            Column {
                PDialogListItem(title = stringResource(R.string.peer_id), value = peer.id)
                PDialogListItem(title = stringResource(R.string.ip_address), value = peer.getBestIp())
                PDialogListItem(title = stringResource(R.string.port), value = peer.port.toString())
                PDialogListItem(title = stringResource(R.string.device_type), value = DeviceType.fromValue(peer.deviceType).getText())
                val status = peer.getStatusText()
                if (status.isNotEmpty()) {
                    PDialogListItem(title = stringResource(R.string.status), value = status)
                }
            }
        },
    )
}

@Composable
private fun PendingMemberDialog(
    peer: DPeer, liveChannel: DChatChannel, channelVM: ChannelViewModel, onDismiss: () -> Unit,
) {
    AlertDialog(
        containerColor = MaterialTheme.colorScheme.surface,
        onDismissRequest = onDismiss,
        confirmButton = { Button(onClick = onDismiss) { Text(stringResource(R.string.close)) } },
        title = { Text(text = peer.name.ifBlank { peer.getBestIp() }, style = MaterialTheme.typography.titleLarge) },
        text = {
            Column {
                PDialogListItem(
                    modifier = Modifier.clickable { channelVM.resendInvite(liveChannel.id, peer.id); onDismiss() },
                    title = stringResource(R.string.resend_invite),
                )
                PDialogListItem(
                    modifier = Modifier.clickable { channelVM.removeChannelMember(liveChannel.id, peer.id); onDismiss() },
                    title = stringResource(R.string.remove_member),
                )
            }
        },
    )
}
