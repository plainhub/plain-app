package com.ismartcoding.plain.ui.page.chat.components

import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.i18n.*
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import com.ismartcoding.plain.data.DNearbyDevice
import com.ismartcoding.plain.enums.BadgeType
import com.ismartcoding.plain.enums.ButtonSize
import com.ismartcoding.plain.enums.ButtonType
import com.ismartcoding.plain.ui.base.HorizontalSpace
import com.ismartcoding.plain.ui.base.PListItem
import com.ismartcoding.plain.ui.base.POutlinedButton
import com.ismartcoding.plain.ui.base.PStatusBadge
import com.ismartcoding.plain.ui.models.NearbyItemStatus
import com.ismartcoding.plain.ui.theme.PlainTheme

@Composable
fun NearbyDeviceItem(
    item: DNearbyDevice,
    status: NearbyItemStatus,
    onPairClick: () -> Unit,
    onUnpairClick: () -> Unit,
    onCancelClick: () -> Unit,
) {
    Surface(
        modifier = PlainTheme.getCardModifier(selected = status == NearbyItemStatus.PAIRED),
        color = Color.Unspecified,
    ) {
        PListItem(
            title = item.name,
            titleSuffix = {
                if (item.discoveredViaBle) {
                    Icon(
                        painter = painterResource(Res.drawable.bluetooth),
                        contentDescription = "Bluetooth",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (item.discoveredViaLan) {
                    if (item.discoveredViaBle) HorizontalSpace(2.dp)
                    Icon(
                        painter = painterResource(Res.drawable.wifi),
                        contentDescription = "Wi-Fi",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (status == NearbyItemStatus.PAIRING) {
                    HorizontalSpace(4.dp)
                    PStatusBadge(text = stringResource(Res.string.pending), type = BadgeType.WARN)
                }
            },
            subtitle = com.ismartcoding.plain.lib.helpers.NetworkHelper.getBestIp(item.ips),
            icon = item.deviceType.getIcon(),
            action = {
                when (status) {
                    NearbyItemStatus.COMPLETING -> {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }

                    NearbyItemStatus.PAIRING -> {
                        POutlinedButton(
                            text = stringResource(Res.string.cancel),
                            onClick = onCancelClick,
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
                            onClick = onUnpairClick,
                            type = ButtonType.DANGER,
                            buttonSize = ButtonSize.SMALL,
                        )
                    }

                    NearbyItemStatus.UNPAIRED -> {
                        POutlinedButton(
                            text = stringResource(Res.string.pair),
                            onClick = onPairClick,
                            buttonSize = ButtonSize.SMALL,
                        )
                    }
                }
            }
        )
    }
}
