package com.ismartcoding.plain.ui.page.home

import com.ismartcoding.plain.i18n.*
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import org.jetbrains.compose.resources.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.ismartcoding.plain.R
import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.helpers.PhoneHelper
import com.ismartcoding.plain.preferences.NearbyDiscoverablePreference
import com.ismartcoding.plain.preferences.dataFlow
import com.ismartcoding.plain.preferences.dataStore
import com.ismartcoding.plain.ui.base.ActionButtonScan
import com.ismartcoding.plain.ui.base.ActionButtonSettings
import com.ismartcoding.plain.ui.base.PDialogListItem
import com.ismartcoding.plain.ui.base.PIconButton
import com.ismartcoding.plain.ui.base.PSwitch
import com.ismartcoding.plain.ui.components.DeviceRenameDialog
import com.ismartcoding.plain.ui.extensions.collectAsStateValue
import com.ismartcoding.plain.ui.models.PeerViewModel
import com.ismartcoding.plain.ui.nav.Routing
import kotlinx.coroutines.flow.map

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBarHome(navController: NavHostController, peerVM: PeerViewModel) {
    val context = LocalContext.current
    var showRenameDialog by remember { mutableStateOf(false) }
    var showDiscoverableDialog by remember { mutableStateOf(false) }
    var deviceName by remember { mutableStateOf("") }
    val isDiscoverable = remember {
        context.dataStore.dataFlow.map { NearbyDiscoverablePreference.get(it) }
    }.collectAsStateValue(initial = NearbyDiscoverablePreference.default)

    LaunchedEffect(Unit) { deviceName = TempData.deviceName }

    if (showRenameDialog) {
        DeviceRenameDialog(
            name = deviceName,
            onDismiss = { showRenameDialog = false },
            onDone = { deviceName = TempData.deviceName },
        )
    }

    if (showDiscoverableDialog) {
        AlertDialog(
            containerColor = MaterialTheme.colorScheme.surface,
            onDismissRequest = { showDiscoverableDialog = false },
            title = { Text(stringResource(R.string.make_discoverable), style = MaterialTheme.typography.titleLarge) },
            text = {
                PDialogListItem(title = stringResource(R.string.make_discoverable_desc)) {
                    PSwitch(activated = isDiscoverable) {
                        peerVM.updateDiscoverable(context, it)
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showDiscoverableDialog = false }) {
                    Text(text = stringResource(R.string.close))
                }
            },
        )
    }

    TopAppBar(
        title = {
            Row(
                modifier = Modifier.padding(start = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = deviceName.ifEmpty { PhoneHelper.getDeviceName(context) },
                    style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp, fontWeight = FontWeight.SemiBold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                IconButton(
                    onClick = { showRenameDialog = true },
                    modifier = Modifier.size(32.dp),
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.pen),
                        contentDescription = stringResource(R.string.device_name),
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        actions = {
            PIconButton(
                icon = if (isDiscoverable) Res.drawable.eye else Res.drawable.eye_off,
                contentDescription = stringResource(R.string.make_discoverable),
                tint = MaterialTheme.colorScheme.onSurface,
                click = { showDiscoverableDialog = true },
            )
            ActionButtonSettings(
                onClick = { navController.navigate(Routing.Settings) },
            )
            ActionButtonScan {
                navController.navigate(Routing.Scan)
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
            scrolledContainerColor = MaterialTheme.colorScheme.background,
        ),
    )
}