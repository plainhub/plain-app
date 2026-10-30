package com.ismartcoding.plain.ui.tools

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.ismartcoding.plain.ui.base.pullrefresh.setRefreshState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.ui.base.*
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ToolsPage(navController: NavHostController, device: ManagedDeviceViewModel, onNavigateBack: () -> Unit) {
    val state by device.state.collectAsState()
    val scope = rememberCoroutineScope()
    val refresh = com.ismartcoding.plain.ui.base.pullrefresh.rememberRefreshLayoutState {
        val refreshState = this
        scope.launch {
            try { device.refresh() }
            finally { refreshState.setRefreshState(com.ismartcoding.plain.ui.base.pullrefresh.RefreshContentState.Finished) }
        }
    }
    PScaffold(topBar = {
        PTopAppBar(
            title = state.info?.deviceName ?: stringResource(Res.string.remote_management),
            onNavigateBack = onNavigateBack,
        )
    }) { padding ->
        com.ismartcoding.plain.ui.base.pullrefresh.PullToRefresh(
            modifier = Modifier.fillMaxSize().padding(padding), refreshLayoutState = refresh,
        ) {
            when {
                state.error != null -> Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) { ManagedError(state.error!!) }
                state.loading -> Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) { NoDataColumn(loading = true) }
                else -> FeatureItemsGrid(
                    features = FeatureItem.getList(navController, state.info!!.capabilities).filterNot { it.type in localToolTypes },
                    modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(vertical = 8.dp),
                )
            }
        }
    }
}

@Composable
internal fun ManagedError(message: String, modifier: Modifier = Modifier) {
    val description = when (message) {
        "connection_timeout" -> stringResource(Res.string.feed_err_timeout)
        "unauthorized", "peer_identity_changed", "peer_removed" -> stringResource(Res.string.managed_authorization_changed)
        "no_permission", "desktop_access_disabled" -> stringResource(Res.string.managed_permission_denied)
        else -> message
    }
    Column(modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        PAlert(description = description, type = AlertType.WARNING)
    }
}
