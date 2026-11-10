package com.ismartcoding.plain.ui.chat.components

import androidx.compose.runtime.*
import androidx.compose.ui.platform.testTag
import com.ismartcoding.plain.features.nearby.PairingPhase
import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.models.NearbyDevice
import com.ismartcoding.plain.ui.base.PListItem
import com.ismartcoding.plain.ui.base.POutlinedButton
import com.ismartcoding.plain.ui.components.ButtonSize
import com.ismartcoding.plain.ui.nearby.NearbyItemStatus
import com.ismartcoding.plain.ui.nearby.NearbyViewModel
import com.ismartcoding.plain.ui.nearby.PairPeerDialog
import com.ismartcoding.plain.ui.platform.system.getIcon
import com.ismartcoding.plain.ui.theme.PlainTheme
import org.jetbrains.compose.resources.stringResource

@Composable
fun NearbyDeviceItem(item: NearbyDevice, status: NearbyItemStatus, bestIp: String) {
    var showPair by remember(item.id) { mutableStateOf(false) }
    if (showPair) {
        PairPeerDialog(item) {
            showPair = false
        }
    }
    PListItem(
        modifier = PlainTheme.getCardModifier().testTag("nearby-device:${item.id}"),
        title = item.name,
        subtitle = bestIp,
        start = { PeerIconWithStatus(item.deviceType.getIcon(), null) },
        action = {
            NearbyPairButton(item.id, status, if (item.remoteAccess == true) Res.string.pair else Res.string.pair_chat_only) {
                showPair = true
            }
        },
    )
}

@Composable
internal fun NearbyPairButton(
    deviceId: String,
    status: NearbyItemStatus,
    label: org.jetbrains.compose.resources.StringResource,
    enabled: Boolean = true,
    onPair: () -> Unit,
) {
    val active = NearbyViewModel.pairingStates.values.any {
        it.deviceId == deviceId && it.phase !in setOf(PairingPhase.SUCCEEDED, PairingPhase.FAILED, PairingPhase.CANCELLED)
    }
    val cancelling = active || status == NearbyItemStatus.PAIRING
    POutlinedButton(
        text = stringResource(if (cancelling) Res.string.cancel else label),
        buttonSize = ButtonSize.SMALL,
        isLoading = status == NearbyItemStatus.STARTING || status == NearbyItemStatus.UNPAIRING,
        enabled = cancelling || enabled,
        onClick = { if (cancelling) NearbyViewModel.cancelPairing(deviceId) else onPair() },
    )
}
