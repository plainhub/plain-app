package com.ismartcoding.plain.ui.tools

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.ui.base.*
import com.ismartcoding.plain.ui.navigation.Routing
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ToolsPage(navController: NavHostController, device: ManagedDeviceViewModel) {
    val state by device.state.collectAsState()
    val scope = rememberCoroutineScope()
    LaunchedEffect(device) { device.refresh() }
    PScaffold(topBar = {
        PTopAppBar(
            title = state.info?.deviceName ?: stringResource(Res.string.remote_management),
            subtitle = stringResource(Res.string.tools),
            onNavigateBack = { navController.navigateUp() },
            actions = {
                PTextButton(text = stringResource(Res.string.refresh), enabled = !state.loading, onClick = { scope.launch { device.refresh() } })
            },
        )
    }) { padding ->
        when {
            state.error != null -> ManagedError(state.error!!, Modifier.padding(padding)) { scope.launch { device.refresh() } }
            state.loading -> NoDataColumn(loading = true, modifier = Modifier.fillMaxSize().padding(padding))
            else -> {
                val info = state.info!!
                val features = managedFeatureItems(info.capabilities) {
                    navController.navigate(Routing.ManagedContent(device.peerId, device.publicKey, it.name))
                }
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(104.dp),
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(vertical = 8.dp),
                ) {
                    items(features, key = { it.type }) { item ->
                        PIconTextActionButton(
                            icon = item.iconRes, text = stringResource(item.titleRes),
                            modifier = Modifier.fillMaxWidth().heightIn(min = 88.dp), onClick = item.click,
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun ManagedError(message: String, modifier: Modifier = Modifier, onRetry: () -> Unit) {
    val description = when (message) {
        "connection_timeout" -> stringResource(Res.string.feed_err_timeout)
        "unauthorized", "peer_identity_changed", "peer_removed" -> stringResource(Res.string.managed_authorization_changed)
        "no_permission", "desktop_access_disabled" -> stringResource(Res.string.managed_permission_denied)
        else -> message
    }
    Column(modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        PAlert(description = description, type = AlertType.WARNING)
        PTextButton(text = stringResource(Res.string.refresh), onClick = onRetry)
    }
}
