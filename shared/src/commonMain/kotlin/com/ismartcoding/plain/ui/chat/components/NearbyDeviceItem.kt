package com.ismartcoding.plain.ui.chat.components

import com.ismartcoding.plain.models.NearbyDevice
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.i18n.*
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import com.ismartcoding.plain.ui.components.ButtonSize
import com.ismartcoding.plain.ui.components.ButtonType
import com.ismartcoding.plain.features.nearby.PairingPhase
import com.ismartcoding.plain.features.nearby.DiscoveryMethod
import com.ismartcoding.plain.ui.platform.system.getIcon
import com.ismartcoding.plain.ui.base.HorizontalSpace
import com.ismartcoding.plain.ui.base.PListItem
import com.ismartcoding.plain.ui.base.POutlinedButton
import com.ismartcoding.plain.ui.nearby.NearbyItemStatus
import com.ismartcoding.plain.ui.nearby.NearbyViewModel
import com.ismartcoding.plain.ui.theme.PlainTheme
import com.ismartcoding.plain.ui.resources.Res as UiRes
import com.ismartcoding.plain.ui.resources.bluetooth as ui_drawable_bluetooth
import com.ismartcoding.plain.ui.resources.wifi as ui_drawable_wifi
import com.ismartcoding.plain.i18n.bluetooth
import com.ismartcoding.plain.ui.nearby.PairPeerDialog

@Composable
fun NearbyDeviceItem(
    item: NearbyDevice,
    status: NearbyItemStatus,
    bestIp: String,
) {
    var showPair by remember { mutableStateOf(false) }
    val verificationCode = NearbyViewModel.pairingStates.values.firstOrNull {
        it.deviceId == item.id && it.phase !in setOf(PairingPhase.SUCCEEDED, PairingPhase.FAILED, PairingPhase.CANCELLED)
    }?.verificationCode
    if (showPair) PairPeerDialog(item) { showPair = false }
    Surface(
        modifier = PlainTheme.getCardModifier().testTag("nearby-device:${item.id}"),
        color = Color.Unspecified,
    ) {
        PListItem(
            title = item.name,
            titleSuffix = {
                if (DiscoveryMethod.BLE in item.discoveryMethods) {
                    Icon(
                        painter = painterResource(UiRes.drawable.ui_drawable_bluetooth),
                        contentDescription = "Bluetooth",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (DiscoveryMethod.LAN in item.discoveryMethods) {
                    if (DiscoveryMethod.BLE in item.discoveryMethods) HorizontalSpace(4.dp)
                    Icon(
                        painter = painterResource(UiRes.drawable.ui_drawable_wifi),
                        contentDescription = "Wi-Fi",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            subtitle = verificationCode ?: bestIp,
            icon = item.deviceType.getIcon(),
            action = {
                when (status) {
                    NearbyItemStatus.PAIRING -> {
                        POutlinedButton(
                            text = stringResource(Res.string.cancel),
                            onClick = {
                                NearbyViewModel.cancelPairing(item.id)
                            },
                            type = ButtonType.DANGER,
                            buttonSize = ButtonSize.SMALL,
                        )
                    }

                    NearbyItemStatus.UNPAIRING -> {
                        POutlinedButton(
                            text = stringResource(Res.string.unpair),
                            onClick = {},
                            type = ButtonType.DANGER,
                            buttonSize = ButtonSize.SMALL,
                            isLoading = true,
                            enabled = false,
                        )
                    }

                    NearbyItemStatus.PAIRED -> {
                        POutlinedButton(
                            text = stringResource(Res.string.unpair),
                            onClick = {
                                NearbyViewModel.unpairDevice(item.id)
                            },
                            type = ButtonType.DANGER,
                            buttonSize = ButtonSize.SMALL,
                        )
                    }

                    NearbyItemStatus.UNPAIRED -> {
                        POutlinedButton(
                            text = stringResource(Res.string.pair),
                            onClick = {
                                showPair = true
                            },
                            buttonSize = ButtonSize.SMALL,
                        )
                    }

                    NearbyItemStatus.STARTING -> {
                        POutlinedButton(
                            text = stringResource(Res.string.pair),
                            onClick = {},
                            buttonSize = ButtonSize.SMALL,
                            isLoading = true,
                            enabled = false,
                        )
                    }
                }
            }
        )
    }
}
