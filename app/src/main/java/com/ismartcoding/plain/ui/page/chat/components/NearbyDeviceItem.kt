package com.ismartcoding.plain.ui.page.chat.components

import com.ismartcoding.plain.i18n.*

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import org.jetbrains.compose.resources.stringResource
import com.ismartcoding.plain.data.DNearbyDevice
import com.ismartcoding.plain.enums.BadgeType
import com.ismartcoding.plain.enums.ButtonSize
import com.ismartcoding.plain.enums.ButtonType
import com.ismartcoding.plain.ui.base.PListItem
import com.ismartcoding.plain.ui.base.POutlinedButton
import com.ismartcoding.plain.ui.base.PStatusBadge
import com.ismartcoding.plain.ui.theme.PlainTheme


@Composable
fun NearbyDeviceItem(
    item: DNearbyDevice,
    isPaired: Boolean = false,
    isPairing: Boolean = false,
    onPairClick: () -> Unit,
    onUnpairClick: () -> Unit,
    onCancelClick: () -> Unit
) {
    Surface(
        modifier = PlainTheme.getCardModifier(selected = isPaired),
        color = Color.Unspecified,
    ) {
        PListItem(
            title = item.name,
            titleSuffix = if (isPairing) {
                { PStatusBadge(text = stringResource(Res.string.pending), type = BadgeType.WARN) }
            } else null,
            subtitle = item.getBestIp(),
            icon = item.deviceType.getIcon(),
            action = {
                when {
                    isPairing -> {
                        POutlinedButton(
                            text = stringResource(Res.string.cancel),
                            onClick = onCancelClick,
                            type = ButtonType.DANGER,
                            buttonSize = ButtonSize.SMALL,
                        )
                    }

                    isPaired -> {
                        POutlinedButton(
                            text = stringResource(Res.string.unpair),
                            onClick = onUnpairClick,
                            type = ButtonType.DANGER,
                            buttonSize = ButtonSize.SMALL,
                        )
                    }

                    else -> {
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

