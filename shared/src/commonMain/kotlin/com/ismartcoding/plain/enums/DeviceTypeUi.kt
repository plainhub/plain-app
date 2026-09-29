package com.ismartcoding.plain.enums

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.stringResource
import com.ismartcoding.plain.i18n.Res
import com.ismartcoding.plain.i18n.computer
import com.ismartcoding.plain.ui.resources.devices as ui_drawable_devices
import com.ismartcoding.plain.ui.resources.laptop as ui_drawable_laptop
import com.ismartcoding.plain.i18n.other
import com.ismartcoding.plain.i18n.paired
import com.ismartcoding.plain.i18n.phone
import com.ismartcoding.plain.ui.resources.smartphone as ui_drawable_smartphone
import com.ismartcoding.plain.ui.resources.tablet as ui_drawable_tablet
import com.ismartcoding.plain.ui.resources.tv as ui_drawable_tv
import com.ismartcoding.plain.i18n.unpaired
import com.ismartcoding.plain.ui.resources.Res as UiRes
import com.ismartcoding.plain.ui.resources.devices as ui_drawable_devices
import com.ismartcoding.plain.ui.resources.laptop as ui_drawable_laptop
import com.ismartcoding.plain.ui.resources.smartphone as ui_drawable_smartphone
import com.ismartcoding.plain.ui.resources.tablet as ui_drawable_tablet
import com.ismartcoding.plain.ui.resources.tv as ui_drawable_tv
import com.ismartcoding.plain.i18n.tablet
import com.ismartcoding.plain.i18n.devices
import com.ismartcoding.plain.i18n.tv

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
        DeviceType.NAS, DeviceType.OTHER -> stringResource(Res.string.other)
    }
}

fun DeviceType.getIcon(): DrawableResource {
    return when (this) {
        DeviceType.COMPUTER -> UiRes.drawable.ui_drawable_laptop
        DeviceType.PHONE -> UiRes.drawable.ui_drawable_smartphone
        DeviceType.TABLET -> UiRes.drawable.ui_drawable_tablet
        DeviceType.TV -> UiRes.drawable.ui_drawable_tv
        DeviceType.NAS, DeviceType.OTHER -> UiRes.drawable.ui_drawable_devices
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
