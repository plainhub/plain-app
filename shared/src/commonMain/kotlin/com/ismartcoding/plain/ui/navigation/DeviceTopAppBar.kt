package com.ismartcoding.plain.ui.navigation

import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.platform.LocalDensity
import com.ismartcoding.plain.platform.system.getDeviceType
import com.ismartcoding.plain.ui.platform.system.getIcon
import com.ismartcoding.plain.ui.chat.components.PeerIconWithStatus
import androidx.compose.foundation.layout.width
import com.ismartcoding.plain.ui.components.DeviceRenameDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.models.Peer
import com.ismartcoding.plain.platform.system.Capability
import com.ismartcoding.plain.ui.base.PDropdownMenu
import com.ismartcoding.plain.ui.base.PDropdownMenuItem
import com.ismartcoding.plain.ui.base.PTopAppBar
import com.ismartcoding.plain.ui.resources.Res as UiRes
import com.ismartcoding.plain.ui.resources.check as ui_drawable_check
import com.ismartcoding.plain.ui.resources.chevron_down as ui_drawable_chevron_down
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

internal data class DeviceNavigation(
    val deviceName: String,
    val peers: List<Peer>,
    val capabilities: List<Capability>,
    val selectedPeerId: String? = null,
    val onSelect: (Peer?) -> Unit,
    val onSettings: () -> Unit,
    val onRename: (suspend (String) -> Unit)?,
)

internal val LocalDeviceNavigation = staticCompositionLocalOf<DeviceNavigation?> { null }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DeviceTopAppBar(
    fallbackTitle: String,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val navigation = LocalDeviceNavigation.current
    var expanded by remember { mutableStateOf(false) }
    var showRename by remember { mutableStateOf(false) }
    if (showRename && navigation?.onRename != null) DeviceRenameDialog(
        name = navigation.deviceName, onDismiss = { showRename = false }, onSave = navigation.onRename,
    )
    val canSelect = navigation != null
    val textMeasurer = rememberTextMeasurer()
    val textStyle = MaterialTheme.typography.labelLarge
    val thisDeviceText = stringResource(Res.string.this_device)
    val renameText = stringResource(Res.string.rename)
    val menuWidth = with(LocalDensity.current) {
        val renameWidth = textMeasurer.measure(renameText, textStyle, maxLines = 1, softWrap = false).size.width.toDp() + 48.dp
        val names = if (navigation != null && (navigation.peers.isNotEmpty() || navigation.selectedPeerId != null)) {
            listOf(thisDeviceText) + navigation.peers.map { if (navigation.selectedPeerId == it.id) navigation.deviceName else it.name }
        } else emptyList()
        maxOf(renameWidth, names.maxOfOrNull { textMeasurer.measure(it, textStyle, maxLines = 1, softWrap = false).size.width.toDp() + 144.dp } ?: 0.dp)
    }
    PTopAppBar(
        title = navigation?.deviceName ?: fallbackTitle,
        onTitleClick = if (canSelect) ({ expanded = true }) else null,
        titleTrailing = if (canSelect) ({
            Box {
                Icon(painterResource(UiRes.drawable.ui_drawable_chevron_down), contentDescription = stringResource(Res.string.devices))
                PDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, modifier = Modifier.width(menuWidth)) {
                    if (navigation.peers.isNotEmpty() || navigation.selectedPeerId != null) PDropdownMenuItem(
                        text = { Text(thisDeviceText, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        leadingIcon = { PeerIconWithStatus(getDeviceType().getIcon(), online = null) },
                        trailingIcon = if (navigation.selectedPeerId == null) ({ Icon(painterResource(UiRes.drawable.ui_drawable_check), null) }) else null,
                        onClick = { expanded = false; navigation.onSelect(null) },
                    )
                    navigation.peers.forEach { peer ->
                        PDropdownMenuItem(
                            text = { Text(if (navigation.selectedPeerId == peer.id) navigation.deviceName else peer.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            leadingIcon = { PeerIconWithStatus(peer.deviceType.getIcon(), peer.online) },
                            trailingIcon = if (navigation.selectedPeerId == peer.id) ({ Icon(painterResource(UiRes.drawable.ui_drawable_check), null) }) else null,
                            onClick = { expanded = false; navigation.onSelect(peer) },
                        )
                    }
                    PDropdownMenuItem(
                        text = { Text(renameText) },
                        enabled = navigation.onRename != null,
                        onClick = { expanded = false; showRename = true },
                    )
                }
            }
        }) else null,
        actions = actions,
    )
}
