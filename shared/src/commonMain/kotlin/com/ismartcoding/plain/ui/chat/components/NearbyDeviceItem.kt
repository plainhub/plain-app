package com.ismartcoding.plain.ui.chat.components

import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.ismartcoding.plain.ui.theme.listItemSubtitle
import org.jetbrains.compose.resources.stringResource

@Composable
fun NearbyDeviceItem(item: NearbyDevice, enabled: Boolean, onPair: () -> Unit) {
    val pairing = NearbyViewModel.pairingViews[item.id]
    val pending = pairing?.active == true
    val isUnpairing = NearbyViewModel.itemStatus[item.id] == NearbyItemStatus.UNPAIRING
    PListItem(
        modifier = PlainTheme.getCardModifier().testTag("nearby-device:${item.id}"),
        title = {
            if (pairing?.step == PairingStep.FAILED && !pairing.expired) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(item.name, Modifier.weight(1f))
                    Text(stringResource(pairing.error.messageResource()))
                }
            } else Text(item.name)
        },
        subtitle = if (item.bestIp.isNotEmpty()) ({ Text(item.bestIp) }) else null,
        description = if (pairing?.expired == true) ({
            Text(
                stringResource(Res.string.pairing_expired),
                style = MaterialTheme.typography.listItemSubtitle(),
                color = MaterialTheme.colorScheme.error,
            )
        }) else null,
        start = { PeerIconWithStatus(item.deviceType.getIcon(), null) },
        action = {
            POutlinedButton(
                text = stringResource(when {
                    pending -> Res.string.pending
                    item.remoteAccess == true -> Res.string.pair
                    else -> Res.string.pair_chat_only
                }),
                buttonSize = ButtonSize.SMALL,
                enabled = enabled && !isUnpairing,
                onClick = { if (pending) NearbyViewModel.cancelPairing(item.id) else onPair() },
            )
        },
    )
}
