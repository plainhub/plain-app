package com.ismartcoding.plain.ui.chat.components

import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.ismartcoding.plain.features.chat.models.getBestIp
import com.ismartcoding.plain.ui.components.BadgeType
import com.ismartcoding.plain.ui.components.ButtonSize
import com.ismartcoding.plain.ui.components.ButtonType
import com.ismartcoding.plain.ui.platform.system.getIcon
import com.ismartcoding.plain.i18n.Res
import com.ismartcoding.plain.i18n.cancel
import com.ismartcoding.plain.i18n.channel_creator
import com.ismartcoding.plain.i18n.pending
import com.ismartcoding.plain.i18n.this_device
import com.ismartcoding.plain.ui.base.PListItem
import com.ismartcoding.plain.ui.base.POutlinedButton
import com.ismartcoding.plain.ui.base.PStatusBadge
import org.jetbrains.compose.resources.stringResource

@Composable
fun ChannelMemberListItem(
    member: PeerMember,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    onRemove: (() -> Unit)? = null,
    isLoading: Boolean = false,
) {
    val subtitle = if (member.isSelf) stringResource(Res.string.this_device) else member.peer?.getBestIp().orEmpty()
    PListItem(
        modifier = if (onClick != null) modifier.clickable { onClick() } else modifier,
        title = member.displayName(),
        subtitle = subtitle,
        start = {
            PeerIconWithStatus(
                icon = member.deviceType.getIcon(),
                online = null
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
