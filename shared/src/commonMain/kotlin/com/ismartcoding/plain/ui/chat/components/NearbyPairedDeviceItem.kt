package com.ismartcoding.plain.ui.chat.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.painterResource
import com.ismartcoding.plain.ui.resources.check as ui_drawable_check
import com.ismartcoding.plain.ui.components.ToastManager
import androidx.compose.runtime.*
import androidx.compose.ui.platform.testTag
import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.models.NearbyDevice
import com.ismartcoding.plain.models.Peer
import com.ismartcoding.plain.ui.app.DeviceIdentity
import com.ismartcoding.plain.ui.app.MainViewModel
import com.ismartcoding.plain.ui.base.PDropdownMenu
import com.ismartcoding.plain.ui.base.PDropdownMenuItem
import com.ismartcoding.plain.ui.base.PListItem
import com.ismartcoding.plain.ui.base.POutlinedButton
import com.ismartcoding.plain.ui.components.ButtonSize
import com.ismartcoding.plain.ui.components.DialogHelper
import com.ismartcoding.plain.ui.components.PIconButton
import com.ismartcoding.plain.ui.nearby.NearbyItemStatus
import com.ismartcoding.plain.ui.nearby.NearbyViewModel
import com.ismartcoding.plain.ui.platform.system.getIcon
import com.ismartcoding.plain.ui.theme.PlainTheme
import com.ismartcoding.plain.ui.resources.Res as UiRes
import com.ismartcoding.plain.ui.resources.more_horiz as ui_drawable_more_horiz
import org.jetbrains.compose.resources.stringResource

@Composable
fun NearbyPairedDeviceItem(peer: Peer, device: NearbyDevice?, onPair: (NearbyDevice) -> Unit) {
    var showMenu by remember(peer.id) { mutableStateOf(false) }
    val isUnpairing = NearbyViewModel.itemStatus[peer.id] == NearbyItemStatus.UNPAIRING
    val selected = MainViewModel.selectedDevice.value?.peerId == peer.id
    val unpairText = stringResource(Res.string.unpair)
    val actions = pairedDeviceActions(peer, selected)
    val unpairTitle = stringResource(Res.string.confirm_unpair_device, peer.name)
    val unpairMessage = stringResource(Res.string.unpair_device_hint)
    val cancelText = stringResource(Res.string.cancel)
    PListItem(
        modifier = PlainTheme.getCardModifier().testTag("nearby-device:${peer.id}"),
        title = peer.name,
        subtitle = listOfNotNull(
            peer.ip,
            if (device?.remoteAccess == false) stringResource(Res.string.nearby_enable_remote_access) else null,
        ).filter { it.isNotEmpty() }.joinToString(" · "),
        titleSuffix = if (selected) { { CurrentDeviceIcon() } } else null,
        start = { PeerIconWithStatus(peer.deviceType.getIcon(), peer.online) },
        action = {
            if (actions == PairedDeviceActions.CANCEL) {
                POutlinedButton(
                    text = cancelText,
                    buttonSize = ButtonSize.SMALL,
                    onClick = { NearbyViewModel.cancelPairing(peer.id) },
                )
            } else Row {
                if (actions == PairedDeviceActions.SWITCH_AND_MORE) {
                    val reason = when {
                        device?.remoteAccess == false -> stringResource(Res.string.event_reason_remote_access_disabled)
                        !peer.online -> stringResource(Res.string.offline)
                        else -> null
                    }
                    PIconButton(
                        Res.drawable.arrow_left_right,
                        contentDescription = stringResource(Res.string.nearby_switch_device),
                        click = {
                            if (reason == null) MainViewModel.selectDevice(DeviceIdentity(peer.id, peer.publicKey))
                            else ToastManager.showInfoToast("${peer.name}: $reason")
                        },
                    )
                }
                Box {
                    PIconButton(
                        UiRes.drawable.ui_drawable_more_horiz,
                        contentDescription = stringResource(Res.string.more),
                        click = { showMenu = true },
                    )
                    PDropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        if (peer.token.isEmpty() && !selected) PDropdownMenuItem(
                            text = { Text(stringResource(Res.string.pair_full_control)) },
                            enabled = !isUnpairing && peer.online && device?.remoteAccess == true,
                            onClick = {
                                showMenu = false
                                device?.let(onPair)
                            },
                        )
                        PDropdownMenuItem(
                            text = { Text(unpairText) },
                            onClick = {
                                showMenu = false
                                DialogHelper.showConfirmDialog(
                                    title = unpairTitle,
                                    message = unpairMessage,
                                    confirmButton = Pair(unpairText) { NearbyViewModel.unpairPeer(peer.id) },
                                    dismissButton = Pair(cancelText) {},
                                    danger = true,
                                )
                            },
                        )
                    }
                }
            }
        },
    )
}

internal enum class PairedDeviceActions { CANCEL, MORE, SWITCH_AND_MORE }

@Composable
internal fun CurrentDeviceIcon() {
    Icon(
        painterResource(UiRes.drawable.ui_drawable_check),
        contentDescription = stringResource(Res.string.nearby_current_device),
        modifier = Modifier.size(16.dp),
        tint = MaterialTheme.colorScheme.primary,
    )
}

internal fun pairedDeviceActions(peer: Peer, selected: Boolean): PairedDeviceActions {
    val status = NearbyViewModel.itemStatus[peer.id]
    if (NearbyViewModel.pairingViews[peer.id]?.active == true || status == NearbyItemStatus.STARTING || status == NearbyItemStatus.PAIRING) {
        return PairedDeviceActions.CANCEL
    }
    return if (peer.token.isNotEmpty() && !selected) PairedDeviceActions.SWITCH_AND_MORE else PairedDeviceActions.MORE
}
