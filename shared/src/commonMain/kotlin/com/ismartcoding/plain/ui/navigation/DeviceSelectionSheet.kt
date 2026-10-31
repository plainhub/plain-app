package com.ismartcoding.plain.ui.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.ismartcoding.plain.Prefs
import com.ismartcoding.plain.graphql.Mutation
import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.models.Peer
import com.ismartcoding.plain.platform.system.getDeviceName
import com.ismartcoding.plain.platform.system.getDeviceType
import com.ismartcoding.plain.rust.graphql.GraphqlApi
import com.ismartcoding.plain.ui.app.DeviceIdentity
import com.ismartcoding.plain.ui.app.MainViewModel
import com.ismartcoding.plain.ui.base.BottomSpace
import com.ismartcoding.plain.ui.base.PBottomSheetTopAppBar
import com.ismartcoding.plain.ui.base.PFilledButton
import com.ismartcoding.plain.ui.base.PListItem
import com.ismartcoding.plain.ui.base.PModalBottomSheet
import com.ismartcoding.plain.ui.chat.LocalPeerViewModel
import com.ismartcoding.plain.ui.chat.components.PeerIconWithStatus
import com.ismartcoding.plain.ui.components.CheckCircle
import com.ismartcoding.plain.ui.components.DeviceRenameDialog
import com.ismartcoding.plain.ui.components.PIconButton
import com.ismartcoding.plain.ui.platform.system.getIcon
import com.ismartcoding.plain.ui.tools.LocalManagedDeviceViewModel
import com.ismartcoding.plain.ui.theme.listItemTitle
import com.ismartcoding.plain.ui.resources.Res as UiRes
import com.ismartcoding.plain.ui.resources.pen as ui_drawable_pen
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DeviceSelectionSheet(navController: NavHostController, onDismiss: () -> Unit) {
    val selectedDevice = MainViewModel.selectedDevice.value
    val localName by Prefs.deviceName.collectAsState()
    val peersMap by LocalPeerViewModel.current.peersMap.collectAsState()
    val peers = peersMap.values.filter { it.isPaired() && it.token.isNotEmpty() }.sortedBy { it.name.lowercase() }
    val manager = LocalManagedDeviceViewModel.current
    val state = manager?.state?.collectAsState()?.value
    val info = state?.client?.state?.collectAsState()?.value
    val deviceName = if (selectedDevice == null) localName.ifEmpty { getDeviceName() }
        else info?.deviceName ?: peersMap[selectedDevice.peerId]?.name.orEmpty()
    var showRename by remember { mutableStateOf(false) }
    if (showRename) {
        DeviceRenameDialog(name = deviceName, onDismiss = onDismiss, onSave = { name ->
            if (manager == null) GraphqlApi.execute(Mutation.updateDeviceName(name)) else manager.rename(name)
        })
        return
    }
    val hasChoices = peers.isNotEmpty() || selectedDevice != null
    PModalBottomSheet(onDismissRequest = onDismiss, modifier = Modifier.fillMaxWidth()) {
        PBottomSheetTopAppBar(
            title = stringResource(Res.string.select_a_device),
            actions = {
                PIconButton(
                    UiRes.drawable.ui_drawable_pen,
                    contentDescription = stringResource(Res.string.rename),
                    click = { showRename = true },
                )
            },
        )
        Column(
            Modifier.fillMaxWidth().weight(1f, fill = false)
                .verticalScroll(rememberScrollState())
                .then(if (hasChoices) Modifier.selectableGroup() else Modifier),
        ) {
            if (hasChoices) {
                (listOf<Peer?>(null) + peers).forEach { peer ->
                    val selected = peer?.id == selectedDevice?.peerId
                    val title = when {
                        peer == null -> stringResource(Res.string.this_device)
                        selected -> deviceName
                        else -> peer.name
                    }
                    PListItem(
                        modifier = Modifier.clip(MaterialTheme.shapes.small)
                            .selectable(selected = selected, role = Role.RadioButton, onClick = {
                                onDismiss()
                                MainViewModel.selectDevice(peer?.let { DeviceIdentity(it.id, it.publicKey) })
                            }),
                        title = { Text(title, style = MaterialTheme.typography.listItemTitle(), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        start = { PeerIconWithStatus((peer?.deviceType ?: getDeviceType()).getIcon(), peer?.online) },
                        action = { CheckCircle(selected = selected) },
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
                        onClick = {
                            onDismiss()
                            navController.navigate(Routing.Nearby)
                        },
                    )
                }
            }
            BottomSpace()
        }
    }
}
