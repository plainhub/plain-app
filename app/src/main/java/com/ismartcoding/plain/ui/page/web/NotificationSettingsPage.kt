package com.ismartcoding.plain.ui.page.web

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.ismartcoding.plain.R
import com.ismartcoding.plain.ui.base.ActionButtonMoreWithMenu
import com.ismartcoding.plain.ui.base.BottomSpace
import com.ismartcoding.plain.ui.base.HorizontalSpace
import com.ismartcoding.plain.ui.base.PDropdownMenuItem
import com.ismartcoding.plain.ui.base.PScaffold
import com.ismartcoding.plain.ui.base.PTopAppBar
import com.ismartcoding.plain.ui.base.Subtitle
import com.ismartcoding.plain.ui.base.TopSpace
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.models.NotificationSettingsViewModel
import com.ismartcoding.plain.ui.theme.red
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun NotificationSettingsPage(
    navController: NavHostController,
    vm: NotificationSettingsViewModel = viewModel()
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    val selectedAppsState by vm.selectedAppsFlow.collectAsState()

    LaunchedEffect(Unit) {
        scope.launch(Dispatchers.IO) { vm.loadDataAsync(context) }
    }

    PScaffold(
        topBar = {
            PTopAppBar(
                navController = navController,
                title = stringResource(R.string.notification_filter_settings),
                actions = {
                    if (selectedAppsState.isNotEmpty()) {
                        ActionButtonMoreWithMenu { dismiss ->
                            PDropdownMenuItem(
                                leadingIcon = {
                                    Icon(painter = painterResource(R.drawable.delete_forever),
                                        tint = MaterialTheme.colorScheme.red,
                                        contentDescription = stringResource(R.string.clear_all))
                                },
                                onClick = { dismiss(); scope.launch(Dispatchers.IO) { vm.clearAllAsync(context) } },
                                text = { Text(text = stringResource(R.string.clear_all)) }
                            )
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(modifier = Modifier.padding(top = paddingValues.calculateTopPadding())) {
            item {
                TopSpace()
                Subtitle(text = stringResource(R.string.filter_mode))
                NotificationFilterModeCard(
                    mode = vm.filterData.value.mode,
                    onToggleMode = { scope.launch(Dispatchers.IO) { vm.toggleModeAsync(context) } }
                )
                VerticalSpace(dp = 16.dp)
            }

            item {
                Subtitle(text = stringResource(
                    if (vm.filterData.value.mode == "allowlist") R.string.allowed_apps else R.string.blocked_apps
                ))
            }

            if (!vm.isLoading.value) {
                items(selectedAppsState, key = { it.id }) { app ->
                    NotificationAppListItem(app = app, onRemove = {
                        scope.launch(Dispatchers.IO) { vm.removeAppAsync(context, app.id) }
                    })
                }
                item {
                    Button(
                        onClick = { vm.showAppSelectorDialog() },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                    ) {
                        Icon(painter = painterResource(R.drawable.plus), contentDescription = null, modifier = Modifier.size(18.dp))
                        HorizontalSpace(dp = 8.dp)
                        Text(stringResource(R.string.add_app))
                    }
                    VerticalSpace(dp = 16.dp)
                }
            }

            item { BottomSpace(paddingValues) }
        }
    }

    if (vm.showAppSelector.value) {
        AppSelectorBottomSheet(
            vm = vm,
            onDismiss = { vm.showAppSelector.value = false },
            onAppsSelected = { packageNames ->
                scope.launch(Dispatchers.IO) { vm.addAppsAsync(context, packageNames) }
                vm.showAppSelector.value = false
            }
        )
    }
}
