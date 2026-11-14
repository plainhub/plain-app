package com.ismartcoding.plain.ui.chat.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.platform.testTag
import com.ismartcoding.plain.ui.nearby.PairingStep
import com.ismartcoding.plain.features.nearby.PairingMode
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
import com.ismartcoding.plain.ui.components.ConfirmDialogCheckbox
import com.ismartcoding.plain.ui.components.DialogHelper
import com.ismartcoding.plain.ui.components.PIconButton
import com.ismartcoding.plain.ui.nearby.NearbyItemStatus
import com.ismartcoding.plain.ui.nearby.NearbyViewModel
import com.ismartcoding.plain.ui.nearby.PairPeerDialog
import com.ismartcoding.plain.ui.platform.system.getIcon
import com.ismartcoding.plain.ui.theme.PlainTheme
import com.ismartcoding.plain.ui.resources.Res as UiRes
import com.ismartcoding.plain.ui.resources.ellipsis_vertical as ui_drawable_ellipsis_vertical
import org.jetbrains.compose.resources.stringResource

@Composable
fun NearbyPairedDeviceItem(peer: Peer, device: NearbyDevice?) {
    var showPair by remember(peer.id) { mutableStateOf(false) }
    var showMenu by remember(peer.id) { mutableStateOf(false) }
    val status = NearbyViewModel.getStatus(peer.id, true)
    val active = NearbyViewModel.pairingViews.values.any {
        it.peerId == peer.id && it.step !in setOf(PairingStep.SUCCEEDED, PairingStep.FAILED, PairingStep.CANCELED)
    }
    val canceling = active || status == NearbyItemStatus.PAIRING
    val selected = MainViewModel.selectedDevice.value?.peerId == peer.id
    val unpairText = stringResource(Res.string.unpair)
    val unpairMessage = stringResource(Res.string.confirm_unpair_device, peer.name)
    val deleteChatsText = stringResource(Res.string.delete_chat_history)
    val cancelText = stringResource(Res.string.cancel)
    if (showPair && device != null) PairPeerDialog(device, initialMode = PairingMode.MANAGE) { showPair = false }
    PListItem(
        modifier = PlainTheme.getCardModifier().testTag("nearby-device:${peer.id}"),
        title = peer.name,
        subtitle = listOfNotNull(
            peer.ip,
            if (device?.remoteAccess == false) stringResource(Res.string.nearby_enable_remote_access) else null,
        ).filter { it.isNotEmpty() }.joinToString(" · "),
        titleTrailing = {
            if (selected) Text(stringResource(Res.string.nearby_current_device), color = MaterialTheme.colorScheme.primary)
        },
        start = { PeerIconWithStatus(peer.deviceType.getIcon(), peer.online) },
        action = {
            Row {
                if (peer.token.isNotEmpty()) {
                    if (!selected) POutlinedButton(
                        text = stringResource(Res.string.nearby_switch_device),
                        buttonSize = ButtonSize.SMALL,
                        enabled = peer.online && device?.remoteAccess != false,
                        onClick = { MainViewModel.selectDevice(DeviceIdentity(peer.id, peer.publicKey)) },
                    )
                }
                Box {
                    PIconButton(
                        UiRes.drawable.ui_drawable_ellipsis_vertical,
                        contentDescription = stringResource(Res.string.more),
                        click = { showMenu = true },
                    )
                    PDropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        if (peer.token.isEmpty()) PDropdownMenuItem(
                            text = { Text(stringResource(if (canceling) Res.string.cancel else Res.string.pair_full_control)) },
                            enabled = status != NearbyItemStatus.STARTING && status != NearbyItemStatus.UNPAIRING &&
                                (canceling || (peer.online && device?.remoteAccess == true)),
                            onClick = {
                                showMenu = false
                                if (canceling) NearbyViewModel.cancelPairing(peer.id) else showPair = true
                            },
                        )
                        PDropdownMenuItem(text = { Text(unpairText) }, onClick = {
                            showMenu = false
                            val checkbox = ConfirmDialogCheckbox(deleteChatsText)
                            DialogHelper.showConfirmDialog(
                                title = unpairText,
                                message = unpairMessage,
                                confirmButton = Pair(unpairText) { NearbyViewModel.unpairPeer(peer.id, checkbox.checked) },
                                dismissButton = Pair(cancelText) {},
                                danger = true,
                                checkbox = checkbox,
                            )
                        })
                    }
                }
            }
        },
    )
}

