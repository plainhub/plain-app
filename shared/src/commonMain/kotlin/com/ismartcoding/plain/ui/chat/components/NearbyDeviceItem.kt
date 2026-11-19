package com.ismartcoding.plain.ui.chat.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.testTag
import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.models.NearbyDevice
import com.ismartcoding.plain.ui.base.PListItem
import com.ismartcoding.plain.ui.base.POutlinedButton
import com.ismartcoding.plain.ui.components.ButtonSize
import com.ismartcoding.plain.ui.nearby.NearbyItemStatus
import com.ismartcoding.plain.ui.nearby.NearbyViewModel
import com.ismartcoding.plain.ui.nearby.PairingStep
import com.ismartcoding.plain.ui.nearby.messageResource
import com.ismartcoding.plain.ui.platform.system.getIcon
import com.ismartcoding.plain.ui.theme.PlainTheme
import org.jetbrains.compose.resources.stringResource

@Composable
fun NearbyDeviceItem(item: NearbyDevice, enabled: Boolean, onPair: () -> Unit) {
    val pairing = NearbyViewModel.pairingViews[item.id]
    val pending = pairing?.active == true
    val status = NearbyViewModel.getStatus(item.id, false)
    val detail = when {
        pairing?.step == PairingStep.FAILED -> stringResource(pairing.error.messageResource())
        pairing?.step == PairingStep.WAITING_FOR_PEER -> pairing.verificationCode
        else -> null
    }
    PListItem(
        modifier = PlainTheme.getCardModifier().testTag("nearby-device:${item.id}"),
        title = item.name,
        subtitle = item.bestIp,
        titleTrailing = { if (detail != null) Text(detail) },
        start = { PeerIconWithStatus(item.deviceType.getIcon(), null) },
        action = {
            POutlinedButton(
                text = stringResource(when {
                    pending -> Res.string.pending
                    item.remoteAccess == true -> Res.string.pair
                    else -> Res.string.pair_chat_only
                }),
                buttonSize = ButtonSize.SMALL,
                enabled = enabled && status != NearbyItemStatus.UNPAIRING,
                onClick = { if (pending) NearbyViewModel.cancelPairing(item.id) else onPair() },
            )
        },
    )
}
