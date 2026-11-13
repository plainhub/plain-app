package com.ismartcoding.plain.ui.chat

import com.ismartcoding.plain.ui.permissions.getText

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.ismartcoding.plain.models.ChatChannel
import com.ismartcoding.plain.features.chat.models.getBestIp
import com.ismartcoding.plain.ui.components.ButtonSize
import com.ismartcoding.plain.ui.components.ButtonType
import com.ismartcoding.plain.ui.platform.system.getText
import com.ismartcoding.plain.i18n.Res
import com.ismartcoding.plain.i18n.close
import com.ismartcoding.plain.i18n.device_type
import com.ismartcoding.plain.i18n.ip_address
import com.ismartcoding.plain.i18n.kick_member
import com.ismartcoding.plain.i18n.peer_id
import com.ismartcoding.plain.i18n.port
import com.ismartcoding.plain.i18n.resend_invite
import com.ismartcoding.plain.i18n.status
import com.ismartcoding.plain.ui.base.PFilledButton
import com.ismartcoding.plain.ui.base.PListItem
import com.ismartcoding.plain.ui.base.PTextButton
import com.ismartcoding.plain.ui.chat.ChannelViewModel
import com.ismartcoding.plain.ui.chat.components.PeerMember
import com.ismartcoding.plain.ui.theme.dialogSheetBackground
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun MemberInfoDialog(
    peerMember: PeerMember,
    canManageMembers: Boolean,
    liveChannel: ChatChannel?,
    channelVM: ChannelViewModel,
    onDismiss: () -> Unit,
) {
    val peer = requireNotNull(peerMember.peer)
    AlertDialog(
        containerColor = MaterialTheme.colorScheme.dialogSheetBackground,
        onDismissRequest = onDismiss,
        confirmButton = {
            PTextButton(
                text = stringResource(Res.string.close),
                buttonSize = ButtonSize.MEDIUM,
                onClick = onDismiss,
            )
        },
        dismissButton = if (canManageMembers && !peerMember.isOwner && !peerMember.isSelf && liveChannel != null) {
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
                        isLoading = peer.id in channelVM.pendingIds,
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
                        isLoading = peer.id in channelVM.pendingIds,
                    )
                }
            }
        } else null,
        title = { Text(text = peer.name, style = MaterialTheme.typography.titleLarge) },
        text = {
            Column {
                PListItem(title = stringResource(Res.string.peer_id), value = peer.id)
                PListItem(title = stringResource(Res.string.ip_address), value = peer.getBestIp())
                PListItem(title = stringResource(Res.string.port), value = peer.port?.toString().orEmpty())
                PListItem(title = stringResource(Res.string.device_type), value = peer.deviceType.getText())
                val status = peer.status.getText()
                if (status.isNotEmpty()) {
                    PListItem(title = stringResource(Res.string.status), value = status)
                }
            }
        },
    )
}
