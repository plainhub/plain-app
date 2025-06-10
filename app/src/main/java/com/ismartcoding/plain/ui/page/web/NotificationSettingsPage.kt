package com.ismartcoding.plain.ui.page.web

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import coil3.compose.AsyncImage
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import com.ismartcoding.plain.R
import com.ismartcoding.plain.packageManager
import com.ismartcoding.plain.ui.base.ActionButtonMoreWithMenu
import com.ismartcoding.plain.ui.base.BottomSpace
import com.ismartcoding.plain.ui.base.HorizontalSpace
import com.ismartcoding.plain.ui.base.PCard
import com.ismartcoding.plain.ui.base.PDropdownMenuItem
import com.ismartcoding.plain.ui.base.PScaffold
import com.ismartcoding.plain.ui.base.PTopAppBar
import com.ismartcoding.plain.ui.base.Subtitle
import com.ismartcoding.plain.ui.base.TopSpace
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.models.NotificationSettingsViewModel
import com.ismartcoding.plain.ui.theme.PlainTheme
import com.ismartcoding.plain.ui.theme.listItemSubtitle
import com.ismartcoding.plain.ui.theme.listItemTitle
import com.ismartcoding.plain.ui.theme.red
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun NotificationSettingsPage(
    navController: NavHostController,
    viewModel: NotificationSettingsViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val selectedAppsState by viewModel.selectedAppsFlow.collectAsState()

    LaunchedEffect(Unit) {
        scope.launch(Dispatchers.IO) {
            viewModel.loadDataAsync(context)
        }
    }

    fun toggleMode() {
        scope.launch(Dispatchers.IO) {
            viewModel.toggleModeAsync(context)
        }
    }

    fun removeApp(packageName: String) {
        scope.launch(Dispatchers.IO) {
            viewModel.removeAppAsync(context, packageName)
        }
    }

    fun addApps(packageNames: List<String>) {
        scope.launch(Dispatchers.IO) {
            viewModel.addAppsAsync(context, packageNames)
        }
    }

    fun clearAll() {
        scope.launch(Dispatchers.IO) {
            viewModel.clearAllAsync(context)
        }
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
                                    Icon(
                                        painter = painterResource(R.drawable.delete_forever),
                                        tint = MaterialTheme.colorScheme.red,
                                        contentDescription = stringResource(R.string.clear_all)
                                    )
                                },
                                onClick = {
                                    dismiss()
                                    clearAll()
                                },
                                text = {
                                    Text(text = stringResource(R.string.clear_all))
                                }
                            )
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.padding(top = paddingValues.calculateTopPadding())
        ) {
            item {
                TopSpace()
                Subtitle(text = stringResource(R.string.filter_mode))
                PCard {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = stringResource(R.string.notification_filter_mode_desc),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        VerticalSpace(dp = 12.dp)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = viewModel.filterData.value.mode == "allowlist",
                                onClick = { if (viewModel.filterData.value.mode != "allowlist") toggleMode() },
                                label = { Text(stringResource(R.string.allowlist_mode)) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                            FilterChip(
                                selected = viewModel.filterData.value.mode == "blacklist",
                                onClick = { if (viewModel.filterData.value.mode != "blacklist") toggleMode() },
                                label = { Text(stringResource(R.string.blacklist_mode)) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }
                }
                VerticalSpace(dp = 16.dp)
            }

            item {
                Subtitle(
                    text = stringResource(
                        if (viewModel.filterData.value.mode == "allowlist")
                            R.string.allowed_apps
                        else
                            R.string.blocked_apps
                    )
                )
            }

            if (!viewModel.isLoading.value) {
                items(selectedAppsState, key = { it.id }) { app ->
                    Row(
                        modifier = PlainTheme
                            .getCardModifier()
                            .padding(start = 16.dp, end = 8.dp, top = 16.dp, bottom = 16.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val appIcon = remember(app.id) {
                            packageManager.getApplicationIcon(app.appInfo)
                        }
                        AsyncImage(
                            model = appIcon,
                            contentDescription = app.name,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            placeholder = rememberDrawablePainter(
                                appIcon
                            )
                        )
                        HorizontalSpace(dp = 12.dp)
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = app.name,
                                style = MaterialTheme.typography.listItemTitle()
                            )
                            VerticalSpace(dp = 4.dp)
                            Text(
                                text = app.id,
                                style = MaterialTheme.typography.listItemSubtitle()
                            )
                        }
                        TextButton(
                            onClick = { removeApp(app.id) }
                        ) {
                            Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.red)
                        }
                    }
                    VerticalSpace(dp = 8.dp)
                }

                item {
                    Button(
                        onClick = { 
                            viewModel.showAppSelectorDialog()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.plus),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        HorizontalSpace(dp = 8.dp)
                        Text(stringResource(R.string.add_app))
                    }
                    VerticalSpace(dp = 16.dp)
                }
            }

            item {
                BottomSpace()
            }
        }
    }

    if (viewModel.showAppSelector.value) {
        AppSelectorBottomSheet(
            vm = viewModel,
            onDismiss = { viewModel.showAppSelector.value = false },
            onAppsSelected = { packageNames ->
                addApps(packageNames)
                viewModel.showAppSelector.value = false
            }
        )
    }
}