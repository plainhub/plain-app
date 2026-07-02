package com.ismartcoding.plain.ui.page.chat.components

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.ismartcoding.plain.features.bluetooth.BlePairingCandidate
import com.ismartcoding.plain.ui.base.PListItem
import com.ismartcoding.plain.ui.base.POutlinedButton
import com.ismartcoding.plain.ui.theme.PlainTheme
import com.ismartcoding.plain.enums.BadgeType
import com.ismartcoding.plain.enums.ButtonSize
import com.ismartcoding.plain.enums.ButtonType
import com.ismartcoding.plain.i18n.Res
import com.ismartcoding.plain.i18n.bluetooth
import com.ismartcoding.plain.i18n.pair
import com.ismartcoding.plain.i18n.pending
import org.jetbrains.compose.resources.stringResource

@Composable
fun BleDeviceItem(
    candidate: BlePairingCandidate,
    isPairing: Boolean,
    onPairClick: () -> Unit,
) {
    Surface(
        modifier = PlainTheme.getCardModifier(selected = false),
        color = Color.Unspecified,
    ) {
        PListItem(
            title = candidate.name,
            titleSuffix = if (isPairing) {
                { com.ismartcoding.plain.ui.base.PStatusBadge(
                    text = stringResource(Res.string.pending),
                    type = BadgeType.WARN,
                ) }
            } else null,
            subtitle = candidate.mac,
            icon = com.ismartcoding.plain.enums.DeviceType.PHONE.getIcon(),
            action = {
                POutlinedButton(
                    text = stringResource(if (isPairing) Res.string.bluetooth else Res.string.pair),
                    onClick = onPairClick,
                    enabled = !isPairing,
                    type = ButtonType.PRIMARY,
                    buttonSize = ButtonSize.SMALL,
                )
            }
        )
    }
}
