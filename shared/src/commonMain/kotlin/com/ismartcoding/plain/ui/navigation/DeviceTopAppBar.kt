package com.ismartcoding.plain.ui.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.models.Peer
import com.ismartcoding.plain.platform.system.Capability
import com.ismartcoding.plain.platform.system.getDeviceType
import com.ismartcoding.plain.ui.base.BottomSpace
import com.ismartcoding.plain.ui.base.PBottomSheetTopAppBar
import com.ismartcoding.plain.ui.base.PListItem
import com.ismartcoding.plain.ui.base.PFilledButton
import com.ismartcoding.plain.ui.base.PModalBottomSheet
import com.ismartcoding.plain.ui.base.PTopAppBar
import com.ismartcoding.plain.ui.chat.components.PeerIconWithStatus
import com.ismartcoding.plain.ui.components.DeviceRenameDialog
import com.ismartcoding.plain.ui.components.CheckCircle
import com.ismartcoding.plain.ui.components.PIconButton
import com.ismartcoding.plain.ui.platform.system.getIcon
import com.ismartcoding.plain.ui.resources.Res as UiRes
import com.ismartcoding.plain.ui.resources.chevron_down as ui_drawable_chevron_down
import com.ismartcoding.plain.ui.resources.pen as ui_drawable_pen
import com.ismartcoding.plain.ui.theme.listItemTitle
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

internal data class DeviceNavigation(
    val deviceName: String,
    val peers: List<Peer>,
    val capabilities: List<Capability>,
    val selectedPeerId: String? = null,
    val onSelect: (Peer?) -> Unit,
    val onPairDevice: () -> Unit,
    val onSettings: () -> Unit,
    val onRename: suspend (String) -> Unit,
)

internal val LocalDeviceNavigation = staticCompositionLocalOf<DeviceNavigation?> { null }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DeviceTopAppBar(
    fallbackTitle: String,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val navigation = LocalDeviceNavigation.current
    var showDevices by remember { mutableStateOf(false) }
    var showRename by remember { mutableStateOf(false) }
    if (showRename && navigation != null) DeviceRenameDialog(
        name = navigation.deviceName, onDismiss = { showRename = false }, onSave = navigation.onRename,
    )
    if (showDevices && navigation != null) DeviceSelectionSheet(
        navigation = navigation,
        onDismiss = { showDevices = false },
        onRename = { showDevices = false; showRename = true },
    )
    PTopAppBar(
        title = navigation?.deviceName ?: fallbackTitle,
        onTitleClick = if (navigation != null) ({ showDevices = true }) else null,
        titleTrailing = if (navigation != null) ({
            Icon(painterResource(UiRes.drawable.ui_drawable_chevron_down), contentDescription = stringResource(Res.string.select_a_device))
        }) else null,
        actions = actions,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DeviceSelectionSheet(navigation: DeviceNavigation, onDismiss: () -> Unit, onRename: () -> Unit) {
    val hasChoices = navigation.peers.isNotEmpty() || navigation.selectedPeerId != null
    PModalBottomSheet(onDismissRequest = onDismiss, modifier = Modifier.fillMaxWidth()) {
        PBottomSheetTopAppBar(
            title = stringResource(Res.string.select_a_device),
            actions = {
                PIconButton(
                    UiRes.drawable.ui_drawable_pen,
                    contentDescription = stringResource(Res.string.rename),
                    click = onRename,
                )
            },
        )
        Column(
            Modifier.fillMaxWidth().weight(1f, fill = false)
                .verticalScroll(rememberScrollState())
                .then(if (hasChoices) Modifier.selectableGroup() else Modifier),
        ) {
            if (hasChoices) {
                DeviceSelectionRow(
                    title = stringResource(Res.string.this_device), icon = getDeviceType().getIcon(),
                    online = null, isSelected = navigation.selectedPeerId == null,
                    onClick = { onDismiss(); navigation.onSelect(null) },
                )
                navigation.peers.forEach { peer ->
                    DeviceSelectionRow(
                        title = if (navigation.selectedPeerId == peer.id) navigation.deviceName else peer.name,
                        icon = peer.deviceType.getIcon(), online = peer.online,
                        isSelected = navigation.selectedPeerId == peer.id,
                        onClick = { onDismiss(); navigation.onSelect(peer) },
                    )
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text(
                        text = stringResource(Res.string.no_paired_devices_hint),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    PFilledButton(
                        text = stringResource(Res.string.pair_device),
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { onDismiss(); navigation.onPairDevice() },
                    )
                }
            }
            BottomSpace()
        }
    }
}

@Composable
private fun DeviceSelectionRow(title: String, icon: DrawableResource, online: Boolean?, isSelected: Boolean, onClick: () -> Unit) {
    PListItem(
        modifier = Modifier.clip(MaterialTheme.shapes.small)
            .selectable(selected = isSelected, role = Role.RadioButton, onClick = onClick),
        title = { Text(title, style = MaterialTheme.typography.listItemTitle(), maxLines = 1, overflow = TextOverflow.Ellipsis) },
        start = { PeerIconWithStatus(icon, online) },
        action = { CheckCircle(selected = isSelected) },
    )
}
