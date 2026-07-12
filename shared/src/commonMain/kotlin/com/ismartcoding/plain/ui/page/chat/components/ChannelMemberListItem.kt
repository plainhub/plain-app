package com.ismartcoding.plain.ui.page.chat.components

import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.ismartcoding.plain.db.ChannelMember
import com.ismartcoding.plain.db.DPeer
import com.ismartcoding.plain.db.getBestIp
import com.ismartcoding.plain.db.getName
import com.ismartcoding.plain.enums.BadgeType
import com.ismartcoding.plain.enums.ButtonSize
import com.ismartcoding.plain.enums.ButtonType
import com.ismartcoding.plain.enums.DeviceType
import com.ismartcoding.plain.i18n.Res
import com.ismartcoding.plain.i18n.cancel
import com.ismartcoding.plain.i18n.channel_creator
import com.ismartcoding.plain.i18n.pending
import com.ismartcoding.plain.i18n.this_device
import com.ismartcoding.plain.ui.base.PListItem
import com.ismartcoding.plain.ui.base.POutlinedButton
import com.ismartcoding.plain.ui.base.PStatusBadge
import org.jetbrains.compose.resources.stringResource

data class PeerMember(
    val peer: DPeer,
    val member: ChannelMember,
    val isSelf: Boolean = false,
    val isOwner: Boolean = false,
) {
    fun isJoined(): Boolean = member.isJoined()
    fun isPending(): Boolean = member.isPending()
    fun displayName(): String = peer.getName()
}

@Composable
fun ChannelMemberListItem(
    member: PeerMember,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    onRemove: (() -> Unit)? = null,
    isLoading: Boolean = false,
) {
    val subtitle = if (member.isSelf) stringResource(Res.string.this_device) else member.peer.getBestIp()
    PListItem(
        modifier = if (onClick != null) modifier.clickable { onClick() } else modifier,
        title = member.displayName(),
        subtitle = subtitle,
        start = {
            PeerIconWithStatus(
                icon = DeviceType.fromValue(member.peer.deviceType).getIcon(),
                title = member.displayName(), online = null
            )
        },
        titleSuffix = if (member.isOwner) {
            { PStatusBadge(text = stringResource(Res.string.channel_creator), type = BadgeType.ON) }
        } else if (member.isPending()) {
            { PStatusBadge(text = stringResource(Res.string.pending), type = BadgeType.WARN) }
        } else null,
        action = when {
            member.isPending() && onRemove != null -> {
                {
                    POutlinedButton(
                        text = stringResource(Res.string.cancel),
                        onClick = onRemove,
                        buttonSize = ButtonSize.SMALL,
                        type = ButtonType.DANGER,
                        isLoading = isLoading
                    )
                }
            }

            else -> null
        },
    )
}