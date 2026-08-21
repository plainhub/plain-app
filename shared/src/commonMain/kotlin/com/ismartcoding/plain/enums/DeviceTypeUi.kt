package com.ismartcoding.plain.enums

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.stringResource
import com.ismartcoding.plain.i18n.Res
import com.ismartcoding.plain.i18n.computer
import com.ismartcoding.plain.i18n.devices
import com.ismartcoding.plain.i18n.laptop
import com.ismartcoding.plain.i18n.other
import com.ismartcoding.plain.i18n.paired
import com.ismartcoding.plain.i18n.phone
import com.ismartcoding.plain.i18n.smartphone
import com.ismartcoding.plain.i18n.tablet
import com.ismartcoding.plain.i18n.tv
import com.ismartcoding.plain.i18n.unpaired

/**
 * UI presentation helpers for [DeviceType]. Kept as extension functions (instead of
 * members on the enum) so the enum stays a pure data type and can live in the
 * `:room-db` module without pulling Compose resources into it.
 */
@Composable
fun DeviceType.getText(): String {
    return when (this) {
        DeviceType.COMPUTER -> stringResource(Res.string.computer)
        DeviceType.PHONE -> stringResource(Res.string.phone)
        DeviceType.TABLET -> stringResource(Res.string.tablet)
        DeviceType.TV -> stringResource(Res.string.tv)
        DeviceType.OTHER -> stringResource(Res.string.other)
    }
}

fun DeviceType.getIcon(): DrawableResource {
    return when (this) {
        DeviceType.COMPUTER -> Res.drawable.laptop
        DeviceType.PHONE -> Res.drawable.smartphone
        DeviceType.TABLET -> Res.drawable.tablet
        DeviceType.TV -> Res.drawable.tv
        DeviceType.OTHER -> Res.drawable.devices
    }
}

@Composable
fun PeerStatus.getText(): String {
    return when (this) {
        PeerStatus.PAIRED -> stringResource(Res.string.paired)
        PeerStatus.UNPAIRED -> stringResource(Res.string.unpaired)
        PeerStatus.CHANNEL -> ""
    }
}
